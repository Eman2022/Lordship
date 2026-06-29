package io.github.lordship.lots.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class LotRepository {

    private final JdbcClient jdbc;

    public LotRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public LotRow save(LotRow row) {
        return jdbc.sql("""
            INSERT INTO lot (
                property_id, lot_number, lot_type_code,
                description, notes, sort_order
            ) VALUES (
                :propertyId, :lotNumber, :lotTypeCode,
                :description, :notes, :sortOrder
            ) RETURNING *
            """)
                .paramSource(row)
                .query(LotRow.class)
                .single();
    }

    public LotRow update(LotRow row) {
        return jdbc.sql("""
            UPDATE lot SET
                lot_number    = :lotNumber,
                lot_type_code = :lotTypeCode,
                description   = :description,
                notes         = :notes,
                sort_order    = :sortOrder
            WHERE uuid = :uuid AND deleted_at IS NULL
            RETURNING *
            """)
                .paramSource(row)
                .query(LotRow.class)
                .single();
    }

    public Optional<LotRow> findById(UUID uuid) {
        return jdbc.sql("SELECT * FROM lot WHERE uuid = :uuid AND deleted_at IS NULL")
                .param("uuid", uuid)
                .query(LotRow.class)
                .optional();
    }

    public List<LotRow> findByProperty(String propertyCode) {
        return jdbc.sql("""
            SELECT * FROM lot
            WHERE property_code = :propertyCode AND deleted_at IS NULL
            ORDER BY sort_order NULLS LAST, lot_number
            """)
                .param("propertyCode", propertyCode)
                .query(LotRow.class)
                .list();
    }

    // Surfaces lots that share a number within a property (e.g. duplicate "DF"
    // labels seen in real park data). The application flags these rather than
    // the DB rejecting them.
    public List<LotRow> findDuplicateNumbers(String propertyId) {
        return jdbc.sql("""
            SELECT * FROM lot
            WHERE deleted_at IS NULL
              AND property_id = :property_id
              AND LOWER(lot_number) IN (
                  SELECT LOWER(lot_number) FROM lot
                  WHERE property_id = :propertyId AND deleted_at IS NULL
                  GROUP BY LOWER(lot_number) HAVING COUNT(*) > 1
              )
            ORDER BY LOWER(lot_number)
            """)
                .param("propertyId", propertyId)
                .query(LotRow.class)
                .list();
    }

    public void softDelete(UUID uuid) {
        jdbc.sql("UPDATE lot SET deleted_at = CURRENT_TIMESTAMP WHERE uuid = :uuid AND deleted_at IS NULL")
                .param("uuid", uuid)
                .update();
    }
}