package meterbilling;

import io.github.lordship.audit.AuditService;
import io.github.lordship.meterbills.MeterBills;
import io.github.lordship.meterbills.MeterBillsService;
import io.github.lordship.meterbills.internal.MeterBillsCreateRequest;
import io.github.lordship.meterbills.internal.MeterBillsRepository;
import io.github.lordship.meterbills.internal.MeterBillsRow;
import io.github.lordship.meters.MeterMeasurement;
import io.github.lordship.meters.MeterService;
import io.github.lordship.meters.MeterType;
import io.github.lordship.meters.Meters;
import io.github.lordship.meters.internal.MeterCreateRequest;
import io.github.lordship.meters.internal.MeterRepository;
import io.github.lordship.meters.internal.MeterRow;
import io.micrometer.core.instrument.Meter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private AuditService auditService;
    private MeterBillsService meterBillsService;

    private UUID billedMeter;
    private UUID uuid1;
    private UUID uuid2;

    @BeforeEach
    void setup() {
        meterBillsRepository = mock(MeterBillsRepository.class);
        auditService = mock(AuditService.class);

        meterBillsService = new MeterBillsService(
                meterBillsRepository,
                null,          // encryptionService unused
                auditService
        );

        billedMeter = UUID.randomUUID();
        uuid1 = UUID.randomUUID();
        uuid2 = UUID.randomUUID();
    }

    private MeterBillsRow row(UUID id) {
        return new MeterBillsRow(
                id,
                billedMeter,
                100,
                0.02,
                MeterMeasurement.GAL,
                LocalDate.now(),
                null,
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(10).truncatedTo(ChronoUnit.DAYS),
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(5).truncatedTo(ChronoUnit.DAYS),
                null
        );
    }

    @Test
    void create_shouldSaveAndAudit() {
        MeterBillsCreateRequest req = new MeterBillsCreateRequest(
                billedMeter, 100, 0.02, MeterMeasurement.GAL, LocalDate.now(),  null
        );

        MeterBillsRow saved = row(uuid1);
        when(meterBillsRepository.save(any())).thenReturn(saved);

        MeterBills result = meterBillsService.create(req);

        assertEquals(uuid1, result.uuid());
        verify(meterBillsRepository).save(any());
        verify(auditService).recordInsert(eq("meter_billing"), eq(uuid1), any());
    }

    @Test
    void findMetersById_shouldReturnMappedMeters() {
        MeterBillsRow saved = row(uuid1);
        when(meterBillsRepository.findById(uuid1)).thenReturn(Optional.of(saved));

        Optional<MeterBills> result = meterBillsService.findMeterBillById(uuid1);

        assertTrue(result.isPresent());
        assertEquals(uuid1, result.get().uuid());
    }

    // Check if audit successfully ignores redundant patches
    @Test
    void patch_noChanges_doesNotAudit() {
        UUID id = UUID.randomUUID();

        MeterBillsRow before = new MeterBillsRow(
                id,
                billedMeter,
                100,
                0.02,
                MeterMeasurement.GAL,
                LocalDate.now(),
                null,
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(10).truncatedTo(ChronoUnit.DAYS),
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(5).truncatedTo(ChronoUnit.DAYS),
                null
        );

        when(meterBillsRepository.findById(id)).thenReturn(Optional.of(before));

        Map<String, Object> changes = Map.of("billed_amount", "100"); // Setting "patch" to same val as create

        Optional<MeterBills> result = meterBillsService.patchMeterBill(id, changes);

        assertTrue(result.isPresent());
        verify(auditService, never()).recordUpdate(any(), any(), any(), any());
    }
}