package io.github.lordship.lots.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class LotRepository {

    // Whitelisted so the dynamic patch UPDATE can never touch an arbitrary column.
    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "lot_number", "description", "notes", "sort_order"
    );

    private final JdbcClient jdbc;

    public LotRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public LotRow save(LotRow row) {
        return jdbc.sql("""
            INSERT INTO lot (
                property_id, lot_number,
                description, notes, sort_order
            ) VALUES (
                :propertyId, :lotNumber,
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

    // Partial update: only the columns present in `changes` are written. Trades
    // compile-time type safety for generality, so callers must pass already-coerced
    // values (e.g. Integer sort_order). Column names are whitelisted.
    public Optional<LotRow> patch(UUID uuid, Map<String, Object> changes) {
        if (changes.isEmpty()) return findById(uuid);

        for (String col : changes.keySet()) {
            if (!ALLOWED_COLUMNS.contains(col)) {
                throw new IllegalArgumentException("Invalid column: " + col);
            }
        }

        StringBuilder sql = new StringBuilder("UPDATE lot SET ");
        changes.forEach((col, val) -> sql.append(col).append(" = :").append(col).append(", "));
        // trim trailing comma and space
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE uuid = :uuid AND deleted_at IS NULL RETURNING *");

        Map<String, Object> params = new HashMap<>(changes);
        params.put("uuid", uuid);

        return jdbc.sql(sql.toString())
                .params(params)
                .query(LotRow.class)
                .optional();
    }

    public List<LotRow> findByProperty(String propertyCode) {
        return jdbc.sql("""
            SELECT l.* FROM lot l
            JOIN property p ON p.uuid = l.property_id
            WHERE p.property_code = :propertyCode
              AND p.deleted_at IS NULL
              AND l.deleted_at IS NULL
            ORDER BY sort_order NULLS LAST, lot_number
            """)
                .param("propertyCode", propertyCode)
                .query(LotRow.class)
                .list();
    }

    // Surfaces lots that share a number within a property (e.g. duplicate "DF"
    // labels seen in real park data). The application flags these rather than
    // the DB rejecting them.
    public List<LotRow> findDuplicateNumbers(String propertyCode) {
        return jdbc.sql("""
            SELECT l.* FROM lot l
            JOIN property p ON p.uuid = l.property_id
            WHERE l.deleted_at IS NULL
              AND p.property_code = :propertyCode
              AND LOWER(lot_number) IN (
                  SELECT LOWER(lot_number) FROM lot
                  WHERE property_id = p.uuid AND deleted_at IS NULL
                  GROUP BY LOWER(lot_number) HAVING COUNT(*) > 1
              )
            ORDER BY LOWER(lot_number)
            """)
                .param("propertyCode", propertyCode)
                .query(LotRow.class)
                .list();
    }

    public void softDelete(UUID uuid) {
        jdbc.sql("UPDATE lot SET deleted_at = CURRENT_TIMESTAMP WHERE uuid = :uuid AND deleted_at IS NULL")
                .param("uuid", uuid)
                .update();
    }
}
