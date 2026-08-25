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
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;



@Transactional
public class VehicleRepositoryTest extends IntegrationTest {

    @Autowired
    VehicleRepository vehicleRepository;

    @Test
    void save_persistsRow_AndReturnsGeneratedFields() {
        // Arrange
        UUID tenancyId = testData.insertChainToTenancy().uuid();

        // Act
        VehicleRow saved = vehicleRepository.save(tenancyId, "ABC123");

        // Assert
        assertNotNull(saved.uuid());
        assertNotNull(saved.createdAt());
        assertNull(saved.deletedAt());
        Duration age = Duration.between(saved.createdAt(), OffsetDateTime.now(ZoneOffset.UTC)).abs();
        System.out.println("createdAt = " + saved.createdAt());
        System.out.println("now       = " + OffsetDateTime.now(ZoneOffset.UTC));
        System.out.println("age       = " + age);
        assertTrue(age.toSeconds() < 5, "expecting to be created within 5s");
        assertEquals("ABC123", saved.plateNumber());
    }

    @Test
    void findByIdReturnsSavedVehicle() {
        UUID tenancyId = testData.insertChainToTenancy().uuid();
        VehicleRow saved = vehicleRepository.save(tenancyId,  "ABC123");

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
        UUID tenancyId = testData.insertChainToTenancy().uuid();
        vehicleRepository.save(tenancyId,   "ABC123");
        vehicleRepository.save(tenancyId,   "DEF456");

        // Act
        List<VehicleRow> vehicles = vehicleRepository.findByTenancy(tenancyId);

        // Assert
        assertEquals(2, vehicles.size());
    }

    @Test
    void countByTenancyReturnsCorrectCount() {
        // Arrange
        UUID tenancyId = testData.insertChainToTenancy().uuid();
        vehicleRepository.save(tenancyId,    "ABC123");
        vehicleRepository.save(tenancyId,    "DEF456");

        // Act
        int count = vehicleRepository.countByTenancy(tenancyId);

        // Asset
        assertEquals(2, count);
    }

    @Test
    void findConflictingPlateDetectsPlateUnderDifferentTenancy() {
        UUID propertyUuid = testData.insertProperty("TP").uuid();
        UUID lot1Uuid = testData.insertLot(propertyUuid, "1").uuid();
        UUID lot2Uuid = testData.insertLot(propertyUuid, "2").uuid();
        UUID tenancy1 = testData.insertTenancy(lot1Uuid).uuid();
        UUID tenancy2 = testData.insertTenancy(lot2Uuid).uuid();

        vehicleRepository.save(tenancy1, "ABC123");

        // Same plate, same property, but different tenancy — should flag
        List<VehicleRow> conflicts = vehicleRepository.findUnregisteredByPlate("ABC123", tenancy2);

        assertFalse(conflicts.isEmpty());
    }

    @Test
    void findConflictingPlateDoesNotFlagSameTenancy() {
        // Arrange
        UUID tenancyId = testData.insertChainToTenancy().uuid();

        // Act
        vehicleRepository.save(tenancyId, "ABC123");

        // Assert
        // Same plate, same tenancy — should not flag
        List<VehicleRow> conflicts = vehicleRepository.findUnregisteredByPlate("ABC123", tenancyId);
        assertTrue(conflicts.isEmpty());
    }

    @Test
    void softDelete_removesVehicle_fromResults() {
        UUID tenancyId = testData.insertChainToTenancy().uuid();
        VehicleRow saved = vehicleRepository.save(tenancyId, "ABC123");

        vehicleRepository.softDelete(saved.uuid());

        Optional<VehicleRow> found = vehicleRepository.findById(saved.uuid());
        assertTrue(found.isEmpty());
    }

}
