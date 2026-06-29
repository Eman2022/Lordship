package io.github.lordship.properties.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PropertyRepository {
    private final JdbcClient jdbc;

    public PropertyRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

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

    public List<PropertyRow> findAll() {
        return jdbc.sql("SELECT * FROM property WHERE deleted_at IS NULL")
                .query(PropertyRow.class)
                .list();
    }

}
