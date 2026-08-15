package io.github.lordship.standardterms.internal;

import io.github.lordship.shared.AgreementType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class StandardTermsRepository {

    // property and agreement_type not included - they should not be changed
    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "name", "target_rate",
            "car_fee", "allowed_cars", "pet_fee", "allowed_pets",
            "rent_due_day", "grace_period_days",
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

    public StandardTermsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // Creates a blank set -- every term column takes its DB default.
    public StandardTermsRow save(StandardTermsRow row) {
        return jdbc.sql("""
                INSERT INTO standard_terms (property, name, agreement_type)
                VALUES (:property, :name, :agreementType::agreement_type)
                RETURNING *
                """)
                .param("property", row.property())
                .param("name", row.name())
                .param("agreementType", nameOf(row.agreementType()))
                .query(StandardTermsRow.class)
                .single();
    }

    // copy a template into a property.
    public StandardTermsRow saveCopy(StandardTermsRow row) {
        return jdbc.sql("""
                INSERT INTO standard_terms (
                    property, name, agreement_type, target_rate,
                    car_fee, allowed_cars, pet_fee, allowed_pets,
                    rent_due_day, grace_period_days,
                    rule_violation_fee_method, rule_violation_fee_amount,
                    nsf_fee_method, nsf_fee_amount,
                    late_fee_method, late_fee_amount,
                    water_method, water_flat_amount,
                    power_method, power_flat_amount,
                    sewer_method, sewer_flat_amount,
                    trash_method, trash_flat_amount,
                    note
                ) VALUES (
                    :property, :name, :agreementType::agreement_type, :targetRate,
                    :carFee, :allowedCars, :petFee, :allowedPets,
                    :rentDueDay, :gracePeriodDays,
                    :ruleViolationFeeMethod, :ruleViolationFeeAmount,
                    :nsfFeeMethod, :nsfFeeAmount,
                    :lateFeeMethod, :lateFeeAmount,
                    :waterMethod, :waterFlatAmount,
                    :powerMethod, :powerFlatAmount,
                    :sewerMethod, :sewerFlatAmount,
                    :trashMethod, :trashFlatAmount,
                    :note
                ) RETURNING *
                """)
                .param("property", row.property())
                .param("name", row.name())
                .param("agreementType", nameOf(row.agreementType()))
                .param("targetRate", row.targetRate())
                .param("carFee", row.carFee())
                .param("allowedCars", row.allowedCars())
                .param("petFee", row.petFee())
                .param("allowedPets", row.allowedPets())
                .param("rentDueDay", row.rentDueDay())
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
                .param("note", row.note())
                .query(StandardTermsRow.class)
                .single();
    }

    public Optional<StandardTermsRow> findById(UUID uuid) {
        return jdbc.sql("SELECT * FROM standard_terms WHERE uuid = :uuid AND deleted_at IS NULL")
                .param("uuid", uuid)
                .query(StandardTermsRow.class)
                .optional();
    }

    // The deal types this property may offer, in enum declaration order.
    public List<StandardTermsRow> findByProperty(UUID property) {
        return jdbc.sql("""
                SELECT * FROM standard_terms
                WHERE property = :property AND deleted_at IS NULL
                ORDER BY agreement_type
                """)
                .param("property", property)
                .query(StandardTermsRow.class)
                .list();
    }

    // Admin-only templates: the pool a property copies from.
    public List<StandardTermsRow> findGlobalTemplates() {
        return jdbc.sql("""
                SELECT * FROM standard_terms
                WHERE property IS NULL AND deleted_at IS NULL
                ORDER BY agreement_type
                """)
                .query(StandardTermsRow.class)
                .list();
    }

    // Property-scoped only. Global templates may have several per agreement type,
// so they are looked up by name instead.
    public Optional<StandardTermsRow> findByPropertyAndAgreementType(UUID property, AgreementType agreementType) {
        return jdbc.sql("""
            SELECT * FROM standard_terms
            WHERE property = :property
              AND agreement_type = :agreementType::agreement_type
              AND deleted_at IS NULL
            """)
                .param("property", property)
                .param("agreementType", nameOf(agreementType))
                .query(StandardTermsRow.class)
                .optional();
    }

    public Optional<StandardTermsRow> findGlobalByName(String name) {
        return jdbc.sql("""
            SELECT * FROM standard_terms
            WHERE property IS NULL
              AND lower(name) = lower(:name)
              AND deleted_at IS NULL
            """)
                .param("name", name)
                .query(StandardTermsRow.class)
                .optional();
    }

    public Optional<StandardTermsRow> patch(UUID uuid, Map<String, Object> changes, UUID updatedBy) {
        if (changes.isEmpty()) return findById(uuid);

        for (String col : changes.keySet()) {
            if (!ALLOWED_COLUMNS.contains(col)) {
                throw new IllegalArgumentException("Invalid column: " + col);
            }
        }

        StringBuilder sql = new StringBuilder("UPDATE standard_terms SET ");
        changes.forEach((col, val) -> sql.append(col).append(" = :").append(col).append(", "));
        sql.append("updated_at = now(), updated_by = :updatedBy");
        sql.append(" WHERE uuid = :uuid AND deleted_at IS NULL RETURNING *");

        Map<String, Object> params = new HashMap<>(changes);
        params.put("uuid", uuid);
        params.put("updatedBy", updatedBy);

        return jdbc.sql(sql.toString())
                .params(params)
                .query(StandardTermsRow.class)
                .optional();
    }

    public boolean softDelete(UUID uuid) {
        return jdbc.sql("UPDATE standard_terms SET deleted_at = now() WHERE uuid = :uuid AND deleted_at IS NULL")
                .param("uuid", uuid)
                .update() > 0;
    }

    private static String nameOf(Enum<?> value) {
        return value == null ? null : value.name();
    }
}