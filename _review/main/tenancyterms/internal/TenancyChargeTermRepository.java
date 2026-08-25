package io.github.lordship.tenancyterms.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class TenancyChargeTermRepository {

    // tenancy, agreement_type, status, source, standard_terms and the cancel
    // columns are not patchable - they move through service methods, not PATCH.
    private static final Set<String> PATCHABLE_COLUMNS = Set.of(
            "valid_at", "rate",
            "car_fee", "allowed_cars", "cars_max",
            "pet_fee", "allowed_pets",
            "payment_due_day", "grace_period_days",
            "rule_violation_fee_method", "rule_violation_fee_amount",
            "nsf_fee_method", "nsf_fee_amount",
            "late_fee_method", "late_fee_amount",
            "water_method", "water_flat_amount",
            "power_method", "power_flat_amount",
            "sewer_method", "sewer_flat_amount",
            "trash_method", "trash_flat_amount",
            "note"
    );

    private final JdbcClient jdbc;
    private final TenancyChargeTermRowMapper rowMapper;

    public TenancyChargeTermRepository(JdbcClient jdbc, TenancyChargeTermRowMapper rowMapper) {
        this.jdbc = jdbc;
        this.rowMapper = rowMapper;
    }

    public TenancyChargeTermRow save(TenancyChargeTermRow row) {
        return jdbc.sql("""
                INSERT INTO tenancy_charge_term (
                    tenancy, valid_at, agreement_type, rate,
                    car_fee, allowed_cars, cars_max, pet_fee, allowed_pets,
                    payment_due_day, grace_period_days,
                    rule_violation_fee_method, rule_violation_fee_amount,
                    nsf_fee_method, nsf_fee_amount,
                    late_fee_method, late_fee_amount,
                    water_method, water_flat_amount,
                    power_method, power_flat_amount,
                    sewer_method, sewer_flat_amount,
                    trash_method, trash_flat_amount,
                    status, source, source_uuid, standard_terms, batch,
                    note, created_by
                ) VALUES (
                    :tenancy, :validAt, :agreementType::agreement_type, :rate,
                    :carFee, :allowedCars, :carsMax, :petFee, :allowedPets,
                    :paymentDueDay, :gracePeriodDays,
                    :ruleViolationFeeMethod, :ruleViolationFeeAmount,
                    :nsfFeeMethod, :nsfFeeAmount,
                    :lateFeeMethod, :lateFeeAmount,
                    :waterMethod, :waterFlatAmount,
                    :powerMethod, :powerFlatAmount,
                    :sewerMethod, :sewerFlatAmount,
                    :trashMethod, :trashFlatAmount,
                    :status, :source, :sourceUuid, :standardTerms, :batch,
                    :note, :createdBy
                ) RETURNING *
                """)
                .param("tenancy", row.tenancy())
                .param("validAt", row.validAt())
                .param("agreementType", nameOf(row.agreementType()))
                .param("rate", row.rate())
                .param("carFee", row.carFee())
                .param("allowedCars", row.allowedCars())
                .param("carsMax", row.carsMax())
                .param("petFee", row.petFee())
                .param("allowedPets", row.allowedPets())
                .param("paymentDueDay", row.paymentDueDay())
                .param("gracePeriodDays", row.gracePeriodDays())
                .param("ruleViolationFeeMethod", nameOf(row.ruleViolationFeeMethod()))
                .param("ruleViolationFeeAmount", row.ruleViolationFeeAmount())
                .param("nsfFeeMethod", nameOf(row.nsfFeeMethod()))
                .param("nsfFeeAmount", row.nsfFeeAmount())
                .param("lateFeeMethod", nameOf(row.lateFeeMethod()))
                .param("lateFeeAmount", row.lateFeeAmount())
                .param("waterMethod", nameOf(row.waterMethod()))
                .param("waterFlatAmount", row.waterFlatAmount())
                .param("powerMethod", nameOf(row.powerMethod()))
                .param("powerFlatAmount", row.powerFlatAmount())
                .param("sewerMethod", nameOf(row.sewerMethod()))
                .param("sewerFlatAmount", row.sewerFlatAmount())
                .param("trashMethod", nameOf(row.trashMethod()))
                .param("trashFlatAmount", row.trashFlatAmount())
                .param("status", nameOf(row.status()))
                .param("source", nameOf(row.source()))
                .param("sourceUuid", row.sourceUuid())
                .param("standardTerms", row.standardTerms())
                .param("batch", row.batch())
                .param("note", row.note())
                .param("createdBy", row.createdBy())
                .query(rowMapper)
                .single();
    }

    public Optional<TenancyChargeTermRow> findById(UUID uuid) {
        return jdbc.sql("SELECT * FROM tenancy_charge_term WHERE uuid = :uuid AND deleted_at IS NULL")
                .param("uuid", uuid)
                .query(rowMapper)
                .optional();
    }

    // Everything ever agreed for this tenancy, newest deal first.
    public List<TenancyChargeTermRow> findByTenancy(UUID tenancy) {
        return jdbc.sql("""
                SELECT * FROM tenancy_charge_term
                WHERE tenancy = :tenancy AND deleted_at IS NULL
                ORDER BY valid_at DESC, created_at DESC
                """)
                .param("tenancy", tenancy)
                .query(rowMapper)
                .list();
    }

    // The deal in force on a given day: the latest ACTIVE term that has taken effect.
    public Optional<TenancyChargeTermRow> findInForce(UUID tenancy, LocalDate on) {
        return jdbc.sql("""
                SELECT * FROM tenancy_charge_term
                WHERE tenancy = :tenancy
                  AND status = 'ACTIVE'
                  AND valid_at <= :on
                  AND deleted_at IS NULL
                ORDER BY valid_at DESC
                LIMIT 1
                """)
                .param("tenancy", tenancy)
                .param("on", on)
                .query(rowMapper)
                .optional();
    }

    // The work queue: terms still being written or out for signature.
    public List<TenancyChargeTermRow> findOpen(UUID tenancy) {
        return jdbc.sql("""
                SELECT * FROM tenancy_charge_term
                WHERE tenancy = :tenancy
                  AND status IN ('PROPOSED','PENDING')
                  AND deleted_at IS NULL
                ORDER BY created_at
                """)
                .param("tenancy", tenancy)
                .query(rowMapper)
                .list();
    }

    // Only a PROPOSED term is editable -- once paper is out, the values are frozen.
    public Optional<TenancyChargeTermRow> patch(UUID uuid, Map<String, Object> changes) {
        if (changes.isEmpty()) return findById(uuid);

        for (String col : changes.keySet()) {
            if (!PATCHABLE_COLUMNS.contains(col)) {
                throw new IllegalArgumentException("Invalid column: " + col);
            }
        }

        StringBuilder sql = new StringBuilder("UPDATE tenancy_charge_term SET ");
        changes.forEach((col, val) -> sql.append(col).append(" = :").append(col).append(", "));
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE uuid = :uuid AND status = 'PROPOSED' AND deleted_at IS NULL RETURNING *");

        Map<String, Object> params = new HashMap<>(changes);
        params.put("uuid", uuid);

        return jdbc.sql(sql.toString())
                .params(params)
                .query(rowMapper)
                .optional();
    }

    // Submitted: a document is out for signature or service.
    public boolean markPending(UUID uuid, UUID instrument) {
        return jdbc.sql("""
                UPDATE tenancy_charge_term
                SET status = 'PENDING', source_uuid = :instrument
                WHERE uuid = :uuid AND status = 'PROPOSED' AND deleted_at IS NULL
                """)
                .param("uuid", uuid)
                .param("instrument", instrument)
                .update() > 0;
    }

    // In force from valid_at until a later ACTIVE term supersedes it.
    public boolean markActive(UUID uuid) {
        return jdbc.sql("""
                UPDATE tenancy_charge_term
                SET status = 'ACTIVE'
                WHERE uuid = :uuid AND status = 'PENDING' AND deleted_at IS NULL
                """)
                .param("uuid", uuid)
                .update() > 0;
    }

    public boolean cancel(UUID uuid, UUID cancelledBy, String reason) {
        return jdbc.sql("""
                UPDATE tenancy_charge_term
                SET status = 'CANCELLED', cancelled_at = now(),
                    cancelled_by = :cancelledBy, cancel_reason = :reason
                WHERE uuid = :uuid AND status = 'ACTIVE' AND deleted_at IS NULL
                """)
                .param("uuid", uuid)
                .param("cancelledBy", cancelledBy)
                .param("reason", reason)
                .update() > 0;
    }

    // Only before a term has gone into force -- the DB constraint enforces this too.
    public boolean softDelete(UUID uuid) {
        return jdbc.sql("""
                UPDATE tenancy_charge_term SET deleted_at = now()
                WHERE uuid = :uuid AND status IN ('PROPOSED','PENDING') AND deleted_at IS NULL
                """)
                .param("uuid", uuid)
                .update() > 0;
    }

    private static String nameOf(Enum<?> value) {
        return value == null ? null : value.name();
    }
}