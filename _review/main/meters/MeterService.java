package io.github.lordship.meters;

import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.shared.EncryptionService;
import io.github.lordship.meters.internal.MeterCreateRequest;
import io.github.lordship.meters.internal.MeterRepository;
import io.github.lordship.meters.internal.MeterRow;
import io.github.lordship.tenancy.Tenancy;
import io.github.lordship.tenancy.internal.TenancyRow;
import io.github.lordship.tenants.internal.TenantRow;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

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
        if (request.utilityType() == MeterType.ENERGY &&
                request.measurement() != MeterMeasurement.KWH) {
            throw new IllegalArgumentException("ENERGY meters must use KWH as a measurement"); // energy meters must use KWH
        }

        if (request.utilityType() == MeterType.WATER &&
                request.measurement() == MeterMeasurement.KWH) {
            throw new IllegalArgumentException("WATER meters cannot use KWH as a measurement"); // water meters cannot use KWH
        }

        MeterRow row = meterRepository.save(
                MeterRow.forInsert(
                        request.meterId(),
                        request.pointX(),
                        request.pointY(),
                        request.utilityType(),
                        request.measurement(),
                        request.isMasterMeter()
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

    @Transactional
    public Optional<Meters> patchMeter(UUID uuid, Map<String, Object> changes) {
        Optional<MeterRow> beforeOpt = meterRepository.findById(uuid);
        if (beforeOpt.isEmpty()) {
            return Optional.empty();
        }
        MeterRow before = beforeOpt.get();

        Map<String, Object> mutable = new HashMap<>(changes);

        if (mutable.containsKey("title")) {
            Object raw = mutable.get("title");
            String newTitle = (raw == null) ? null : raw.toString();
                if (Objects.equals(before.title(), newTitle)) {
                    mutable.remove("title");
                } else {
                    mutable.put("title", newTitle);
                }
            }

        if (mutable.containsKey("description")) {
            Object raw = mutable.get("description");
            String newDescription = (raw == null) ? null : raw.toString();
            if (Objects.equals(before.description(), newDescription)) {
                mutable.remove("description");
            } else {
                mutable.put("description", newDescription);
            }

        }

        if (mutable.containsKey("serial_number")) {
            Object raw = mutable.get("serial_number");
            String newSerial = (raw == null) ? null : raw.toString();
            if (Objects.equals(before.serialNumber(), newSerial)) {
                mutable.remove("serial_number");
            } else {
                mutable.put("serial_number", newSerial);
            }
        }

        if (mutable.containsKey("installed_at")) {
            Object raw = mutable.get("installed_at");
            try {
                if (raw instanceof String s && !s.isBlank()) {
                    LocalDate parsed = LocalDate.parse(s);

                    if (Objects.equals(before.installedAt(), parsed)) {
                        mutable.remove("installed_at");
                    } else {
                        mutable.put("installed_at", parsed);
                    }

                } else {
                    if (before.installedAt() == null) {
                        mutable.remove("installed_at");
                    } else {
                        mutable.put("installed_at", null);
                    }
                }
            } catch(DateTimeParseException e){
                throw new IllegalArgumentException("Invalid date"); // Throws error if updated date is not valid
                }
        }

        if (mutable.containsKey("measurement")) {
            Object raw = mutable.get("measurement");

            if (raw == null) {
                throw new IllegalArgumentException("measurement cannot be null");
            }

            String value = raw.toString().trim();

            MeterMeasurement mm;
            try {
                mm = MeterMeasurement.valueOf(value);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "Invalid measurement: " + value +
                                ". Allowed: GAL, KWH, CBF"
                );
            }

            // ensures that the proper measurements are used for the meter
            if (before.utilityType() == MeterType.ENERGY &&
                    mm != MeterMeasurement.KWH) {
                throw new IllegalArgumentException("ENERGY meters must use KWH as a measurement");
            }

            if (before.utilityType() == MeterType.WATER &&
                    mm == MeterMeasurement.KWH) {
                throw new IllegalArgumentException("WATER meters cannot use KWH as a measurement");
            }

            if (Objects.equals(before.measurement(), mm)) {
                mutable.remove("measurement");
            } else {
                mutable.put("measurement", mm.name());
            }
        }

        if(mutable.isEmpty()) {
            return Optional.of(before.toMeters());
        }

        Optional<MeterRow> updatedMeter = meterRepository.patch(uuid, mutable);
        if (updatedMeter.isEmpty()) {
            return Optional.empty();
        }
        MeterRow after = updatedMeter.get();

        var diff = AuditMapper.diff(before, after);
        if(!diff.before().isEmpty()) {
            auditService.recordUpdate("meters", uuid, diff.before(), diff.after());
        }

        return Optional.of(after.toMeters());
    }

    @Transactional
    public boolean softDelete(UUID uuid) {
        return meterRepository.findById(uuid).map(meterRow -> {
            if (!meterRepository.softDelete(uuid)) {
                return false;
            }
            auditService.recordDelete("meters", uuid, AuditMapper.toMap(meterRow));
            return true;
        }).orElse(false);
    }
}
