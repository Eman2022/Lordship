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
                VALUES ((SELECT uuid FROM property WHERE property_code = :code), '1')
                RETURNING uuid
                """)
                .param("PropertyUuid", propertyUuid)
                .query(UUID.class).single();

        return jdbc.sql("""
                INSERT INTO tenancy (lot_id, start_date)
                VALUES (:lotId, CURRENT_DATE)
                RETURNING uuid
                """)
                .param("lotId", lotId)
                .query(UUID.class).single();
    }

    private VehicleRow buildRow(UUID tenancyUuid, UUID propertyUuid) {
        return new VehicleRow(tenancyUuid, propertyUuid, "Toyota", "Camry", 2020, "ABC123", "WA", "Blue", null);
    }

    @Test
    void savePersistsRowAndReturnsGeneratedFields() {
        UUID propertyCode = insertTestProperty();
        UUID tenancyUuid = insertTestLotAndTenancy(propertyCode);

        VehicleRow saved = vehicleRepository.save(buildRow(tenancyUuid, propertyCode));

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
        VehicleRow saved = vehicleRepository.save(buildRow(tenancyUuid, propertyCode));

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
        vehicleRepository.save(buildRow(tenancyUuid, propertyCode));
        vehicleRepository.save(new VehicleRow(tenancyUuid, propertyCode, "Honda", "Civic", 2019, "XYZ789", "WA", "Red", null));

        List<VehicleRow> vehicles = vehicleRepository.findByTenancy(tenancyUuid);

        assertEquals(2, vehicles.size());
    }

    @Test
    void countByTenancyReturnsCorrectCount() {
        UUID propertyCode = insertTestProperty();
        UUID tenancyUuid = insertTestLotAndTenancy(propertyCode);
        vehicleRepository.save(buildRow(tenancyUuid, propertyCode));
        vehicleRepository.save(new VehicleRow(tenancyUuid, propertyCode, "Honda", "Civic", 2019, "XYZ789", "WA", "Red", null));

        int count = vehicleRepository.countByTenancy(tenancyUuid);

        assertEquals(2, count);
    }

//    @Test
//    void findByPropertyReturnsVehiclesForProperty() {
//        UUID propertyCode = insertTestProperty();
//        UUID tenancyUuid = insertTestLotAndTenancy(propertyCode);
//        vehicleRepository.save(buildRow(tenancyUuid, propertyCode));
//
//        List<VehicleRow> vehicles = vehicleRepository.findByProperty(propertyCode);
//
//        assertFalse(vehicles.isEmpty());
//        assertTrue(vehicles.stream().allMatch(v -> v.propertyUuid().equals(propertyCode)));
//    }

    @Test
    void findConflictingPlateDetectsPlateUnderDifferentTenancy() {
        UUID propertyCode = insertTestProperty();
        UUID tenancy1 = insertTestLotAndTenancy(propertyCode);
        UUID tenancy2 = insertTestLotAndTenancy(propertyCode);

        vehicleRepository.save(buildRow(tenancy1, propertyCode));

        // Same plate, same property, but different tenancy — should flag
        List<VehicleRow> conflicts = vehicleRepository.findUnregisteredByPlate("ABC123", propertyCode, tenancy2);

        assertFalse(conflicts.isEmpty());
    }

    @Test
    void findConflictingPlateDoesNotFlagSameTenancy() {
        UUID propertyCode = insertTestProperty();
        UUID tenancyUuid = insertTestLotAndTenancy(propertyCode);

        vehicleRepository.save(buildRow(tenancyUuid, propertyCode));

        // Same plate, same tenancy — should not flag
        List<VehicleRow> conflicts = vehicleRepository.findUnregisteredByPlate("ABC123", propertyCode, tenancyUuid);

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void softDeleteRemovesVehicleFromResults() {
        UUID propertyCode = insertTestProperty();
        UUID tenancyUuid = insertTestLotAndTenancy(propertyCode);
        VehicleRow saved = vehicleRepository.save(buildRow(tenancyUuid, propertyCode));

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
}
