package io.github.lordship.meterbills.internal;

import io.github.lordship.meterbills.MeterBills;
import io.github.lordship.meters.internal.MeterRow;
import io.github.lordship.tenancy.internal.TenancyRow;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class MeterBillsRepository {

    private final JdbcClient jdbc;

    // Decide which data points should be modified
    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "billed_amount",
            "rate_amount",
            "period_start",
            "period_end"
    );

    public MeterBillsRepository(JdbcClient jdbcClient) {
        this.jdbc = jdbcClient;
    }

    public MeterBillsRow save(MeterBillsRow row) {
        return jdbc.sql("""
                        INSERT INTO meter_billing (
                                billed_meter, billed_amount, rate_amount, rate_unit, period_start, period_end
                            ) VALUES (
                                :billedMeter, :billedAmount, rateAmount, :rateUnit::meter_measurement, periodStart, periodEnd
                            ) RETURNING *
                        """)
                .param("billedMeter", row.billedMeter())
                .param("billedAmount", row.billedAmount())
                .param("rateAmount", row.rateAmount())
                .param("rateUnit", row.rateUnit() != null ? row.rateUnit().name() : null) // useful for binding enums
                .param("periodStart", row.periodStart())
                .param("periodEnd", row.periodEnd())
                .query(MeterBillsRow.class)
                .single();
    }

    public Optional<MeterBillsRow> findById(UUID uuid) {
        return jdbc.sql("SELECT * FROM meter_billing WHERE uuid = :uuid AND deleted_at IS NULL")
                .param("uuid", uuid)
                .query(MeterBillsRow.class)
                .optional();
    }

    public void softDelete(UUID uuid) {
        jdbc.sql("UPDATE meter_billing SET deleted_at = CURRENT_TIMESTAMP WHERE uuid = :uuid")
                .param("uuid", uuid)
                .update();
    }

    public Optional<MeterBillsRow> patch(UUID uuid, Map<String, Object> changes) {
        if (changes.isEmpty()) return findById(uuid);

        // Only allow specific columns to be patched
        for (String col : changes.keySet()) {
            if (!ALLOWED_COLUMNS.contains(col)) {
                throw new IllegalArgumentException("Invalid column: " + col);
            }
        }

        StringBuilder sql = new StringBuilder("UPDATE meter_billing SET ");

        changes.forEach((col, val) -> sql.append(col).append(" = :").append(col).append(", "));

        // Remove trailing comma
        sql.setLength(sql.length() - 2);

        sql.append(" WHERE uuid = :uuid AND deleted_at IS NULL RETURNING *");

        Map<String, Object> params = new HashMap<>(changes);
        params.put("uuid", uuid);

        return jdbc.sql(sql.toString())
                .paramSource(params)
                .query(MeterBillsRow.class)
                .optional();
    }
}
