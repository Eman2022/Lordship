package io.github.lordship.meters.internal;

import io.github.lordship.meters.internal.MeterRow;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class MeterRepository {

    private final JdbcClient jdbc;

    // Decide which data points should be modified
    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "title",
            "description",
            "serial_number",
            "installed_at",
            "measurement"
    );

    public MeterRepository(JdbcClient jdbcClient) {
        this.jdbc = jdbcClient;
    }

    public MeterRow save(MeterRow row) {
        return jdbc.sql("""
                    INSERT INTO meters (
                            meter_id, title, description, serial_number, point_x, point_y, utility_type, measurement, is_master_meter, rollover_max, meter_multiplier, read_due_day, is_bimonthly
                        ) VALUES (
                            :meterId, :title, :description, :serialNumber, :pointX, :pointY, :utilityType::meter_type, :measurement::meter_measurement, :isMasterMeter, :rolloverMax, :meterMultiplier, :readDueDay, :isBiMonthly
                        ) RETURNING *
                    """)
                .param("meterId", row.meterId())
                .param("title", row.title())
                .param("description", row.description())
                .param("serialNumber", row.serialNumber())
                .param("pointX", row.pointX())
                .param("pointY", row.pointY())
                .param("utilityType", row.utilityType() != null ? row.utilityType().name() : null) // useful for binding enums
                .param("measurement", row.measurement() != null ? row.measurement().name() : null)
                .param("isMasterMeter", row.isMasterMeter())
                .param("rolloverMax", row.rolloverMax())
                .param("meterMultiplier", row.meterMultiplier())
                .param("readDueDay", row.readDueDay())
                .param("isBimonthly", row.isBimonthly())
                .query(MeterRow.class)
                .single();
    }

    public Optional<MeterRow> findById(UUID uuid) {
        return jdbc.sql("SELECT * FROM meters WHERE uuid = :uuid AND deleted_at IS NULL")
                .param("uuid", uuid)
                .query(MeterRow.class)
                .optional();
    }

    public List<MeterRow> findMeterByLot(UUID meterId) {
        return jdbc.sql("""
                       SELECT * FROM meters WHERE meter_id = :meterId
                       AND deleted_at IS NULL
                       """)
                .param("meterId", meterId)
                .query(MeterRow.class)
                .list();
    }

    public void softDelete(UUID uuid) {
        jdbc.sql("UPDATE meters SET deleted_at = CURRENT_TIMESTAMP WHERE uuid = :uuid")
                .param("uuid", uuid)
                .update();
    }

    public Optional<MeterRow> patch(UUID uuid, Map<String, Object> changes) {
        if (changes.isEmpty()) return findById(uuid);

        // Only allow specific columns to be patched
        for (String col : changes.keySet()) {
            if (!ALLOWED_COLUMNS.contains(col)) {
                throw new IllegalArgumentException("Invalid column: " + col);
            }
        }

        StringBuilder sql = new StringBuilder("UPDATE meters SET ");

        changes.forEach((col, val) -> sql.append(col).append(" = :").append(col).append(", "));

        // Remove trailing comma
        sql.setLength(sql.length() - 2);

        sql.append(" WHERE uuid = :uuid AND deleted_at IS NULL RETURNING *");

        Map<String, Object> params = new HashMap<>(changes);
        params.put("uuid", uuid);

        return jdbc.sql(sql.toString())
                .paramSource(params)
                .query(MeterRow.class)
                .optional();
    }
}
