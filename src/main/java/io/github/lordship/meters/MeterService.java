package io.github.lordship.meters;

import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.shared.EncryptionService;
import io.github.lordship.meters.internal.MeterCreateRequest;
import io.github.lordship.meters.internal.MeterRepository;
import io.github.lordship.meters.internal.MeterRow;
import io.github.lordship.tenants.internal.TenantRow;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MeterService {
    private final MeterRepository meterRepository;
    private final EncryptionService encryptionService;
    private final AuditService auditService;

    private static final Logger log = LoggerFactory.getLogger(MeterService.class);

    public MeterService(
            MeterRepository meterRepository,
            EncryptionService encryptionService,
            AuditService auditService

    ) {
        this.meterRepository = meterRepository;
        this.encryptionService = encryptionService;
        this.auditService = auditService;
    }

    @Transactional
    public Meters create(MeterCreateRequest request) {
        MeterRow row = meterRepository.save(
                MeterRow.forInsert(
                        request.meterId(),
                        request.pointX(),
                        request.pointY(),
                        request.utilityType(),
                        request.measurement()
                )
        );

        auditService.recordInsert("meters", row.uuid(), AuditMapper.toMap(row));
        return row.toMeters();
    }

    public Optional<Meters> findMetersById(UUID uuid) {
        return meterRepository.findById(uuid).map(MeterRow::toMeters);
    }

    public List<Meters> findActiveMetersByLot(UUID lotId) {
        return meterRepository.findMeterByLot(lotId)
                .stream()
                .map(MeterRow::toMeters)
                .toList();
    }
}
