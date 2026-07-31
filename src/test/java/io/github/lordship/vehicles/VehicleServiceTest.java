package io.github.lordship.vehicles;

import io.github.lordship.properties.internal.PropertyRow;
import io.github.lordship.vehicles.internal.VehicleCreateRequest;
import io.github.lordship.vehicles.internal.VehiclePolicyRow;
import io.github.lordship.vehicles.internal.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional

public class VehicleServiceTest {

    @Autowired
    VehicleService vehicleService;

    @Autowired
    VehicleRepository vehicleRepository;

    @Autowired
    JdbcClient jdbc;

    private UUID insertTestProperty() {
        PropertyRow row = jdbc.sql("""
                INSERT INTO property (property_code, property_name, property_address)
                VALUES ('TST01', 'Test Mobile Park', '999 Test Ave') RETURNING *
                """).query(PropertyRow.class).single();
        return row.uuid();
    }

    private UUID insertTestTenancy(UUID propertyUuid) {
        UUID lotId = jdbc.sql("""
                INSERT INTO lot (property_id, lot_number)
                VALUES (:propertyUuid, '1')
                RETURNING uuid
                """)
                .param("propertyUuid", propertyUuid)
                .query(UUID.class).single();

        return jdbc.sql("""
                INSERT INTO tenancy (lot_id, start_date)
                VALUES (:lotId, CURRENT_DATE)
                RETURNING uuid
                """)
                .param("lotId", lotId)
                .query(UUID.class).single();
    }

    private VehicleCreateRequest buildRequest(UUID tenancyUuid, UUID propertyUuid) {
        return new VehicleCreateRequest(tenancyUuid, propertyUuid, "ABC123");
    }

    @Test
    void registerVehicleReturnsVehicleWithGeneratedFields() {
        UUID propertyCode = insertTestProperty();
        UUID tenancyUuid = insertTestTenancy(propertyCode);

        VehicleRegistrationResult result = vehicleService.registerVehicle(buildRequest(tenancyUuid, propertyCode));

        assertNotNull(result.vehicle().uuid());
        assertEquals("Toyota", result.vehicle().make());
        assertEquals("ABC123", result.vehicle().plateNumber());
    }

    @Test
    void registerVehicleNoFeeWhenUnderFreeLimit() {
        UUID propertyCode = insertTestProperty();
        UUID tenancyUuid = insertTestTenancy(propertyCode);

        // Policy: 2 free vehicles, $25 fee for extras
        vehicleRepository.savePolicy(new VehiclePolicyRow(null, propertyCode, 2, new BigDecimal("25.00"), null, null, null));

        VehicleRegistrationResult result = vehicleService.registerVehicle(buildRequest(tenancyUuid, propertyCode));

        assertEquals(BigDecimal.ZERO, result.applicableFee());
    }

    @Test
    void registerVehicleFeeAppliedWhenOverFreeLimit() {
        UUID propertyCode = insertTestProperty();
        UUID tenancyUuid = insertTestTenancy(propertyCode);

        vehicleRepository.savePolicy(new VehiclePolicyRow(null, propertyCode, 1, new BigDecimal("25.00"), null, null, null));

        // Register first vehicle (free)
        vehicleService.registerVehicle(buildRequest(tenancyUuid, propertyCode));

        // Register second vehicle (should trigger fee)
        VehicleCreateRequest secondRequest = new VehicleCreateRequest(tenancyUuid, propertyCode, "XYZ789");
        VehicleRegistrationResult result = vehicleService.registerVehicle(secondRequest);

        assertEquals(new BigDecimal("25.00"), result.applicableFee());
    }

    @Test
    void registerVehicleFlagsPlateConflict() {
        UUID propertyCode = insertTestProperty();
        UUID tenancy1 = insertTestTenancy(propertyCode);
        UUID tenancy2 = insertTestTenancy(propertyCode);

        // Register plate under tenancy1
        vehicleService.registerVehicle(buildRequest(tenancy1, propertyCode));

        // Register same plate under tenancy2 — should flag conflict
        VehicleRegistrationResult result = vehicleService.registerVehicle(buildRequest(tenancy2, propertyCode));

        assertTrue(result.plateConflictFlagged());
        assertFalse(result.conflictingVehicles().isEmpty());
    }

    @Test
    void findByTenancyReturnsRegisteredVehicles() {
        UUID propertyCode = insertTestProperty();
        UUID tenancyUuid = insertTestTenancy(propertyCode);

        vehicleService.registerVehicle(buildRequest(tenancyUuid, propertyCode));

        List<Vehicle> vehicles = vehicleService.findByTenancy(tenancyUuid);

        assertFalse(vehicles.isEmpty());
        assertEquals("Toyota", vehicles.get(0).make());
    }

    @Test
    void findByPropertyReturnsVehiclesForProperty() {
        UUID propertyCode = insertTestProperty();
        UUID tenancyUuid = insertTestTenancy(propertyCode);

        vehicleService.registerVehicle(buildRequest(tenancyUuid, propertyCode));

        List<Vehicle> vehicles = vehicleService.findByProperty(propertyCode);

        assertFalse(vehicles.isEmpty());
        assertTrue(vehicles.stream().allMatch(v -> v.propertyUuid().equals(propertyCode)));
    }

    @Test
    void deleteVehicleRemovesItFromResults() {
        UUID propertyCode = insertTestProperty();
        UUID tenancyUuid = insertTestTenancy(propertyCode);

        VehicleRegistrationResult result = vehicleService.registerVehicle(buildRequest(tenancyUuid, propertyCode));
        UUID vehicleUuid = result.vehicle().uuid();

        boolean deleted = vehicleService.deleteVehicle(vehicleUuid);

        assertTrue(deleted);
        assertTrue(vehicleService.findById(vehicleUuid).isEmpty());
    }
}
