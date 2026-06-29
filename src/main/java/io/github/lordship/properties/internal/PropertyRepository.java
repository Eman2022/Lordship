package io.github.lordship.properties.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;

@Repository
public class PropertyRepository {
    private final JdbcClient jdbc;

    public PropertyRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public PropertyRow save(PropertyRow row) {
        return jdbc.sql("""
                        Insert INTO property (
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

        String setClauses = changes.keySet().stream()
                .map(col -> col + " = :" + col)
                .collect(java.util.stream.Collectors.joining(", "));

        String sql = "UPDATE property SET " + setClauses +
                ", updated_at = NOW() WHERE uuid = :uuid AND deleted_at IS NULL RETURNING *";

        changes.put("uuid", uuid);

        return jdbc.sql(sql)
                .paramSource(new org.springframework.jdbc.core.namedparam.MapSqlParameterSource(changes))
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
