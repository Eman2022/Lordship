package io.github.lordship.tenancy.internal;

import io.github.lordship.tenancy.Tenancy;
import io.github.lordship.tenancy.internal.TenancyRow;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Consider adding public List func for findByAccount and findAllLots
@Repository
public class TenancyRepository {

    private final JdbcClient jdbc;

    public TenancyRepository(JdbcClient jdbcClient) {
        this.jdbc = jdbcClient;
    }

    public TenancyRow save(TenancyRow row) {
        return jdbc.sql("""
                INSERT INTO tenancy (
                        lot_id, start_date, end_date
                    ) VALUES (
                        :lotId, :startDate, :endDate
                    ) RETURNING *
                """)
                .paramSource(row)
                .query(TenancyRow.class)
                .single();
    }

    public Optional<TenancyRow> findById(UUID uuid) {
        return jdbc.sql("SELECT * FROM tenancy WHERE uuid = :uuid AND deleted_at IS NULL")
                .param("uuid", uuid)
                .query(TenancyRow.class)
                .optional();
    }

    // Some lots may have two tenancies at a time
    public List<TenancyRow> findActiveByLot(UUID lotId) {
        return jdbc.sql("""
                SELECT * from tenancy WHERE lot_id = :lotId
                AND end_date IS NULL AND deleted_at IS NULL
                """)
                .param("lotId", lotId)
                .query(TenancyRow.class)
                .list();
    }

    // Determines when a tenancyId closes
    public TenancyRow close(UUID uuid, LocalDate endDate) {
        return jdbc.sql("""
                UPDATE tenancy
                SET end_date = :endDate
                WHERE uuid = :uuid AND deleted_at IS NULL
                RETURNING *
                """)
                .param("uuid", uuid)
                .param("endDate", endDate)
                .query(TenancyRow.class)
                .single();
    }

    // Stores deleted tenancies instead of removing them
    public void softDelete(UUID uuid) {
        jdbc.sql("UPDATE tenancy SET deleted_at = CURRENT_TIMESTAMP WHERE uuid = :uuid")
                .param("uuid", uuid)
                .update();
    }

}
