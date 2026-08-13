package io.github.lordship.meters;

import io.github.lordship.audit.AuditService;
import io.github.lordship.meters.internal.*;
import io.github.lordship.properties.internal.PropertyRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

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
public class MetersServiceTests {
    private MeterRepository meterRepository;
    private MeterRelationRepository meterRelation;
    private MeterReadRepository meterRead;
    private AuditService auditService;
    private MeterService meterService;

    private UUID meterId;
    private UUID uuid1;
    private UUID uuid2;

    @BeforeEach
    void setup() {
        meterRepository = mock(MeterRepository.class);
        meterRead = mock(MeterReadRepository.class);
        meterRelation = mock(MeterRelationRepository.class);
        auditService = mock(AuditService.class);

        meterService = new MeterService(
                meterRepository,
                meterRead,
                meterRelation,
                null,          // encryptionService unused
                auditService
        );

        meterId = UUID.randomUUID();
        uuid1 = UUID.randomUUID();
        uuid2 = UUID.randomUUID();
    }

    private MeterRow row(UUID id) {
        return new MeterRow(
                id,
                meterId,
                "Water meter",
                null,
                "065E1GHB",
                0.0,
                0.0,
                null,
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(10).truncatedTo(ChronoUnit.DAYS),
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(5).truncatedTo(ChronoUnit.DAYS),
                null,
                null,
                null,
                true,
                99999,
                1.0,
                15,
                false
        );
    }

    private MeterRow createTestMeter(UUID lotId, boolean isMaster) {
        return new MeterRow(
                UUID.randomUUID(),
                meterId,
                "Water meter",
                null,
                "065E1GHB",
                0.0,
                0.0,
                null,
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(10).truncatedTo(ChronoUnit.DAYS),
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(5).truncatedTo(ChronoUnit.DAYS),
                null,
                null,
                null,
                true,
                99999,
                1.0,
                15,
                false
        );
    }

    @Test
    void create_shouldSaveAndAudit() {
        MeterCreateRequest req = new MeterCreateRequest(
                meterId, 1.0, 2.0, MeterType.WATER, MeterMeasurement.GAL, true, 99999,                 1.0,
                15,
                false
        );

        MeterRow saved = row(uuid1);
        when(meterRepository.save(any())).thenReturn(saved);

        Meters result = meterService.create(req);

        assertEquals(uuid1, result.uuid());
        verify(meterRepository).save(any());
        verify(auditService).recordInsert(eq("meters"), eq(uuid1), any());
    }

    @Test
    void findMetersById_shouldReturnMappedMeters() {
        MeterRow saved = row(uuid1);
        when(meterRepository.findById(uuid1)).thenReturn(Optional.of(saved));

        Optional<Meters> result = meterService.findMetersById(uuid1);

        assertTrue(result.isPresent());
        assertEquals(uuid1, result.get().uuid());
    }

    // Test create method with energy meter
    @Test
    void create_energyMeter_withKWH_succeeds() {
        MeterCreateRequest req = new MeterCreateRequest(
                UUID.randomUUID(),
                1.0,
                2.0,
                MeterType.ENERGY,
                MeterMeasurement.KWH,
                false,
                99999,
                1.0,
                15,
                false
        );

        MeterRow saved = MeterRow.forInsert(
                req.meterId(), req.pointX(), req.pointY(),
                req.utilityType(), req.measurement(), req.isMasterMeter(),
                req.rolloverMax(), req.meterMultiplier(), req.readDueDay(), req.isBimonthly()
        );

        when(meterRepository.save(any())).thenReturn(saved);

        Meters result = meterService.create(req);

        assertEquals(MeterMeasurement.KWH, result.measurement());
        verify(auditService).recordInsert(eq("meters"), any(), any());
    }

    @Test
    void create_energyMeter_withInvalidMeasurement_throws() {
        MeterCreateRequest req = new MeterCreateRequest(
                UUID.randomUUID(),
                1.0,
                2.0,
                MeterType.ENERGY,
                MeterMeasurement.GAL,
                false,
                99999,
                1.0,
                15,
                false
        );

        assertThrows(IllegalArgumentException.class, () -> meterService.create(req));
    }

    // Test create method with water meter
    @Test
    void create_waterMeter_withGallons_succeeds() {
        MeterCreateRequest req = new MeterCreateRequest(
                UUID.randomUUID(),
                1.0,
                2.0,
                MeterType.WATER,
                MeterMeasurement.GAL,
                false,
                99999,
                1.0,
                15,
                false
        );

        MeterRow saved = MeterRow.forInsert(
                req.meterId(), req.pointX(), req.pointY(),
                req.utilityType(), req.measurement(), req.isMasterMeter(),
                req.rolloverMax(), req.meterMultiplier(), req.readDueDay(), req.isBimonthly()
        );

        when(meterRepository.save(any())).thenReturn(saved);

        Meters result = meterService.create(req);

        assertEquals(MeterMeasurement.GAL, result.measurement());
        verify(auditService).recordInsert(eq("meters"), any(), any());
    }

    @Test
    void create_waterMeter_withKWH_throws() {
        MeterCreateRequest req = new MeterCreateRequest(
                UUID.randomUUID(),
                1.0,
                2.0,
                MeterType.WATER,
                MeterMeasurement.KWH,
                false,
                99999,
                1.0,
                15,
                false
        );

        assertThrows(IllegalArgumentException.class, () -> meterService.create(req));
    }

    // Test patch method with energy meter
    @Test
    void patch_energyMeter_toGallons_throws() {
        UUID id = UUID.randomUUID();

        MeterRow before = new MeterRow(
                id, UUID.randomUUID(), "Energy meter", "Should throw error", "123456789",
                1.0, 2.0, LocalDate.now(), OffsetDateTime.now(), null,
                null, MeterType.ENERGY, MeterMeasurement.KWH, false, 99999,                 1.0,
                15,
                false
        );


        when(meterRepository.findById(id)).thenReturn(Optional.of(before));

        Map<String, Object> changes = Map.of("measurement", "GAL");

        assertThrows(IllegalArgumentException.class, () -> meterService.patchMeter(id, changes));
    }

    // Test patch method with water meter
    @Test
    void patch_waterMeter_toGallons_succeeds() {
        UUID id = UUID.randomUUID();

        MeterRow before = new MeterRow(
                id, UUID.randomUUID(), "Energy meter", "Should patch successfully", "123456789",
                1.0, 2.0, LocalDate.now(), OffsetDateTime.now(), null,
                null, MeterType.WATER, MeterMeasurement.CBF, false, 99999,                 1.0,
                15,
                false
        );

        when(meterRepository.findById(id)).thenReturn(Optional.of(before));
        MeterRow after = new MeterRow(
                before.uuid(),
                before.meterId(),
                before.title(),
                before.description(),
                before.serialNumber(),
                before.pointX(),
                before.pointY(),
                before.installedAt(),
                before.createdAt(),
                OffsetDateTime.now(),
                null,
                MeterType.WATER,
                MeterMeasurement.GAL,   // patch CBF to GAL
                before.isMasterMeter(),
                99999,
                1.0,
                15,
                false
        );

        when(meterRepository.patch(eq(id), any())).thenReturn(Optional.of(after));

        Map<String, Object> changes = Map.of("measurement", "GAL");

        Optional<Meters> result = meterService.patchMeter(id, changes);

        assertTrue(result.isPresent());
        verify(auditService).recordUpdate(eq("meters"), eq(id), any(), any());
    }

    @Test
    void patch_waterMeter_toKWH_throws() {
        UUID id = UUID.randomUUID();

        MeterRow before = new MeterRow(
                id, UUID.randomUUID(), "Energy meter", "Should patch successfully", "123456789",
                1.0, 2.0, LocalDate.now(), OffsetDateTime.now(), null,
                null, MeterType.WATER, MeterMeasurement.GAL, false, 99999,                 1.0,
                15,
                false
        );

        when(meterRepository.findById(id)).thenReturn(Optional.of(before));

        Map<String, Object> changes = Map.of("measurement", "KWH");

        assertThrows(IllegalArgumentException.class, () -> meterService.patchMeter(id, changes));
    }

    // Check if audit successfully ignores redundant patches
    @Test
    void patch_noChanges_doesNotAudit() {
        UUID id = UUID.randomUUID();

        MeterRow before = new MeterRow(
                id, UUID.randomUUID(), "Water meter", "Should patch successfully", "123456789",
                1.0, 2.0, LocalDate.now(), OffsetDateTime.now(), null,
                null, MeterType.WATER, MeterMeasurement.GAL, false, 99999,                 1.0,
                15,
                false
        );

        when(meterRepository.findById(id)).thenReturn(Optional.of(before));

        Map<String, Object> changes = Map.of("title", "Water meter"); // Setting title "patch" to same val as create

        Optional<Meters> result = meterService.patchMeter(id, changes);

        assertTrue(result.isPresent());
        verify(auditService, never()).recordUpdate(any(), any(), any(), any());
    }

    // Meter Read Tests
    @Test
    void recordRead_incrementsRolloverCount_whenValueDecreases() {
        UUID id = UUID.randomUUID();
        MeterRow meter = createTestMeter(id, false);

        when(meterRepository.findById(meter.uuid())).thenReturn(Optional.of(meter));
        when(meterRead.save(any())).thenAnswer(inv -> {
            MeterReadRow row = inv.getArgument(0);
            return new MeterReadRow(
                    UUID.randomUUID(),
                    row.targetedMeter(),
                    row.meterAmount(),
                    row.readAt(),
                    row.isEstimated(),
                    row.rolloverCount(),
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    null
            );
        });

        meterService.recordRead(meter.uuid(), 99000, OffsetDateTime.now().minusDays(2), false);
        var rolledOver = meterService.recordRead(meter.uuid(), 500, OffsetDateTime.now(), false);

        assertEquals(1, rolledOver.rolloverCount());
    }

    @Test
    void recordRead_doesNotIncrementRollover_whenValueIncreasesNormally() {
        UUID id = UUID.randomUUID();
        MeterRow meter = createTestMeter(id, false);

        when(meterRepository.findById(meter.uuid())).thenReturn(Optional.of(meter));
        when(meterRead.save(any())).thenAnswer(inv -> {
            MeterReadRow row = inv.getArgument(0);
            return new MeterReadRow(
                    UUID.randomUUID(),
                    row.targetedMeter(),
                    row.meterAmount(),
                    row.readAt(),
                    row.isEstimated(),
                    row.rolloverCount(),
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    null
            );
        });

        meterService.recordRead(meter.uuid(), 100, OffsetDateTime.now().minusDays(2), false);
        var next = meterService.recordRead(meter.uuid(), 450, OffsetDateTime.now(), false);

        assertEquals(0, next.rolloverCount());
    }

    @Test
    void getUsageForPeriod_computesSimpleDelta_withNoRollover() {
        UUID id = UUID.randomUUID();
        MeterRow meter = createTestMeter(id, false);

        when(meterRepository.findById(meter.uuid())).thenReturn(Optional.of(meter));
        when(meterRead.save(any())).thenAnswer(inv -> {
            MeterReadRow row = inv.getArgument(0);
            return new MeterReadRow(
                    UUID.randomUUID(),
                    row.targetedMeter(),
                    row.meterAmount(),
                    row.readAt(),
                    row.isEstimated(),
                    row.rolloverCount(),
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    null
            );
        });

        OffsetDateTime start = OffsetDateTime.now().minusDays(30);
        OffsetDateTime end = OffsetDateTime.now();

        meterService.recordRead(meter.uuid(), 1000, start, false);
        meterService.recordRead(meter.uuid(), 1450, end, false);

        int usage = meterService.getUsageForPeriod(meter.uuid(), start, end);
        assertEquals(450, usage);
    }

    @Test
    void getUsageForPeriod_throws_whenNoReadingExistsBeforePeriodStart() {
        UUID id = UUID.randomUUID();
        MeterRow meter = createTestMeter(id, false);

        when(meterRepository.findById(meter.uuid())).thenReturn(Optional.of(meter));
        assertThrows(IllegalStateException.class, () ->
                meterService.getUsageForPeriod(meter.uuid(), OffsetDateTime.now().minusDays(30), OffsetDateTime.now()));
    }

    // Meter Relation Tests
    @Test
    void linkMeters_succeeds_whenParentIsMasterAndTypesMatch() {
        UUID id = UUID.randomUUID();
        MeterRow parent = createTestMeter(id, true);
        MeterRow child = createTestMeter(id, false);

        var relationship = meterService.linkMeters(parent.uuid(), child.uuid(), false, LocalDate.now());

        assertEquals(parent.uuid(), relationship.parentMeter());
        assertEquals(child.uuid(), relationship.childMeter());
    }

    @Test
    void linkMeters_throws_whenParentIsNotFlaggedAsMasterMeter() {
        UUID id = UUID.randomUUID();
        MeterRow notMaster = createTestMeter(id, false);
        MeterRow child = createTestMeter(id, false);

        assertThrows(IllegalArgumentException.class, () ->
                meterService.linkMeters(notMaster.uuid(), child.uuid(), false, LocalDate.now()));
    }

    @Test
    void linkMeters_throws_whenChildAlreadyHasActiveParent() {
        UUID id = UUID.randomUUID();
        MeterRow parentA = createTestMeter(id, true);
        MeterRow parentB = createTestMeter(id, true);
        MeterRow child = createTestMeter(id, false);

        meterService.linkMeters(parentA.uuid(), child.uuid(), false, LocalDate.now());

        assertThrows(IllegalStateException.class, () ->
                meterService.linkMeters(parentB.uuid(), child.uuid(), false, LocalDate.now()));
    }

    @Test
    void unlinkMeter_thenResolveParentMeter_returnsEmpty() {
        UUID id = UUID.randomUUID();
        MeterRow parent = createTestMeter(id, true);
        MeterRow child = createTestMeter(id, false);

        meterService.linkMeters(parent.uuid(), child.uuid(), false, LocalDate.now().minusDays(10));
        meterService.unlinkMeter(child.uuid(), LocalDate.now());

        assertTrue(meterService.resolveParentMeter(child.uuid(), LocalDate.now()).isEmpty());
    }

    @Test
    void resolveParentMeter_returnsCorrectParent_whenActiveRelationshipExists() {
        UUID id = UUID.randomUUID();
        MeterRow parent = createTestMeter(id, true);
        MeterRow child = createTestMeter(id, false);

        meterService.linkMeters(parent.uuid(), child.uuid(), false, LocalDate.now().minusDays(5));

        var resolved = meterService.resolveParentMeter(child.uuid(), LocalDate.now());
        assertTrue(resolved.isPresent());
        assertEquals(parent.uuid(), resolved.get());
    }
}