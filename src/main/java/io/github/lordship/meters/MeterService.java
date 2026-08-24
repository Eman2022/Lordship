package io.github.lordship.meters;

import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.meters.internal.*;
import io.github.lordship.shared.EncryptionService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class MeterService {
    private final MeterRepository meterRepository;
    private final MeterReadRepository meterReadRepository;
    private final MeterRelationRepository meterRelationRepository;
    private final EncryptionService encryptionService;
    private final AuditService auditService;

    private static final Logger log = LoggerFactory.getLogger(MeterService.class);

    public MeterService(
            MeterRepository meterRepository,
            MeterReadRepository meterReadRepository,
            MeterRelationRepository meterRelationRepository,
            EncryptionService encryptionService,
            AuditService auditService

    ) {
        this.meterRepository = meterRepository;
        this.meterReadRepository = meterReadRepository;
        this.meterRelationRepository = meterRelationRepository;
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
                        request.isMasterMeter(),
                        request.rolloverMax(),
                        request.meterMultiplier(),
                        request.readDueDay(),
                        request.isBimonthly()
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


    // METER READS
    @Transactional
    public MeterRead recordRead(UUID meterId, int rawAmount, OffsetDateTime readAt, boolean isEstimated) {
        MeterRow meter = meterRepository.findById(meterId) // Must find actual meter
                .orElseThrow(() -> new EntityNotFoundException("Meter not found: " + meterId));

        Optional<MeterReadRow> previous = meterReadRepository.findLatestAtOrBefore(meterId, readAt);

        int rolloverCount = previous.map(MeterReadRow::rolloverCount).orElse(0);
        if (previous.isPresent() && rawAmount < previous.get().meterAmount()) { // If the newest meter reading is lower than the last; rollover occured
            log.info("Rollover detected for meter uuid={}: {} -> {}", meterId, previous.get().meterAmount(), rawAmount);
            rolloverCount++;
        }

        MeterReadRow saved = meterReadRepository.save(
                MeterReadRow.forInsert(meterId, rawAmount, readAt, isEstimated, rolloverCount)
        );

        auditService.recordInsert("meter_reads", saved.uuid(), AuditMapper.toMap(saved));
        return saved.toMeterRead();
    }

    // Consider timing for bills that are delayed
    public Usage getUsageForPeriod(UUID meterId, OffsetDateTime start, OffsetDateTime end) {
        MeterRow meter = meterRepository.findById(meterId)
                .orElseThrow(() -> new EntityNotFoundException("Meter not found: " + meterId));

        MeterReadRow startRead = meterReadRepository.findLatestAtOrBefore(meterId, start)
                .orElseThrow(() -> new IllegalStateException(
                        "No reading exists at or before period start for meter " + meterId));
        MeterReadRow endRead = meterReadRepository.findLatestAtOrBefore(meterId, end)
                .orElseThrow(() -> new IllegalStateException(
                        "No reading exists at or before period end for meter " + meterId));

        if (startRead.uuid().equals(endRead.uuid())) {
            throw new IllegalStateException(
                    "No reading was taken within the requested period for meter " + meterId +
                            " — cannot compute usage without at least one read inside [" + start + ", " + end + "].");
        }

        int rawUsage = adjustedValue(endRead, meter.rolloverMax()) - adjustedValue(startRead, meter.rolloverMax());

        if (rawUsage < 0) {
            throw new IllegalStateException(
                    "Computed negative usage for meter " + meterId +
                            " — check rollover_max is set correctly, or investigate a possible meter replacement.");
        }

        double scaledUsage = applyMultiplier(rawUsage, meter.meterMultiplier());

        return new Usage(meterId, startRead.toMeterRead(), endRead.toMeterRead(), scaledUsage);
    }

    public Usage getUsageForCurrentPeriod(UUID meterId) {
        MeterRow meter = meterRepository.findById(meterId)
                .orElseThrow(() -> new EntityNotFoundException("Meter not found: " + meterId));

        LocalDate today = LocalDate.now();
        LocalDate periodEndDate = resolvePeriodEnd(today, meter.readDueDay());
        LocalDate periodStartDate = Boolean.TRUE.equals(meter.isBimonthly())
                ? periodEndDate.minusMonths(2)
                : periodEndDate.minusMonths(1);

        OffsetDateTime start = periodStartDate.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        OffsetDateTime end = periodEndDate.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);

        return getUsageForPeriod(meterId, start, end);
    }

    private LocalDate resolvePeriodEnd(LocalDate today, Integer readDueDay) {
        if (readDueDay == null) {
            return today; // no fixed billing cycle configured — fall back to "as of today"
        }
        int dueDay = Math.min(readDueDay, today.lengthOfMonth());
        LocalDate thisMonthDue = today.withDayOfMonth(dueDay);
        return today.isBefore(thisMonthDue) ? thisMonthDue.minusMonths(1) : thisMonthDue;
    }


    private int adjustedValue(MeterReadRow read, Integer rolloverMax) {
        if (read.rolloverCount() == 0 || rolloverMax == null) {
            return read.meterAmount();
        }
        return read.meterAmount() + ((int) read.rolloverCount() * rolloverMax);
    }

    private double applyMultiplier(int rawUsage, Double meterMultiplier) {
        if (meterMultiplier == null || meterMultiplier == 0) {
            return rawUsage;
        }
        return rawUsage * meterMultiplier;
    }


    // METER RELATIONS
    @Transactional
    public MeterRelation linkMeters(UUID parentMeterId, UUID childMeterId, Boolean hasUnmetered, LocalDate effectiveFrom) {
        MeterRow parent = meterRepository.findById(parentMeterId)
                .orElseThrow(() -> new EntityNotFoundException("Parent meter not found: " + parentMeterId));
        MeterRow child = meterRepository.findById(childMeterId)
                .orElseThrow(() -> new EntityNotFoundException("Child meter not found: " + childMeterId));

        if (!Boolean.TRUE.equals(parent.isMasterMeter())) {
            throw new IllegalArgumentException("Parent meter must be flagged is_master_meter=true");
        }
        if (parent.utilityType() != child.utilityType()) {
            throw new IllegalArgumentException( // Obviously a child meter cannot be of different type than its parent
                    "Cannot link meters of different utility types: " + parent.utilityType() + " / " + child.utilityType());
        }
        if (meterRelationRepository.findActiveByChild(childMeterId, effectiveFrom).isPresent()) {
            throw new IllegalStateException("Meter " + childMeterId + " already has an active parent as of " + effectiveFrom);
        }

        MeterRelationRow saved = meterRelationRepository.save(
                MeterRelationRow.forInsert(parentMeterId, childMeterId, hasUnmetered, effectiveFrom)
        );

        auditService.recordInsert("meter_relationship", saved.uuid(), AuditMapper.toMap(saved));
        return saved.toMeterRelation();
    }


    @Transactional
    public MeterRelation unlinkMeter(UUID childMeterId, LocalDate effectiveTo) {
        MeterRelationRow active = meterRelationRepository.findActiveByChild(childMeterId, effectiveTo)
                .orElseThrow(() -> new EntityNotFoundException("No active relationship for meter " + childMeterId));

        MeterRelationRow closed = meterRelationRepository.close(active.uuid(), effectiveTo);

        auditService.recordUpdate("meter_relationship", closed.uuid(),
                Map.of("effective_to", "null"), Map.of("effective_to", effectiveTo.toString()));
        return closed.toMeterRelation();
    }


    public Optional<UUID> resolveParentMeter(UUID childMeterId, LocalDate asOfDate) {
        return meterRelationRepository.findActiveByChild(childMeterId, asOfDate)
                .map(MeterRelationRow::parentMeter);
    }
}
