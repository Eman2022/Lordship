package io.github.lordship.vehicles.internal;

import io.github.lordship.IntegrationTest;
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



@Transactional
public class VehicleRepositoryTest extends IntegrationTest {
    @Autowired
    VehicleRepository vehicleRepository;

    @Autowired
    JdbcClient jdbc;


    private VehicleRow buildRow(UUID tenancyUuid) {
        return new VehicleRow(tenancyUuid, "Toyota", "Camry", 2020, "ABC123", "WA", "Blue", null);
    }

    @Test
    void savePersistsRowAndReturnsGeneratedFields() {
        // Arrange
        UUID tenancyUuid = testData.insertChainToTenancy().uuid();

        // Act
        VehicleRow saved = vehicleRepository.save(buildRow(tenancyUuid));

        // Assert
        assertNotNull(saved.uuid());
        assertNotNull(saved.createdAt());
        assertNull(saved.deletedAt());
        assertEquals("Toyota", saved.make());
        assertEquals("Camry", saved.model());
        assertEquals("ABC123", saved.plateNumber());
    }

    @Test
    void findByIdReturnsSavedVehicle() {
        UUID tenancyUuid = testData.insertChainToTenancy().uuid();
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
        // Arrange
        UUID tenancyUuid = testData.insertChainToTenancy().uuid();
        vehicleRepository.save(buildRow(tenancyUuid));
        vehicleRepository.save(new VehicleRow(tenancyUuid, "Honda", "Civic", 2019, "XYZ789", "WA", "Red", null));

        // Act
        List<VehicleRow> vehicles = vehicleRepository.findByTenancy(tenancyUuid);

        // Assert
        assertEquals(2, vehicles.size());
    }

    @Test
    void countByTenancyReturnsCorrectCount() {
        // Arrange
        UUID tenancyUuid = testData.insertChainToTenancy().uuid();
        vehicleRepository.save(buildRow(tenancyUuid));
        vehicleRepository.save(new VehicleRow(tenancyUuid, "Honda", "Civic", 2019, "XYZ789", "WA", "Red", null));

        int count = vehicleRepository.countByTenancy(tenancyUuid);

        assertEquals(2, count);
    }


    @Test
    void findConflictingPlateDetectsPlateUnderDifferentTenancy() {
        UUID propertyUuid = testData.insertProperty("TP").uuid();
        UUID lot1Uuid = testData.insertLot(propertyUuid, "1").uuid();
        UUID lot2Uuid = testData.insertLot(propertyUuid, "2").uuid();
        UUID tenancy1 = testData.insertTenancy(lot1Uuid).uuid();
        UUID tenancy2 = testData.insertTenancy(lot2Uuid).uuid();

        vehicleRepository.save(buildRow(tenancy1));

        // Same plate, same property, but different tenancy — should flag
        List<VehicleRow> conflicts = vehicleRepository.findUnregisteredByPlate("ABC123", tenancy2);

        assertFalse(conflicts.isEmpty());
    }

    @Test
    void findConflictingPlateDoesNotFlagSameTenancy() {
        // Arrange
        UUID tenancyUuid = testData.insertChainToTenancy().uuid();

        // Act
        vehicleRepository.save(buildRow(tenancyUuid));

        // Assert
        // Same plate, same tenancy — should not flag
        List<VehicleRow> conflicts = vehicleRepository.findUnregisteredByPlate("ABC123", tenancyUuid);
        assertTrue(conflicts.isEmpty());
    }

    @Test
    void softDelete_removesVehicle_fromResults() {
        UUID tenancyUuid = testData.insertChainToTenancy().uuid();
        VehicleRow saved = vehicleRepository.save(buildRow(tenancyUuid));

        vehicleRepository.softDelete(saved.uuid());

        Optional<VehicleRow> found = vehicleRepository.findById(saved.uuid());
        assertTrue(found.isEmpty());
    }

}
