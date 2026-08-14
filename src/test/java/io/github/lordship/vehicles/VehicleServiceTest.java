package io.github.lordship.vehicles;

import io.github.lordship.audit.AuditService;
import io.github.lordship.vehicles.internal.VehicleCreateRequest;
import io.github.lordship.vehicles.internal.VehiclePolicyRow;
import io.github.lordship.vehicles.internal.VehicleRepository;
import io.github.lordship.vehicles.internal.VehicleRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
                plateNumber, null, null, null,
                LocalDateTime.now(), null
        );
    }

    private VehiclePolicyRow policyRow(UUID propertyUuid, int freeLimit, BigDecimal fee) {
        return new VehiclePolicyRow(UUID.randomUUID(), propertyUuid, freeLimit, fee, null, null, null);
    }

    @Test
    void registerVehicleReturnsVehicleWithGeneratedFields() {
        UUID tenancyUuid = UUID.randomUUID();
        VehicleCreateRequest request = new VehicleCreateRequest(tenancyUuid, "ABC123");
        VehicleRow saved = row(UUID.randomUUID(), tenancyUuid, "ABC123");

        when(vehicleRepository.findUnregisteredByPlate("ABC123", tenancyUuid)).thenReturn(List.of());
        when(vehicleRepository.countByTenancy(tenancyUuid)).thenReturn(0);
        when(vehicleRepository.findPropertyUuidByTenancy(tenancyUuid)).thenReturn(Optional.empty());
        when(vehicleRepository.save(any())).thenReturn(saved);

        VehicleRegistrationResult result = vehicleService.registerVehicle(request);

        assertNotNull(result.vehicle().uuid());
        assertEquals("ABC123", result.vehicle().plateNumber());
        verify(auditService).recordInsert(eq("vehicle"), eq(saved.uuid()), any());
    }

    @Test
    void registerVehicleNoFeeWhenUnderFreeLimit() {
        UUID tenancyUuid = UUID.randomUUID();
        UUID propertyUuid = UUID.randomUUID();
        VehicleCreateRequest request = new VehicleCreateRequest(tenancyUuid, "ABC123");

        when(vehicleRepository.findUnregisteredByPlate("ABC123", tenancyUuid)).thenReturn(List.of());
        when(vehicleRepository.countByTenancy(tenancyUuid)).thenReturn(0);
        when(vehicleRepository.findPropertyUuidByTenancy(tenancyUuid)).thenReturn(Optional.of(propertyUuid));
        when(vehicleRepository.findPolicyByProperty(propertyUuid))
                .thenReturn(Optional.of(policyRow(propertyUuid, 2, new BigDecimal("25.00"))));
        when(vehicleRepository.save(any())).thenReturn(row(UUID.randomUUID(), tenancyUuid, "ABC123"));

        VehicleRegistrationResult result = vehicleService.registerVehicle(request);

        assertEquals(BigDecimal.ZERO, result.applicableFee());
    }

    @Test
    void registerVehicleFeeAppliedWhenOverFreeLimit() {
        UUID tenancyUuid = UUID.randomUUID();
        UUID propertyUuid = UUID.randomUUID();
        VehicleCreateRequest request = new VehicleCreateRequest(tenancyUuid, "XYZ789");

        when(vehicleRepository.findUnregisteredByPlate("XYZ789", tenancyUuid)).thenReturn(List.of());
        when(vehicleRepository.countByTenancy(tenancyUuid)).thenReturn(1);
        when(vehicleRepository.findPropertyUuidByTenancy(tenancyUuid)).thenReturn(Optional.of(propertyUuid));
        when(vehicleRepository.findPolicyByProperty(propertyUuid))
                .thenReturn(Optional.of(policyRow(propertyUuid, 1, new BigDecimal("25.00"))));
        when(vehicleRepository.save(any())).thenReturn(row(UUID.randomUUID(), tenancyUuid, "XYZ789"));

        VehicleRegistrationResult result = vehicleService.registerVehicle(request);

        assertEquals(new BigDecimal("25.00"), result.applicableFee());
    }

    @Test
    void registerVehicleFlagsPlateConflict() {
        UUID tenancyUuid = UUID.randomUUID();
        VehicleCreateRequest request = new VehicleCreateRequest(tenancyUuid, "ABC123");
        VehicleRow conflictingRow = row(UUID.randomUUID(), UUID.randomUUID(), "ABC123");

        when(vehicleRepository.findUnregisteredByPlate("ABC123", tenancyUuid)).thenReturn(List.of(conflictingRow));
        when(vehicleRepository.countByTenancy(tenancyUuid)).thenReturn(0);
        when(vehicleRepository.findPropertyUuidByTenancy(tenancyUuid)).thenReturn(Optional.empty());
        when(vehicleRepository.save(any())).thenReturn(row(UUID.randomUUID(), tenancyUuid, "ABC123"));

        VehicleRegistrationResult result = vehicleService.registerVehicle(request);

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
                before.uuid(), before.tenancyUuid(), null, null, null,
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

    @Test
    void setPolicy_recordsInsert_whenNoPolicyExisted() {
        UUID propertyUuid = UUID.randomUUID();
        VehiclePolicyRow saved = policyRow(propertyUuid, 2, new BigDecimal("25.00"));

        when(vehicleRepository.findPolicyByProperty(propertyUuid)).thenReturn(Optional.empty());
        when(vehicleRepository.savePolicy(any())).thenReturn(saved);

        vehicleService.setPolicy(propertyUuid, 2, new BigDecimal("25.00"), null);

        verify(auditService).recordInsert(eq("vehicle_policy"), eq(saved.uuid()), any());
    }

    @Test
    void setPolicy_recordsUpdate_whenPolicyAlreadyExisted() {
        UUID propertyUuid = UUID.randomUUID();
        VehiclePolicyRow existing = policyRow(propertyUuid, 2, new BigDecimal("25.00"));
        VehiclePolicyRow updated = policyRow(propertyUuid, 3, new BigDecimal("30.00"));

        when(vehicleRepository.findPolicyByProperty(propertyUuid)).thenReturn(Optional.of(existing));
        when(vehicleRepository.savePolicy(any())).thenReturn(updated);

        vehicleService.setPolicy(propertyUuid, 3, new BigDecimal("30.00"), null);

        verify(auditService).recordUpdate(eq("vehicle_policy"), eq(updated.uuid()), any(), any());
    }
}