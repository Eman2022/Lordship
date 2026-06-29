package io.github.lordship.tenancy.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class TenantRepository {

    private final JdbcClient jdbc;

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
}
