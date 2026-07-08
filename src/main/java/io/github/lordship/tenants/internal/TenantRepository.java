package io.github.lordship.tenants.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;

@Repository
public class TenantRepository {

    private final JdbcClient jdbc;

    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "tenancy_id",
            "person_id",
            "start_date",
            "end_date"
    );

    public TenantRepository(JdbcClient jdbcClient) { this.jdbc = jdbcClient; }

    public TenantRow save(TenantRow row) {
        return jdbc.sql("""
                INSERT INTO tenant (
                        tenancy_id, person_id, start_date, end_date
                    ) VALUES (
                        :tenancyId, :personId, :startDate, :endDate
                    ) RETURNING *
                """)
                .paramSource(row)
                .query(TenantRow.class)
                .single();
    }

    public Optional<TenantRow> findById(UUID uuid) {
        return jdbc.sql("SELECT * FROM tenant WHERE uuid = :uuid AND deleted_at IS NULL")
                .param("uuid", uuid)
                .query(TenantRow.class)
                .optional();
    }

    public List<TenantRow> findByTenancy(UUID tenancyId) {
        return jdbc.sql("SELECT * FROM tenant WHERE tenancy_id = :tenancyId")
                .param("tenancyId", tenancyId)
                .query(TenantRow.class)
                .list();
    }

    // Determines when a tenancyId closes
    public TenantRow end(UUID uuid, LocalDate endDate) {
        return jdbc.sql("""
                UPDATE tenant
                SET end_date = :endDate
                WHERE uuid = :uuid AND deleted_at IS NULL
                RETURNING *
                """)
                .param("uuid", uuid)
                .param("endDate", endDate)
                .query(TenantRow.class)
                .single();
    }

    // Softdelete tenants
    public void softDelete(UUID uuid) {
        jdbc.sql("UPDATE tenant SET deleted_at = CURRENT_TIMESTAMP WHERE uuid = :uuid")
                .param("uuid", uuid)
                .update();
    }

    public Optional<TenantRow> patch(UUID uuid, Map<String, Object> changes) {
        if (changes.isEmpty()) return findById(uuid);

        // Only allow specific columns to be patched
        for (String col : changes.keySet()) {
            if (!ALLOWED_COLUMNS.contains(col)) {
                throw new IllegalArgumentException("Invalid column: " + col);
            }
        }

        StringBuilder sql = new StringBuilder("UPDATE tenant SET ");

        changes.forEach((col, val) -> sql.append(col).append(" = :").append(col).append(", "));

        sql.setLength(sql.length() - 2);

        sql.append(" WHERE uuid = :uuid AND deleted_at IS NULL RETURNING *");

        Map<String, Object> params = new HashMap<>(changes);
        params.put("uuid", uuid);

        return jdbc.sql(sql.toString())
                .params(params)
                .query(TenantRow.class)
                .optional();
    }
}
