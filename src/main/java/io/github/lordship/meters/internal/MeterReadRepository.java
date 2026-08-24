package io.github.lordship.meters.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MeterReadRepository {

    private final JdbcClient jdbc;

    public MeterReadRepository(JdbcClient jdbcClient) {
        this.jdbc = jdbcClient;
    }

    public MeterReadRow save(MeterReadRow row) {
        return jdbc.sql("""
                        INSERT INTO meter_reads (
                            targeted_meter, meter_amount, read_at, is_estimated, rollover_count
                        ) VALUES (
                            :targetedMeter, :meterAmount, :readAt, :isEstimated, :rolloverCount
                        ) RETURNING *
                        """)
                .paramSource(row)
                .query(MeterReadRow.class)
                .single();
    }

    // Most recent read at or before the given instant — the anchor for rollover detection
    // and for "usage since period start" calculations.
    public Optional<MeterReadRow> findLatestAtOrBefore(UUID meterId, OffsetDateTime at) {
        return jdbc.sql("""
                        SELECT * FROM meter_reads
                        WHERE targeted_meter = :meterId AND read_at <= :at
                        ORDER BY read_at DESC LIMIT 1
                        """)
                .param("meterId", meterId)
                .param("at", at)
                .query(MeterReadRow.class)
                .optional();
    }

    public List<MeterReadRow> findBetween(UUID meterId, OffsetDateTime start, OffsetDateTime end) {
        return jdbc.sql("""
                        SELECT * FROM meter_reads
                        WHERE targeted_meter = :meterId AND read_at BETWEEN :start AND :end
                        ORDER BY read_at ASC
                        """)
                .param("meterId", meterId)
                .param("start", start)
                .param("end", end)
                .query(MeterReadRow.class)
                .list();
    }
}