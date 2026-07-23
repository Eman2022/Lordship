package io.github.lordship.tenants;
import io.github.lordship.audit.AuditService;
import io.github.lordship.tenants.internal.TenantCreateRequest;
import io.github.lordship.tenants.internal.TenantRepository;
import io.github.lordship.tenants.internal.TenantRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TenantServiceTests {
    @InjectMocks
    TenantService tenantService;

    @Mock
    TenantRepository tenantRepository;

    @Mock
    AuditService auditService;

    private TenantRow row(UUID tenancyId, UUID personId, LocalDate start, LocalDate end) {
        return new TenantRow(
                UUID.randomUUID(),
                tenancyId,
                personId,
                start,
                end,
                LocalDate.now().atStartOfDay(),
                LocalDate.now().atStartOfDay(),
                null
        );
    }

    @Test
    void createPersistsTenantAndReturnsDomainObject() {
        UUID tenancyId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();

        TenantRow existing = row(tenancyId, UUID.randomUUID(), LocalDate.now().minusDays(10), null);
        TenantRow saved = row(tenancyId, personId, LocalDate.now(), null);

        when(tenantRepository.findByTenancy(tenancyId)).thenReturn(List.of(existing));
        when(tenantRepository.end(eq(existing.uuid()), any())).thenReturn(existing);
        when(tenantRepository.save(any())).thenReturn(saved);

        Tenant result = tenantService.create(new TenantCreateRequest(tenancyId, personId));

        assertEquals(saved.uuid(), result.uuid());
        assertEquals(personId, result.personId());
        assertEquals(tenancyId, result.tenancyId());
        assertEquals(LocalDate.now(), result.startDate());

        verify(tenantRepository).end(eq(existing.uuid()), any());
        verify(tenantRepository).save(any());
        verify(auditService).recordInsert(eq("tenant"), eq(saved.uuid()), any());
    }

    @Test
    void createAddsTenantToRepository() {
        UUID tenancyId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();

        TenantRow saved = row(tenancyId, personId, LocalDate.now(), null);

        when(tenantRepository.findByTenancy(tenancyId)).thenReturn(List.of());
        when(tenantRepository.save(any())).thenReturn(saved);

        Tenant created = tenantService.create(new TenantCreateRequest(tenancyId, personId));

        verify(tenantRepository).save(any());
        assertEquals(saved.uuid(), created.uuid());
    }

    @Test
    void createEndsExistingTenants() {
        UUID tenancyId = UUID.randomUUID();

        TenantRow existing = row(tenancyId, UUID.randomUUID(), LocalDate.now().minusDays(10), null);
        TenantRow saved = row(tenancyId, UUID.randomUUID(), LocalDate.now(), null);

        when(tenantRepository.findByTenancy(tenancyId)).thenReturn(List.of(existing));
        when(tenantRepository.end(eq(existing.uuid()), any())).thenReturn(existing);
        when(tenantRepository.save(any())).thenReturn(saved);

        tenantService.create(new TenantCreateRequest(tenancyId, UUID.randomUUID()));

        verify(tenantRepository).end(eq(existing.uuid()), any());
        verify(tenantRepository).save(any());
    }

    @Test
    void findById_returnsTenant_whenPresent() {
        UUID id = UUID.randomUUID();
        TenantRow saved = row(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), null);

        when(tenantRepository.findById(id)).thenReturn(Optional.of(saved));

        Optional<Tenant> result = tenantService.findById(id);

        assertTrue(result.isPresent());
        assertEquals(saved.uuid(), result.get().uuid());
    }

    @Test
    void findById_returnsEmpty_whenNotFound() {
        when(tenantRepository.findById(any())).thenReturn(Optional.empty());

        assertTrue(tenantService.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void delete_returnsTrue_andRecordsAudit() {
        UUID id = UUID.randomUUID();
        TenantRow saved = row(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), null);

        when(tenantRepository.findById(id)).thenReturn(Optional.of(saved));

        boolean result = tenantService.delete(id);

        assertTrue(result);
        verify(tenantRepository).softDelete(id);
        verify(auditService).recordDelete(eq("tenant"), eq(id), any());
    }

    @Test
    void delete_returnsFalse_whenNotFound() {
        when(tenantRepository.findById(any())).thenReturn(Optional.empty());

        boolean result = tenantService.delete(UUID.randomUUID());

        assertFalse(result);
        verify(tenantRepository, never()).softDelete(any());
        verify(auditService, never()).recordDelete(any(), any(), any());
    }

    @Test
    void patchTenancy_updatesFields() {
        UUID id = UUID.randomUUID();

        TenantRow before = row(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), null);
        TenantRow after = row(before.tenancyId(), before.personId(), before.startDate(), LocalDate.of(2026, 1, 1));

        when(tenantRepository.findById(id)).thenReturn(Optional.of(before));
        when(tenantRepository.patch(eq(id), any())).thenReturn(Optional.of(after));

        Map<String, Object> changes = new HashMap<>();
        changes.put("end_date", "2026-01-01");

        Optional<Tenant> result = tenantService.patch(id, changes);

        assertTrue(result.isPresent());
        assertEquals(LocalDate.of(2026, 1, 1), result.get().endDate());
        verify(auditService).recordUpdate(eq("tenant"), eq(id), any(), any());
    }

    @Test
    void patchTenancy_returnsEmpty_whenNotFound() {
        when(tenantRepository.findById(any())).thenReturn(Optional.empty());

        Optional<Tenant> result = tenantService.patch(UUID.randomUUID(), Map.of("end_date", "2026-01-01"));

        assertTrue(result.isEmpty());
    }

    @Test
    void patchTenancy_clearsField_whenNullProvided() {
        UUID id = UUID.randomUUID();

        TenantRow before = row(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), LocalDate.now());
        TenantRow after = row(before.tenancyId(), before.personId(), before.startDate(), null);

        when(tenantRepository.findById(id)).thenReturn(Optional.of(before));
        when(tenantRepository.patch(eq(id), any())).thenReturn(Optional.of(after));

        Map<String, Object> changes = new HashMap<>();
        changes.put("end_date", null);

        Optional<Tenant> result = tenantService.patch(id, changes);

        assertTrue(result.isPresent());
        assertNull(result.get().endDate());
    }

    @Test
    void patchTenancy_doesNotModifyOmittedFields() {
        UUID id = UUID.randomUUID();

        TenantRow before = row(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), LocalDate.of(2025, 1, 1));
        TenantRow after = row(before.tenancyId(), before.personId(), LocalDate.of(2026, 1, 1), before.endDate());

        when(tenantRepository.findById(id)).thenReturn(Optional.of(before));
        when(tenantRepository.patch(eq(id), any())).thenReturn(Optional.of(after));

        Map<String, Object> changes = new HashMap<>();
        changes.put("start_date", "2026-01-01");

        Optional<Tenant> result = tenantService.patch(id, changes);

        assertTrue(result.isPresent());
        assertEquals(LocalDate.of(2026, 1, 1), result.get().startDate());
        assertEquals(LocalDate.of(2025, 1, 1), result.get().endDate());
    }
}