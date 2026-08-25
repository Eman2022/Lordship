package io.github.lordship.meters.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MeterRelationRepository {
    private final JdbcClient jdbc;

    public MeterRelationRepository(JdbcClient jdbcClient) {
        this.jdbc = jdbcClient;
    }

    public MeterRelationRow save(MeterRelationRow row) {
        return jdbc.sql("""
                        INSERT INTO meter_relationship (
                            parent_meter, child_meter, effective_from
                        ) VALUES (
                            :parentMeter, :childMeter, :effectiveFrom
                        ) RETURNING *
                        """)
                .paramSource(row)
                .query(MeterRelationRow.class)
                .single();
    }

    public Optional<MeterRelationRow> findActiveByChild(UUID childMeter, LocalDate asOfDate) {
        return jdbc.sql("""
                        SELECT * FROM meter_relationship
                        WHERE child_meter = :childMeter
                          AND effective_from <= :asOfDate
                          AND (effective_to IS NULL OR effective_to > :asOfDate)
                        """)
                .param("childMeter", childMeter)
                .param("asOfDate", asOfDate)
                .query(MeterRelationRow.class)
                .optional();
    }

    public MeterRelationRow close(UUID uuid, LocalDate effectiveTo) {
        return jdbc.sql("""
                        UPDATE meter_relationship SET effective_to = :effectiveTo
                        WHERE uuid = :uuid RETURNING *
                        """)
                .param("uuid", uuid)
                .param("effectiveTo", effectiveTo)
                .query(MeterRelationRow.class)
                .single();
    }
}
