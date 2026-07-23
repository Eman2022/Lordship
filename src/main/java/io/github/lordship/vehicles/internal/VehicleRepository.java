package io.github.lordship.vehicles.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class VehicleRepository {
    private final JdbcClient jdbc;

    public VehicleRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public VehicleRow save(VehicleRow row) {
        return jdbc.sql("""
                INSERT INTO vehicle (
                    person_uuid, property_code, make, model, year,
                    plate_number, plate_state, color, notes
                ) VALUES (
                    :personUuid, :propertyCode, :make, :model, :year,
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
        return jdbc.sql("SELECT * FROM vehicle WHERE person_uuid = :personUuid AND deleted_at IS NULL ORDER BY created_at ASC")
                .param("tenancyUuid", tenancyUuid)
                .query(VehicleRow.class)
                .list();
    }

    public List<VehicleRow> findByPropertyCode(String propertyCode) {
        return jdbc.sql("SELECT * FROM vehicle WHERE property_code = :propertyCode AND deleted_at IS NULL ORDER BY created_at ASC")
                .param("propertyCode", propertyCode)
                .query(VehicleRow.class)
                .list();
    }

    public List<VehicleRow> findByProperty(UUID propertyId) {
        return jdbc.sql("SELECT * FROM vehicle WHERE property_id = :propertyId AND deleted_at IS NULL ORDER BY created_at ASC")
                .param("propertyId", propertyId)
                .query(VehicleRow.class)
                .list();
    }

    public int countByTenancy(UUID personUuid) {
        return jdbc.sql("SELECT COUNT(*) FROM vehicle WHERE tenancy_uuid = :personUuid AND deleted_at IS NULL")
                .param("personUuid", personUuid)
                .query(Integer.class)
                .single();
    }

    // Flag check: find any vehicle on a property with a matching plate that belongs to a different tenant
    public List<VehicleRow> findUnregisteredByPlate(String plateNumber, UUID propertyCode, UUID personUuid) {
        return jdbc.sql("""
                SELECT * FROM vehicle
                WHERE property_code = :propertyCode
                AND plate_number = :plateNumber
                AND person_uuid != :personUuid
                AND deleted_at IS NULL
                """)
                .param("propertyCode", propertyCode)
                .param("plateNumber", plateNumber)
                .param("personUuid", personUuid)
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
    public Optional<VehiclePolicyRow> findPolicyByProperty(UUID propertyId) {
        return jdbc.sql("SELECT * FROM vehicle_policy WHERE property_id = :propertyId")
                .param("propertyId", propertyId)
                .query(VehiclePolicyRow.class)
                .optional();
    }
    public VehiclePolicyRow savePolicy(VehiclePolicyRow row) {
        return jdbc.sql("""
                INSERT INTO vehicle_policy (property_code, free_vehicle_limit, extra_vehicle_fee, notes)
                VALUES (:propertyCode, :freeVehicleLimit, :extraVehicleFee, :notes)
                ON CONFLICT (property_code) DO UPDATE SET
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
