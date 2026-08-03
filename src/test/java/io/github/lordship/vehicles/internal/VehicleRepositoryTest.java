package io.github.lordship.vehicles.internal;

import io.github.lordship.properties.internal.PropertyRow;
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

public class VehicleRepositoryTest {
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

    private UUID insertTestLotAndTenancy(UUID propertyUuid) {
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

    private VehicleRow buildRow(UUID tenancyUuid) {
        return new VehicleRow(tenancyUuid, "Toyota", "Camry", 2020, "ABC123", "WA", "Blue", null);
    }

    @Test
    void savePersistsRowAndReturnsGeneratedFields() {
        UUID propertyCode = insertTestProperty();
        UUID tenancyUuid = insertTestLotAndTenancy(propertyCode);

        VehicleRow saved = vehicleRepository.save(buildRow(tenancyUuid));

        assertNotNull(saved.uuid());
        assertNotNull(saved.createdAt());
        assertNull(saved.deletedAt());
        assertEquals("Toyota", saved.make());
        assertEquals("Camry", saved.model());
        assertEquals("ABC123", saved.plateNumber());
    }

    @Test
    void findByIdReturnsSavedVehicle() {
        UUID propertyCode = insertTestProperty();
        UUID tenancyUuid = insertTestLotAndTenancy(propertyCode);
        VehicleRow saved = vehicleRepository.save(buildRow(tenancyUuid));

        Optional<VehicleRow> found = vehicleRepository.findById(saved.uuid());

        assertTrue(found.isPresent());
        assertEquals(saved.uuid(), found.get().uuid());
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        Optional<VehicleRow> found = vehicleRepository.findById(UUID.randomUUID());
        assertTrue(found.isEmpty());
    }

    @Test
    void findByTenancyReturnsVehiclesForTenant() {
        UUID propertyCode = insertTestProperty();
        UUID tenancyUuid = insertTestLotAndTenancy(propertyCode);
        vehicleRepository.save(buildRow(tenancyUuid));
        vehicleRepository.save(new VehicleRow(tenancyUuid, "Honda", "Civic", 2019, "XYZ789", "WA", "Red", null));

        List<VehicleRow> vehicles = vehicleRepository.findByTenancy(tenancyUuid);

        assertEquals(2, vehicles.size());
    }

    @Test
    void countByTenancyReturnsCorrectCount() {
        UUID propertyCode = insertTestProperty();
        UUID tenancyUuid = insertTestLotAndTenancy(propertyCode);
        vehicleRepository.save(buildRow(tenancyUuid));
        vehicleRepository.save(new VehicleRow(tenancyUuid, "Honda", "Civic", 2019, "XYZ789", "WA", "Red", null));

        int count = vehicleRepository.countByTenancy(tenancyUuid);

        assertEquals(2, count);
    }


    @Test
    void findConflictingPlateDetectsPlateUnderDifferentTenancy() {
        UUID propertyUuid = insertTestProperty();
        UUID tenancy1 = insertTestLotAndTenancy(propertyUuid);
        UUID tenancy2 = insertTestLotAndTenancy(propertyUuid);

        vehicleRepository.save(buildRow(tenancy1));

        // Same plate, same property, but different tenancy — should flag
        List<VehicleRow> conflicts = vehicleRepository.findUnregisteredByPlate("ABC123", tenancy2);

        assertFalse(conflicts.isEmpty());
    }

    @Test
    void findConflictingPlateDoesNotFlagSameTenancy() {
        UUID propertyUuid = insertTestProperty();
        System.out.println(propertyUuid);
        UUID tenancyUuid = insertTestLotAndTenancy(propertyUuid);

        vehicleRepository.save(buildRow(tenancyUuid));

        // Same plate, same tenancy — should not flag
        List<VehicleRow> conflicts = vehicleRepository.findUnregisteredByPlate("ABC123", tenancyUuid);

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void softDelete_removesVehicle_fromResults() {
        UUID propertyCode = insertTestProperty();
        UUID tenancyUuid = insertTestLotAndTenancy(propertyCode);
        VehicleRow saved = vehicleRepository.save(buildRow(tenancyUuid));

        vehicleRepository.softDelete(saved.uuid());

        Optional<VehicleRow> found = vehicleRepository.findById(saved.uuid());
        assertTrue(found.isEmpty());
    }

    @Test
    void savePolicyAndRetrieveByPropertyCode() {
        UUID propertyUuid = insertTestProperty();
        VehiclePolicyRow policyRow = new VehiclePolicyRow(null, propertyUuid, 2, new BigDecimal("25.00"), "Standard policy", null, null);

        vehicleRepository.savePolicy(policyRow);

        Optional<VehiclePolicyRow> found = vehicleRepository.findPolicyByProperty(propertyUuid);
        assertTrue(found.isPresent());
        assertEquals(2, found.get().freeVehicleLimit());
        assertEquals(new BigDecimal("25.00"), found.get().extraVehicleFee());
    }

    @Test
    void savePolicy_updatesExistingPolicy_onConflict() {
        UUID propertyUuid = insertTestProperty();
        vehicleRepository.savePolicy(new VehiclePolicyRow(null, propertyUuid, 2, new BigDecimal("25.00"), "Standard", null, null));

        vehicleRepository.savePolicy(new VehiclePolicyRow(null, propertyUuid, 3, new BigDecimal("50.00"), "Updated", null, null));

        Optional<VehiclePolicyRow> found = vehicleRepository.findPolicyByProperty(propertyUuid);
        assertTrue(found.isPresent());
        assertEquals(3, found.get().freeVehicleLimit());
        assertEquals(new BigDecimal("50.00"), found.get().extraVehicleFee());
        assertEquals("Updated", found.get().notes());
    }
}
