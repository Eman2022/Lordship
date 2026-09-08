package io.github.lordship.tenancy;

import io.github.lordship.accounts.AccountService;
import io.github.lordship.audit.AuditService;
import io.github.lordship.lots.Lot;
import io.github.lordship.lots.LotService;
import io.github.lordship.tenancy.internal.TenancyRepository;
import io.github.lordship.tenancy.internal.TenancyRow;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TenancyServiceTests {
    private TenancyRepository tenancyRepository;
    private AuditService auditService;
    private AccountService accountService;
    private LotService lotService;
    private TenancyService tenancyService;

    private UUID lotId;
    private UUID uuid1;
    private UUID uuid2;
    private UUID uuid3;

    @BeforeEach
    void setup() {
        tenancyRepository = mock(TenancyRepository.class);
        auditService = mock(AuditService.class);
        accountService = mock(AccountService.class);
        lotService = mock(LotService.class);

        tenancyService = new TenancyService(
                tenancyRepository,
                auditService,
                accountService,
                lotService
        );

        lotId = UUID.randomUUID();
        uuid1 = UUID.randomUUID();
        uuid2 = UUID.randomUUID();
        uuid3 = UUID.randomUUID();
    }

    private TenancyRow row(UUID id, LocalDate start, LocalDate end) {
        return new TenancyRow(
                id,
                lotId,
                start,
                end,
                false,
                false,
                true,
                false,
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(10).truncatedTo(ChronoUnit.DAYS),
                null
        );
    }

    private Lot lot(boolean isRentable, String notRentableReason) {
        return new Lot(
                lotId,
                UUID.randomUUID(),
                isRentable,
                notRentableReason,
                "14",
                null,
                null,
                null,
                null,
                1,
                null,
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                List.of()
        );
    }

    // Stubbed per test rather than in setup: MockitoExtension is strict, and the
    // tests that never reach create() would fail on an unused stub.
    private void lotIsRentable() {
        when(lotService.findById(lotId)).thenReturn(Optional.of(lot(true, null)));
    }

    // Map.of rejects null values, and clearing an end date is exactly a null.
    private static Map<String, Object> change(String key, Object value) {
        Map<String, Object> m = new HashMap<>();
        m.put(key, value);
        return m;
    }


    @Test
    void create_allowsFirstTenancy() {
        lotIsRentable();
        when(tenancyRepository.findActiveByLot(lotId)).thenReturn(List.of());

        TenancyRow saved = row(uuid1, LocalDate.now(), null);
        when(tenancyRepository.save(any())).thenReturn(saved);

        Tenancy result = tenancyService.create(lotId);

        assertEquals(uuid1, result.uuid());
        verify(auditService).recordInsert(eq("tenancy"), eq(uuid1), any());
    }

    // The overlap the office actually works in: the outgoing tenancy is still
    // open while the incoming one is set up.
    @Test
    void create_allowsSecondTenancy() {
        lotIsRentable();
        TenancyRow existing = row(uuid1, LocalDate.now().minusDays(10), null);
        when(tenancyRepository.findActiveByLot(lotId)).thenReturn(List.of(existing));

        TenancyRow saved = row(uuid2, LocalDate.now(), null);
        when(tenancyRepository.save(any())).thenReturn(saved);

        Tenancy result = tenancyService.create(lotId);

        assertEquals(uuid2, result.uuid());
        verify(auditService).recordInsert(eq("tenancy"), eq(uuid2), any());
    }

    @Test
    void create_rejectsThirdTenancy() {
        lotIsRentable();
        TenancyRow t1 = row(uuid1, LocalDate.now().minusDays(10), null);
        TenancyRow t2 = row(uuid2, LocalDate.now().minusDays(5), null);

        when(tenancyRepository.findActiveByLot(lotId)).thenReturn(List.of(t1, t2));

        assertThrows(IllegalStateException.class, () -> tenancyService.create(lotId));
        verify(tenancyRepository, never()).save(any());
    }

    @Test
    void create_rejectsNotRentableLot() {
        when(lotService.findById(lotId))
                .thenReturn(Optional.of(lot(false, "condemned after the flood")));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> tenancyService.create(lotId));

        assertTrue(e.getMessage().contains("condemned after the flood"));
        verify(tenancyRepository, never()).findActiveByLot(any());
        verify(tenancyRepository, never()).save(any());
    }

    @Test
    void create_rejectsUnknownLot() {
        when(lotService.findById(lotId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> tenancyService.create(lotId));
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

    // A tenancy created but not yet given its possession date has a null
    // start_date, which is the normal state during the overlap window.
    @Test
    void enforceSecondTenancyLimit_doesNothingWhenAStartDateIsMissing() {
        TenancyRow first = row(uuid1, LocalDate.now().minusMonths(2), null);
        TenancyRow second = row(uuid2, null, null);

        when(tenancyRepository.findActiveByLot(lotId)).thenReturn(List.of(first, second));

        assertDoesNotThrow(() -> tenancyService.enforceSecondTenancyLimit(lotId));
        verify(tenancyRepository, never()).close(any(), any());
    }

    @Test
    void enforceSecondTenancyLimit_doesNothingWithFewerThanTwoTenancies() {
        when(tenancyRepository.findActiveByLot(lotId)).thenReturn(List.of());

        assertDoesNotThrow(() -> tenancyService.enforceSecondTenancyLimit(lotId));
        verify(tenancyRepository, never()).close(any(), any());
    }

    // ---- end_date as a state transition -------------------------------------

    @Test
    void patchTenancy_closesTenancy_whenEndDateIsSet() {
        LocalDate end = LocalDate.now();
        TenancyRow before = row(uuid1, LocalDate.now().minusDays(20), null);
        TenancyRow after = row(uuid1, before.startDate(), end);

        when(tenancyRepository.findById(uuid1)).thenReturn(Optional.of(before));
        when(tenancyRepository.patch(eq(uuid1), any())).thenReturn(Optional.of(after));

        Optional<Tenancy> result = tenancyService.patchTenancy(uuid1, change("end_date", end.toString()));

        assertTrue(result.isPresent());
        assertEquals(end, result.get().endDate());
        verify(auditService).recordUpdate(eq("tenancy"), eq(uuid1), any(), any());
    }

    // A figure someone typed wrong, not a state change.
    @Test
    void patchTenancy_correctsAnExistingEndDate() {
        LocalDate corrected = LocalDate.now().minusDays(3);
        TenancyRow before = row(uuid1, LocalDate.now().minusDays(20), LocalDate.now().minusDays(1));
        TenancyRow after = row(uuid1, before.startDate(), corrected);

        when(tenancyRepository.findById(uuid1)).thenReturn(Optional.of(before));
        when(tenancyRepository.patch(eq(uuid1), any())).thenReturn(Optional.of(after));

        Optional<Tenancy> result = tenancyService.patchTenancy(uuid1, change("end_date", corrected.toString()));

        assertTrue(result.isPresent());
        assertEquals(corrected, result.get().endDate());
        verify(tenancyRepository, never()).findActiveByLot(any());
    }

    @Test
    void patchTenancy_reopensTenancy_whenLotHasRoom() {
        TenancyRow before = row(uuid1, LocalDate.now().minusDays(20), LocalDate.now().minusDays(1));
        TenancyRow after = row(uuid1, before.startDate(), null);
        TenancyRow other = row(uuid2, LocalDate.now().minusDays(5), null);

        when(tenancyRepository.findById(uuid1)).thenReturn(Optional.of(before));
        when(tenancyRepository.findActiveByLot(lotId)).thenReturn(List.of(other));
        when(tenancyRepository.patch(eq(uuid1), any())).thenReturn(Optional.of(after));

        Optional<Tenancy> result = tenancyService.patchTenancy(uuid1, change("end_date", null));

        assertTrue(result.isPresent());
        assertNull(result.get().endDate());
    }

    // Reopening is a third way onto a full lot, since create() never sees it.
    @Test
    void patchTenancy_rejectsReopen_whenLotAlreadyHasTwoActive() {
        TenancyRow before = row(uuid1, LocalDate.now().minusDays(20), LocalDate.now().minusDays(1));
        TenancyRow other1 = row(uuid2, LocalDate.now().minusDays(10), null);
        TenancyRow other2 = row(uuid3, LocalDate.now().minusDays(5), null);

        when(tenancyRepository.findById(uuid1)).thenReturn(Optional.of(before));
        when(tenancyRepository.findActiveByLot(lotId)).thenReturn(List.of(other1, other2));

        assertThrows(IllegalStateException.class,
                () -> tenancyService.patchTenancy(uuid1, change("end_date", null)));

        verify(tenancyRepository, never()).patch(any(), any());
    }

    @Test
    void patchTenancy_rejectsEndDateBeforeStartDate() {
        TenancyRow before = row(uuid1, LocalDate.now().minusDays(20), null);

        when(tenancyRepository.findById(uuid1)).thenReturn(Optional.of(before));

        assertThrows(IllegalArgumentException.class,
                () -> tenancyService.patchTenancy(uuid1,
                        change("end_date", LocalDate.now().minusDays(30).toString())));

        verify(tenancyRepository, never()).patch(any(), any());
    }

    @Test
    void patchTenancy_rejectsStartDateAfterExistingEndDate() {
        TenancyRow before = row(uuid1, LocalDate.now().minusDays(20), LocalDate.now().minusDays(10));

        when(tenancyRepository.findById(uuid1)).thenReturn(Optional.of(before));

        assertThrows(IllegalArgumentException.class,
                () -> tenancyService.patchTenancy(uuid1,
                        change("start_date", LocalDate.now().toString())));

        verify(tenancyRepository, never()).patch(any(), any());
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
        when(tenancyRepository.softDelete(uuid1)).thenReturn(true);

        boolean result = tenancyService.softDelete(uuid1);

        assertTrue(result);
        verify(tenancyRepository).softDelete(uuid1);
        verify(auditService).recordDelete(eq("tenancy"), eq(uuid1), any());
    }

    @Test
    void softDelete_returnsFalseIfNotFound() {
        when(tenancyRepository.findById(uuid1)).thenReturn(Optional.empty());

        boolean result = tenancyService.softDelete(uuid1);

        assertFalse(result);
        verify(tenancyRepository, never()).softDelete(any());
    }
}