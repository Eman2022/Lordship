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
                    tenancy_uuid, property_uuid, make, model, year,
                    plate_number, plate_state, color, notes
                ) VALUES (
                    :tenancyUuid, :propertyUuid, :make, :model, :year,
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

//    public List<VehicleRow> findByPropertyCode(String propertyCode) {
//        return jdbc.sql("SELECT * FROM vehicle WHERE property_code = :propertyCode AND deleted_at IS NULL ORDER BY created_at ASC")
//                .param("propertyCode", propertyCode)
//                .query(VehicleRow.class)
//                .list();
//    }

    public List<VehicleRow> findByProperty(UUID propertyUuid) {
        return jdbc.sql("SELECT * FROM vehicle WHERE property_uuid = :propertyUuid AND deleted_at IS NULL ORDER BY created_at ASC")
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
    public List<VehicleRow> findUnregisteredByPlate(String plateNumber, UUID propertyUuid, UUID tenancyUuid) {
        return jdbc.sql("""
                SELECT * FROM vehicle
                WHERE property_uuid = :propertyUuid
                AND plate_number = :plateNumber
                AND tenancy_uuid != :tenancyUuid
                AND deleted_at IS NULL
                """)
                .param("propertyUuid", propertyUuid)
                .param("plateNumber", plateNumber)
                .param("tenancyUuid", tenancyUuid)
                .query(VehicleRow.class)
                .list();
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
