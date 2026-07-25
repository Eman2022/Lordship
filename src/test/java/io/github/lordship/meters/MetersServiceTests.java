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
                null
        );
    }
}