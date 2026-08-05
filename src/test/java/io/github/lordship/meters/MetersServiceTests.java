package io.github.lordship.meters;

import io.github.lordship.audit.AuditService;
import io.github.lordship.meters.Meters;
import io.github.lordship.meters.internal.MeterCreateRequest;
import io.github.lordship.meters.internal.MeterRepository;
import io.github.lordship.meters.internal.MeterRow;
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

    private UUID meterId;
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

        meterId = UUID.randomUUID();
        uuid1 = UUID.randomUUID();
        uuid2 = UUID.randomUUID();
    }

    private MeterRow row(UUID id) {
        return new MeterRow(
                id,
                meterId,
                null,
                null,
                null,
                0.0,
                0.0,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
/*
    @Test
    void create_shouldSaveAndAudit() {
        MeterCreateRequest req = new MeterCreateRequest(
                meterId, 1.0, 2.0, MeterType.WATER, MeterMeasurement.GALLONS, true
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
    } */
}