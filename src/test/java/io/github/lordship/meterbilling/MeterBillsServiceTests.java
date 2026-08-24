package io.github.lordship.meterbilling;

import io.github.lordship.audit.AuditService;
import io.github.lordship.meterbills.ChargeCalculation;
import io.github.lordship.meterbills.MeterBills;
import io.github.lordship.meterbills.MeterBillsService;
import io.github.lordship.meterbills.internal.MeterBillsCreateRequest;
import io.github.lordship.meterbills.internal.MeterBillsRepository;
import io.github.lordship.meterbills.internal.MeterBillsRow;
import io.github.lordship.meters.*;
import io.github.lordship.meters.internal.MeterCreateRequest;
import io.github.lordship.meters.internal.MeterRepository;
import io.github.lordship.meters.internal.MeterRow;
import io.micrometer.core.instrument.Meter;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MeterBillsServiceTests {
    private MeterBillsRepository meterBillsRepository;
    private MeterService meterService;
    private AuditService auditService;
    private MeterBillsService meterBillsService;

    private UUID billedMeter;
    private UUID meterId;
    private UUID lotMeterId;
    private UUID parentMeterId;

    @BeforeEach
    void setup() {
        meterBillsRepository = mock(MeterBillsRepository.class);
        meterService = mock(MeterService.class);
        auditService = mock(AuditService.class);

        meterBillsService = new MeterBillsService(
                meterBillsRepository,
                meterService,
                null,          // encryptionService unused
                auditService
        );

        billedMeter = UUID.randomUUID();
        meterId = UUID.randomUUID();
        lotMeterId = UUID.randomUUID();
        parentMeterId = UUID.randomUUID();
    }

    private MeterBillsRow row(UUID id, BigDecimal billedAmount, BigDecimal rateAmount,
                              LocalDate start, LocalDate end) {
        return new MeterBillsRow(id, meterId, billedAmount, rateAmount, MeterMeasurement.GAL,
                start, end, OffsetDateTime.now(), OffsetDateTime.now(), null);
    }

    private Meters meters(UUID uuid, MeterMeasurement measurement) {
        return new Meters(uuid, UUID.randomUUID(), "Meter", null, "SN-1", 0.0, 0.0, null,
                null, null, null, MeterType.WATER, measurement, false, 99999, 1.0, 15, false);
    }

    private MeterRead read(int amount, OffsetDateTime readAt, boolean estimated) {
        return new MeterRead(UUID.randomUUID(), lotMeterId, amount, readAt, estimated, 0, OffsetDateTime.now(), OffsetDateTime.now(), null);
    }

    @Test
    void findMeterBillById_returnsMappedMeterBills() {
        MeterBillsRow saved = row(billedMeter, new BigDecimal("500.00"), new BigDecimal("0.0148"), LocalDate.now().minusMonths(1), LocalDate.now());
        when(meterBillsRepository.findById(billedMeter)).thenReturn(Optional.of(saved));

        Optional<MeterBills> result = meterBillsService.findMeterBillById(billedMeter);

        assertTrue(result.isPresent());
        assertEquals(billedMeter, result.get().uuid());
    }

    @Test
    void findMeterBillById_returnsEmpty_whenNotFound() {
        when(meterBillsRepository.findById(billedMeter)).thenReturn(Optional.empty());

        assertTrue(meterBillsService.findMeterBillById(billedMeter).isEmpty());
    }

    @Test
    void findMeterByBilling_returnsFullHistory() {
        MeterBillsRow a = row(UUID.randomUUID(), new BigDecimal("500.00"), new BigDecimal("0.0148"), LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1));
        MeterBillsRow b = row(UUID.randomUUID(), new BigDecimal("520.00"), new BigDecimal("0.0150"), LocalDate.now().minusMonths(1), LocalDate.now());
        when(meterBillsRepository.findByBilledMeter(meterId)).thenReturn(java.util.List.of(b, a));

        var result = meterBillsService.findMeterByBilling(meterId);

        assertEquals(2, result.size());
    }

    @Test
    void patchMeterBill_throws_whenBilledAmountNotParseable() {
        MeterBillsRow before = row(billedMeter, new BigDecimal("500.00"), new BigDecimal("0.0148"), LocalDate.now().minusMonths(1), LocalDate.now());
        when(meterBillsRepository.findById(billedMeter)).thenReturn(Optional.of(before));

        assertThrows(IllegalArgumentException.class, () ->
                meterBillsService.patchMeterBill(billedMeter, Map.of("billed_amount", "not-a-number")));
    }

    @Test
    void patchMeterBill_updatesPeriodEnd_correctly_notPeriodStart() {
        // regression test for the before.periodStart()/before.periodEnd() copy-paste bug
        LocalDate originalStart = LocalDate.now().minusMonths(1);
        LocalDate originalEnd = LocalDate.now();
        LocalDate newEnd = LocalDate.now().plusDays(5);

        MeterBillsRow before = row(billedMeter, new BigDecimal("500.00"), new BigDecimal("0.0148"), originalStart, originalEnd);
        MeterBillsRow after = row(billedMeter, new BigDecimal("500.00"), new BigDecimal("0.0148"), originalStart, newEnd);

        when(meterBillsRepository.findById(billedMeter)).thenReturn(Optional.of(before));
        when(meterBillsRepository.patch(eq(billedMeter), any())).thenReturn(Optional.of(after));

        Optional<MeterBills> result = meterBillsService.patchMeterBill(
                billedMeter, Map.of("period_end", newEnd.toString()));

        assertTrue(result.isPresent());
        assertEquals(newEnd, result.get().periodEnd());
        verify(meterBillsRepository).patch(eq(billedMeter), argThat(changes ->
                changes.containsKey("period_end") && changes.get("period_end").equals(newEnd)));
    }

    @Test
    void patchMeterBill_removesNoOpChange_doesNotAudit() {
        LocalDate end = LocalDate.now();
        MeterBillsRow before = row(billedMeter, new BigDecimal("500.00"), new BigDecimal("0.0148"), LocalDate.now().minusMonths(1), end);

        when(meterBillsRepository.findById(billedMeter)).thenReturn(Optional.of(before));

        // sending the same period_end that's already set — should be a no-op
        Optional<MeterBills> result = meterBillsService.patchMeterBill(
                billedMeter, Map.of("period_end", end.toString()));

        assertTrue(result.isPresent());
        verify(auditService, never()).recordUpdate(any(), any(), any(), any());
        verify(meterBillsRepository, never()).patch(any(), any());
    }

    @Test
    void patchMeterBill_returnsEmpty_whenBillDoesNotExist() {
        when(meterBillsRepository.findById(billedMeter)).thenReturn(Optional.empty());

        Optional<MeterBills> result = meterBillsService.patchMeterBill(billedMeter, Map.of("billed_amount", "600"));

        assertTrue(result.isEmpty());
    }

    @Test
    void softDelete_returnsTrueAndAudits_whenBillExists() {
        MeterBillsRow existing = row(billedMeter, new BigDecimal("500.00"), new BigDecimal("0.0148"), LocalDate.now().minusMonths(1), LocalDate.now());
        when(meterBillsRepository.findById(billedMeter)).thenReturn(Optional.of(existing));

        boolean result = meterBillsService.softDelete(billedMeter);

        assertTrue(result);
        verify(meterBillsRepository).softDelete(billedMeter);
        verify(auditService).recordDelete(eq("meter_billing"), eq(billedMeter), any());
    }

    @Test
    void softDelete_returnsFalse_whenBillDoesNotExist() {
        when(meterBillsRepository.findById(billedMeter)).thenReturn(Optional.empty());

        assertFalse(meterBillsService.softDelete(billedMeter));
        verify(meterBillsRepository, never()).softDelete(any());
    }

    @Test
    void calculateCharge_flagsEstimatedReads() {
        LocalDate periodStart = LocalDate.now().minusMonths(1);
        LocalDate periodEnd = LocalDate.now();

        when(meterService.resolveParentMeter(lotMeterId, periodEnd)).thenReturn(Optional.of(parentMeterId));

        MeterBillsRow rateRow = row(UUID.randomUUID(), new BigDecimal("500.00"), new BigDecimal("0.02"), periodStart, periodEnd);
        when(meterBillsRepository.findRateForPeriod(parentMeterId, periodStart, periodEnd))
                .thenReturn(Optional.of(rateRow));
        when(meterService.findMetersById(lotMeterId)).thenReturn(Optional.of(meters(lotMeterId, MeterMeasurement.GAL)));

        Usage usage = new Usage(lotMeterId,
                read(1000, periodStart.atStartOfDay().atOffset(ZoneOffset.UTC), true), // estimated
                read(1200, periodEnd.atStartOfDay().atOffset(ZoneOffset.UTC), false),
                200.0);
        when(meterService.getUsageForPeriod(eq(lotMeterId), any(), any())).thenReturn(usage);

        ChargeCalculation charge = meterBillsService.calculateCharge(lotMeterId, periodStart, periodEnd);

        assertTrue(charge.startReadEstimated());
        assertFalse(charge.endReadEstimated());
    }

    @Test
    void calculateCharge_throws_whenNoParentMeterResolved() {
        LocalDate periodStart = LocalDate.now().minusMonths(1);
        LocalDate periodEnd = LocalDate.now();

        when(meterService.resolveParentMeter(lotMeterId, periodEnd)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () ->
                meterBillsService.calculateCharge(lotMeterId, periodStart, periodEnd));
    }

    @Test
    void calculateCharge_throws_whenNoRateOnFile() {
        LocalDate periodStart = LocalDate.now().minusMonths(1);
        LocalDate periodEnd = LocalDate.now();

        when(meterService.resolveParentMeter(lotMeterId, periodEnd)).thenReturn(Optional.of(parentMeterId));
        when(meterBillsRepository.findRateForPeriod(parentMeterId, periodStart, periodEnd))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                meterBillsService.calculateCharge(lotMeterId, periodStart, periodEnd));
    }

    @Test
    void calculateCharge_throws_whenRateUnitDoesNotMatchLotMeterMeasurement() {
        LocalDate periodStart = LocalDate.now().minusMonths(1);
        LocalDate periodEnd = LocalDate.now();

        when(meterService.resolveParentMeter(lotMeterId, periodEnd)).thenReturn(Optional.of(parentMeterId));

        MeterBillsRow rateRow = new MeterBillsRow(UUID.randomUUID(), parentMeterId, new BigDecimal("500.00"), new BigDecimal("0.10"),
                MeterMeasurement.KWH, periodStart, periodEnd, OffsetDateTime.now(), OffsetDateTime.now(), null);
        when(meterBillsRepository.findRateForPeriod(parentMeterId, periodStart, periodEnd))
                .thenReturn(Optional.of(rateRow));

        when(meterService.findMetersById(lotMeterId)).thenReturn(Optional.of(meters(lotMeterId, MeterMeasurement.GAL)));

        assertThrows(IllegalStateException.class, () ->
                meterBillsService.calculateCharge(lotMeterId, periodStart, periodEnd));
    }

    @Test
    void calculateCharge_throws_whenLotMeterDoesNotExist() {
        LocalDate periodStart = LocalDate.now().minusMonths(1);
        LocalDate periodEnd = LocalDate.now();

        when(meterService.resolveParentMeter(lotMeterId, periodEnd)).thenReturn(Optional.of(parentMeterId));

        MeterBillsRow rateRow = row(UUID.randomUUID(), new BigDecimal("500.00"), new BigDecimal("0.0148"), periodStart, periodEnd);
        when(meterBillsRepository.findRateForPeriod(parentMeterId, periodStart, periodEnd))
                .thenReturn(Optional.of(rateRow));

        when(meterService.findMetersById(lotMeterId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                meterBillsService.calculateCharge(lotMeterId, periodStart, periodEnd));
    }
}