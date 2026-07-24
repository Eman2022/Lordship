package io.github.lordship.meters;
/*
import io.github.lordship.audit.AuditService;
import io.github.lordship.meters.internal.MeterRepository;
import io.github.lordship.meters.internal.MeterRow;
import io.github.lordship.tenancy.Tenancy;
import io.github.lordship.meters.MeterService;
import io.github.lordship.tenancy.internal.TenancyCreateRequest;
import io.github.lordship.tenancy.internal.TenancyRepository;
import io.github.lordship.tenancy.internal.TenancyRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MetersServiceTests {
    private MeterRepository meterRepository;
    private AuditService auditService;
    private MeterService meterService;

    private UUID lotId;
    private UUID uuid1;
    private UUID uuid2;

    @BeforeEach
    void setup() {
        meterRepository = mock(MeterRepository.class);
        auditService = mock(AuditService.class);

        meterService = new MeterService(
                meterRepository,
                null,          // encryptionService unused
                auditService
        );

        lotId = UUID.randomUUID();
        uuid1 = UUID.randomUUID();
        uuid2 = UUID.randomUUID();
    }

    private MeterRow row(UUID id, LocalDate start, LocalDate end) {
        return new MeterRow(
                id,
                lotId,
                start,
                end,
                LocalDate.now().minusDays(10).atStartOfDay(),
                LocalDate.now().minusDays(5).atStartOfDay(),
                null
        );
    }


    @Test
    void create_allowsFirstTenancy() {
        when(tenancyRepository.findActiveByLot(lotId)).thenReturn(List.of());

        TenancyCreateRequest req = new TenancyCreateRequest(lotId);

        TenancyRow saved = row(uuid1, LocalDate.now(), null);
        when(tenancyRepository.save(any())).thenReturn(saved);

        Tenancy result = tenancyService.create(req);

        assertEquals(uuid1, result.uuid());
        verify(auditService).recordInsert(eq("tenancy"), eq(uuid1), any());
    }

    @Test
    void create_allowsSecondTenancy() {
        TenancyRow existing = row(uuid1, LocalDate.now().minusDays(10), null);
        when(tenancyRepository.findActiveByLot(lotId)).thenReturn(List.of(existing));

        TenancyCreateRequest req = new TenancyCreateRequest(lotId);

        TenancyRow saved = row(uuid2, LocalDate.now(), null);
        when(tenancyRepository.save(any())).thenReturn(saved);

        Tenancy result = tenancyService.create(req);

        assertEquals(uuid2, result.uuid());
        verify(auditService).recordInsert(eq("tenancy"), eq(uuid2), any());
    }

    @Test
    void create_rejectsThirdTenancy() {
        TenancyRow t1 = row(uuid1, LocalDate.now().minusDays(10), null);
        TenancyRow t2 = row(uuid2, LocalDate.now().minusDays(5), null);

        when(tenancyRepository.findActiveByLot(lotId)).thenReturn(List.of(t1, t2));

        TenancyCreateRequest req = new TenancyCreateRequest(lotId);

        assertThrows(IllegalStateException.class, () -> tenancyService.create(req));
        verify(tenancyRepository, never()).save(any());
    }

    @Test
    void findActiveTenancyByLot_returnsMappedTenancies() {
        TenancyRow r1 = row(uuid1, LocalDate.now(), null);
        TenancyRow r2 = row(uuid2, LocalDate.now(), null);

        when(tenancyRepository.findActiveByLot(lotId)).thenReturn(List.of(r1, r2));

        List<Tenancy> result = tenancyService.findActiveTenancyByLot(lotId);

        assertEquals(2, result.size());
        assertEquals(uuid1, result.get(0).uuid());
    }

    @Test
    void findTenancyById_returnsMappedTenancy() {
        TenancyRow r = row(uuid1, LocalDate.now(), null);
        when(tenancyRepository.findById(uuid1)).thenReturn(Optional.of(r));

        Optional<Tenancy> result = tenancyService.findTenancyById(uuid1);

        assertTrue(result.isPresent());
        assertEquals(uuid1, result.get().uuid());
    }

    @Test
    void enforceSecondTenancyLimit_closesSecondTenancyAfterOneMonth() {
        TenancyRow first = row(uuid1, LocalDate.now().minusMonths(2), null);
        TenancyRow second = row(uuid2, LocalDate.now().minusMonths(1).minusDays(1), null);

        when(tenancyRepository.findActiveByLot(lotId)).thenReturn(List.of(first, second));

        TenancyRow closed = row(uuid2, second.startDate(), LocalDate.now());
        when(tenancyRepository.close(eq(uuid2), any())).thenReturn(closed);

        tenancyService.enforceSecondTenancyLimit(lotId);

        verify(tenancyRepository).close(eq(uuid2), any());
        verify(auditService).recordUpdate(eq("tenancy"), eq(uuid2), any(), any());
    }

    @Test
    void enforceSecondTenancyLimit_doesNothingIfSecondTenancyIsNew() {
        TenancyRow first = row(uuid1, LocalDate.now().minusMonths(2), null);
        TenancyRow second = row(uuid2, LocalDate.now().minusDays(10), null);

        when(tenancyRepository.findActiveByLot(lotId)).thenReturn(List.of(first, second));

        tenancyService.enforceSecondTenancyLimit(lotId);

        verify(tenancyRepository, never()).close(any(), any());
        verify(auditService, never()).recordUpdate(any(), any(), any(), any());
    }

    @Test
    void endTenancy_closesTenancy() {
        TenancyRow before = row(uuid1, LocalDate.now().minusDays(20), null);
        TenancyRow after = row(uuid1, before.startDate(), LocalDate.now());

        when(tenancyRepository.findById(uuid1)).thenReturn(Optional.of(before));
        when(tenancyRepository.close(uuid1, LocalDate.now())).thenReturn(after);

        Tenancy result = tenancyService.endTenancy(uuid1, LocalDate.now());

        assertEquals(LocalDate.now(), result.endDate());
        verify(auditService).recordUpdate(eq("tenancy"), eq(uuid1), any(), any());
    }

    @Test
    void endTenancy_throwsIfAlreadyClosed() {
        TenancyRow before = row(uuid1, LocalDate.now().minusDays(20), LocalDate.now().minusDays(5));

        when(tenancyRepository.findById(uuid1)).thenReturn(Optional.of(before));

        assertThrows(IllegalStateException.class,
                () -> tenancyService.endTenancy(uuid1, LocalDate.now()));
    }


    @Test
    void patchTenancy_updatesFields() {
        TenancyRow before = row(uuid1, LocalDate.now().minusDays(20), null);
        TenancyRow after = row(uuid1, before.startDate(), LocalDate.now());

        when(tenancyRepository.findById(uuid1)).thenReturn(Optional.of(before));
        when(tenancyRepository.patch(eq(uuid1), any())).thenReturn(Optional.of(after));

        Optional<Tenancy> result = tenancyService.patchTenancy(uuid1, Map.of("end_date", LocalDate.now().toString()));

        assertTrue(result.isPresent());
        assertEquals(LocalDate.now(), result.get().endDate());
        verify(auditService).recordUpdate(eq("tenancy"), eq(uuid1), any(), any());
    }

    @Test
    void patchTenancy_returnsEmptyIfNotFound() {
        when(tenancyRepository.findById(uuid1)).thenReturn(Optional.empty());

        Optional<Tenancy> result = tenancyService.patchTenancy(uuid1, Map.of("end_date", "2025-01-01"));

        assertTrue(result.isEmpty());
    }


    @Test
    void softDelete_deletesAndAudits() {
        TenancyRow before = row(uuid1, LocalDate.now().minusDays(20), null);

        when(tenancyRepository.findById(uuid1)).thenReturn(Optional.of(before));

        boolean result = tenancyService.softDelete(uuid1);

        assertTrue(result);
        verify(tenancyRepository).softDelete(uuid1);
        verify(auditService).recordDelete(eq("tenancy"), eq(uuid1), any());
    }

    @Test
    void softDelete_returnsFalseIfNotFound() {
        when(meterRepository.findById(uuid1)).thenReturn(Optional.empty());

        boolean result = meterService.softDelete(uuid1);

        assertFalse(result);
        verify(meterRepository, never()).softDelete(any());
    }
}
*/