package io.github.lordship.vehicles.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class VehicleRepository {
    private final JdbcClient jdbc;

    public VehicleRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

//    private static final Set<String> ALLOWED_COLUMNS = Set.of(
//            // fill in with the property table's patchable columns
//            "", "", "", ""
//    );

    public VehicleRow save(VehicleRow row) {
        return jdbc.sql("""
                INSERT INTO vehicle (
                    tenancy_uuid, make, model, year,
                    plate_number, plate_state, color, notes
                ) VALUES (
                    :tenancyUuid, :make, :model, :year,
                    :plateNumber, :plateState, :color, :notes
                ) RETURNING *
                """)
                .paramSource(row)
                .query(VehicleRow.class)
                .single();
    }

    public Optional<VehicleRow> findById(UUID uuid) {
        return jdbc.sql("SELECT * FROM vehicle WHERE uuid = :uuid AND deleted_at IS NULL")
                .param("uuid", uuid)
                .query(VehicleRow.class)
                .optional();
    }

    public List<VehicleRow> findByTenancy(UUID tenancyUuid) {
        return jdbc.sql("SELECT * FROM vehicle WHERE tenancy_uuid = :tenancyUuid AND deleted_at IS NULL ORDER BY created_at ASC")
                .param("tenancyUuid", tenancyUuid)
                .query(VehicleRow.class)
                .list();
    }

    public List<VehicleRow> findByProperty(UUID propertyUuid) {
        return jdbc.sql("""
            SELECT DISTINCT v.*
            FROM vehicle v
            LEFT JOIN tenancy t ON v.tenancy_uuid = t.uuid
            LEFT JOIN lot l ON t.lot_id = l.uuid
            WHERE (l.property_id = :propertyUuid)
              AND v.deleted_at IS NULL
            ORDER BY v.created_at ASC
            """)
                .param("propertyUuid", propertyUuid)
                .query(VehicleRow.class)
                .list();
    }

    public int countByTenancy(UUID tenancyUuid) {
        return jdbc.sql("SELECT COUNT(*) FROM vehicle WHERE tenancy_uuid = :tenancyUuid AND deleted_at IS NULL")
                .param("tenancyUuid", tenancyUuid)
                .query(Integer.class)
                .single();
    }

    // Flag check: find any vehicle on a property with a matching plate that belongs to a different tenant
    public List<VehicleRow> findUnregisteredByPlate(String plateNumber, UUID tenancyUuid) {
        return jdbc.sql("""
        SELECT v.*
        FROM tenancy t
        JOIN lot l  ON t.lot_id = l.uuid
        JOIN lot l2 ON l2.property_id = l.property_id
        JOIN tenancy t2 ON t2.lot_id = l2.uuid
        JOIN vehicle v ON v.tenancy_uuid = t2.uuid
        WHERE t.uuid = :tenancyUuid
          AND v.plate_number = :plateNumber
          AND v.tenancy_uuid != :tenancyUuid
          AND v.deleted_at IS NULL
        """)
                .param("plateNumber", plateNumber)
                .param("tenancyUuid", tenancyUuid)
                .query(VehicleRow.class)
                .list();
    }

    public Optional<UUID> findPropertyUuidByTenancy(UUID tenancyUuid) {
        return jdbc.sql("""
        SELECT l.property_id
        FROM tenancy t
        JOIN lot l ON t.lot_id = l.uuid
        WHERE t.uuid = :tenancyUuid
        """)
                .param("tenancyUuid", tenancyUuid)
                .query(UUID.class)
                .optional();
    }

    public Optional<VehicleRow> patch(UUID uuid, Map<String, Object> changes) {
        if (changes.isEmpty()) return findById(uuid);

        String setClauses = changes.keySet().stream()
                .map(col -> col + " = :" + col)
                .collect(Collectors.joining(", "));

        String sql = "UPDATE vehicle SET " + setClauses +
                ", updated_at = NOW() WHERE uuid = :uuid AND deleted_at IS NULL RETURNING *";

        changes.put("uuid", uuid);

        return jdbc.sql(sql)
                .paramSource(new org.springframework.jdbc.core.namedparam.MapSqlParameterSource(changes))
                .query(VehicleRow.class)
                .optional();
    }

    public boolean softDelete(UUID uuid) {
        int rows = jdbc.sql("UPDATE vehicle SET deleted_at = NOW() WHERE uuid = :uuid AND deleted_at IS NULL")
                .param("uuid", uuid)
                .update();
        return rows > 0;
    }

    // Policy methods
    public Optional<VehiclePolicyRow> findPolicyByProperty(UUID propertyUuid) {
        return jdbc.sql("SELECT * FROM vehicle_policy WHERE property_uuid = :propertyUuid")
                .param("propertyUuid", propertyUuid)
                .query(VehiclePolicyRow.class)
                .optional();
    }
    public VehiclePolicyRow savePolicy(VehiclePolicyRow row) {
        return jdbc.sql("""
                INSERT INTO vehicle_policy (property_uuid, free_vehicle_limit, extra_vehicle_fee, notes)
                VALUES (:propertyUuid, :freeVehicleLimit, :extraVehicleFee, :notes)
                ON CONFLICT (property_uuid) DO UPDATE SET
                    free_vehicle_limit = EXCLUDED.free_vehicle_limit,
                    extra_vehicle_fee = EXCLUDED.extra_vehicle_fee,
                    notes = EXCLUDED.notes,
                    updated_at = NOW()
                RETURNING *
                """)
                .paramSource(row)
                .query(VehiclePolicyRow.class)
                .single();
    }
}
