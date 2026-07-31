package io.github.lordship.properties.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class PropertyRepository {
    private final JdbcClient jdbc;

    public PropertyRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            // fill in with the property table's patchable columns
            "property_name", "property_address", "property_code", "year_built"
    );

    public PropertyRow save(PropertyRow row) {
        return jdbc.sql("""
                        INSERT INTO property (
                            property_code, property_name, property_address,
                            property_city, property_state, purchase_date, year_built
                        ) VALUES (
                            :propertyCode, :propertyName, :propertyAddress,
                            :propertyCity, :propertyState, :purchaseDate, :yearBuilt
                        ) RETURNING *
                        """)
                .paramSource(row)
                .query(PropertyRow.class)
                .single();
    }

    public Optional<PropertyRow> getPropertyOptional(String propertyCode) {
        return jdbc.sql("SELECT * FROM property WHERE property_code = :propertyCode AND deleted_at IS NULL")
                .param("propertyCode", propertyCode)
                .query(PropertyRow.class)
                .optional();
    }

    public Optional<PropertyRow> getPropertyOptional(UUID propertyId) {
        return jdbc.sql("SELECT * FROM property WHERE uuid = :propertyId AND deleted_at IS NULL")
                .param("propertyId", propertyId)
                .query(PropertyRow.class)
                .optional();
    }

    public List<PropertyRow> findAll() {
        return jdbc.sql("SELECT * FROM property WHERE deleted_at IS NULL")
                .query(PropertyRow.class)
                .list();
    }


    public Optional<PropertyRow> patch(UUID uuid, Map<String, Object> changes) {
        if (changes.isEmpty()) return getPropertyOptional(uuid);

        for (String col : changes.keySet()) {
            if (!ALLOWED_COLUMNS.contains(col)) {
                throw new IllegalArgumentException("Invalid column: " + col);
            }
        }

        String setClauses = changes.keySet().stream()
                .map(col -> col + " = :" + col)
                .collect(Collectors.joining(", "));

        String sql = "UPDATE property SET " + setClauses +
                " WHERE uuid = :uuid AND deleted_at IS NULL RETURNING *";

        Map<String, Object> params = new HashMap<>(changes);
        params.put("uuid", uuid);

        return jdbc.sql(sql)
                .params(params)
                .query(PropertyRow.class)
                .optional();
    }


    public boolean softDelete(UUID uuid) {
        int rows = jdbc.sql("UPDATE property SET deleted_at = NOW() WHERE uuid = :uuid AND deleted_at IS NULL")
                .param("uuid", uuid)
                .update();
        return rows > 0;
    }

}
