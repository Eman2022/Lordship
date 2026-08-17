package io.github.lordship.vehicles;

import io.github.lordship.audit.AuditService;
import io.github.lordship.vehicles.internal.VehicleCreateRequest;
import io.github.lordship.vehicles.internal.VehicleCreationResult;
import io.github.lordship.vehicles.internal.VehicleRepository;
import io.github.lordship.vehicles.internal.VehicleRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VehicleServiceTest {

    @Mock
    VehicleRepository vehicleRepository;

    @Mock
    AuditService auditService;

    @InjectMocks
    VehicleService vehicleService;

    private VehicleRow row(UUID uuid, UUID tenancyUuid, String plateNumber) {
        return new VehicleRow(
                uuid, tenancyUuid, null, null, null,
                plateNumber, null, null,
                null, OffsetDateTime.now(ZoneOffset.UTC), null
        );
    }


    @Test
    void registerVehicleReturnsVehicleWithGeneratedFields() {
        UUID tenancyUuid = UUID.randomUUID();
        VehicleRow saved = row(UUID.randomUUID(), tenancyUuid, "ABC123");

        when(vehicleRepository.findUnregisteredByPlate("ABC123", tenancyUuid)).thenReturn(List.of());
        when(vehicleRepository.save(any(), any())).thenReturn(saved);

        VehicleCreationResult result = vehicleService.registerVehicle(tenancyUuid, "ABC123");

        assertNotNull(result.vehicle().uuid());
        assertEquals("ABC123", result.vehicle().plateNumber());
        verify(auditService).recordInsert(eq("vehicle"), eq(saved.uuid()), any());
    }


    @Test
    void registerVehicleFlagsPlateConflict() {
        // Arrange
        UUID tenancyUuid = UUID.randomUUID();
        VehicleRow conflictingRow = row(UUID.randomUUID(), UUID.randomUUID(), "ABC123");

        when(vehicleRepository.findUnregisteredByPlate("ABC123", tenancyUuid)).thenReturn(List.of(conflictingRow));
        when(vehicleRepository.save(any(), any())).thenReturn(row(UUID.randomUUID(), tenancyUuid, "ABC123"));

        // Act
        VehicleCreationResult result = vehicleService.registerVehicle(tenancyUuid, "ABC123");

        // Assert
        assertTrue(result.plateConflictFlagged());
        assertFalse(result.conflictingVehicles().isEmpty());
    }

    @Test
    void findByTenancyReturnsRegisteredVehicles() {
        UUID tenancyUuid = UUID.randomUUID();
        when(vehicleRepository.findByTenancy(tenancyUuid))
                .thenReturn(List.of(row(UUID.randomUUID(), tenancyUuid, "ABC123")));

        List<Vehicle> vehicles = vehicleService.findByTenancy(tenancyUuid);

        assertFalse(vehicles.isEmpty());
        assertEquals("ABC123", vehicles.get(0).plateNumber());
    }

    @Test
    void findByPropertyReturnsVehiclesForProperty() {
        UUID propertyUuid = UUID.randomUUID();
        when(vehicleRepository.findByProperty(propertyUuid))
                .thenReturn(List.of(row(UUID.randomUUID(), UUID.randomUUID(), "ABC123")));

        List<Vehicle> vehicles = vehicleService.findByProperty(propertyUuid);

        assertFalse(vehicles.isEmpty());
    }

    @Test
    void deleteVehicle_returnsTrue_andRecordsAudit_whenVehicleExists() {
        VehicleRow existing = row(UUID.randomUUID(), UUID.randomUUID(), "ABC123");
        when(vehicleRepository.findById(existing.uuid())).thenReturn(Optional.of(existing));
        when(vehicleRepository.softDelete(existing.uuid())).thenReturn(true);

        boolean deleted = vehicleService.deleteVehicle(existing.uuid());

        assertTrue(deleted);
        verify(vehicleRepository).softDelete(existing.uuid());
        verify(auditService).recordDelete(eq("vehicle"), eq(existing.uuid()), any());
    }

    @Test
    void deleteVehicle_returnsFalse_andDoesNotRecordAudit_whenNotFound() {
        UUID unknownUuid = UUID.randomUUID();
        when(vehicleRepository.findById(unknownUuid)).thenReturn(Optional.empty());

        boolean deleted = vehicleService.deleteVehicle(unknownUuid);

        assertFalse(deleted);
        verify(vehicleRepository, never()).softDelete(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void patchVehicle_recordsAudit_whenFieldActuallyChanges() {
        VehicleRow before = row(UUID.randomUUID(), UUID.randomUUID(), "ABC123");
        VehicleRow after = new VehicleRow(
                before.uuid(), before.tenancyId(), null, null, null,
                before.plateNumber(), null, "Red", null,
                before.createdAt(), null
        );

        when(vehicleRepository.findById(before.uuid())).thenReturn(Optional.of(before));
        when(vehicleRepository.patch(eq(before.uuid()), any())).thenReturn(Optional.of(after));

        vehicleService.patchVehicle(before.uuid(), Map.of("color", "Red"));

        verify(auditService).recordUpdate(eq("vehicle"), eq(before.uuid()), any(), any());
    }

    @Test
    void patchVehicle_doesNotRecordAudit_whenNoDiffProduced() {
        VehicleRow stubRow = row(UUID.randomUUID(), UUID.randomUUID(), "ABC123");

        when(vehicleRepository.findById(stubRow.uuid())).thenReturn(Optional.of(stubRow));
        when(vehicleRepository.patch(eq(stubRow.uuid()), any())).thenReturn(Optional.of(stubRow));

        vehicleService.patchVehicle(stubRow.uuid(), Map.of("plate_number", "ABC123"));

        verifyNoInteractions(auditService);
    }

}