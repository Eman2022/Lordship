package io.github.lordship.vehicles;

import io.github.lordship.audit.AuditService;
import io.github.lordship.vehicles.internal.VehiclePolicyRow;
import io.github.lordship.vehicles.internal.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VehiclePolicyTest {

    @Mock
    VehicleRepository vehicleRepository;

    @Mock
    AuditService auditService;

    @InjectMocks
    VehicleService vehicleService;

    private VehiclePolicyRow policyRow(UUID propertyUuid, int freeLimit, BigDecimal fee, String notes) {
        return new VehiclePolicyRow(UUID.randomUUID(), propertyUuid, freeLimit, fee, notes, null, null);
    }

    @Test
    void setPolicyCreatesNewPolicy_andRecordsAuditInsert() {
        UUID propertyUuid = UUID.randomUUID();
        VehiclePolicyRow saved = policyRow(propertyUuid, 2, new BigDecimal("25.00"), "Standard");

        when(vehicleRepository.findPolicyByProperty(propertyUuid)).thenReturn(Optional.empty());
        when(vehicleRepository.savePolicy(any())).thenReturn(saved);

        VehiclePolicy policy = vehicleService.setPolicy(propertyUuid, 2, new BigDecimal("25.00"), "Standard");

        assertNotNull(policy.uuid());
        assertEquals(propertyUuid, policy.propertyUuid());
        assertEquals(2, policy.freeVehicleLimit());
        assertEquals(new BigDecimal("25.00"), policy.extraVehicleFee());
        verify(auditService).recordInsert(eq("vehicle_policy"), eq(saved.uuid()), any());
        verify(auditService, never()).recordUpdate(any(), any(), any(), any());
    }

    @Test
    void getPolicyReturnsExistingPolicy() {
        UUID propertyUuid = UUID.randomUUID();
        when(vehicleRepository.findPolicyByProperty(propertyUuid))
                .thenReturn(Optional.of(policyRow(propertyUuid, 2, new BigDecimal("25.00"), null)));

        Optional<VehiclePolicy> found = vehicleService.getPolicy(propertyUuid);

        assertTrue(found.isPresent());
        assertEquals(2, found.get().freeVehicleLimit());
    }

    @Test
    void getPolicyReturnsEmptyWhenNoneSet() {
        UUID propertyUuid = UUID.randomUUID();
        when(vehicleRepository.findPolicyByProperty(propertyUuid)).thenReturn(Optional.empty());

        Optional<VehiclePolicy> found = vehicleService.getPolicy(propertyUuid);

        assertTrue(found.isEmpty());
    }

    @Test
    void setPolicyOverwritesExistingPolicy_andRecordsAuditUpdate() {
        UUID propertyUuid = UUID.randomUUID();
        VehiclePolicyRow existing = policyRow(propertyUuid, 2, new BigDecimal("25.00"), null);
        VehiclePolicyRow updated = policyRow(propertyUuid, 3, new BigDecimal("50.00"), "Updated");

        when(vehicleRepository.findPolicyByProperty(propertyUuid)).thenReturn(Optional.of(existing));
        when(vehicleRepository.savePolicy(any())).thenReturn(updated);

        VehiclePolicy policy = vehicleService.setPolicy(propertyUuid, 3, new BigDecimal("50.00"), "Updated");

        assertEquals(3, policy.freeVehicleLimit());
        assertEquals(new BigDecimal("50.00"), policy.extraVehicleFee());
        verify(auditService).recordUpdate(eq("vehicle_policy"), eq(updated.uuid()), any(), any());
        verify(auditService, never()).recordInsert(any(), any(), any());
    }
}