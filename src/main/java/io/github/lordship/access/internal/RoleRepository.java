package io.github.lordship.access.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class RoleRepository {

    private static final Set<String> ALLOWED_COLUMNS = Set.of("role_name", "role_description");

    private final JdbcClient jdbc;


    public RoleRepository(JdbcClient jdbc) { this.jdbc = jdbc;}

    public Optional<RoleRow> findByName(String roleName) {
        return jdbc.sql("""
                SELECT * FROM agent_role
                WHERE role_name = :roleName
                and deleted_at IS NULL
                """)
                .param("roleName", roleName)
                .query(RoleRow.class)
                .optional();
    }

    public Optional<RoleRow> findById(UUID uuid) {
        return jdbc.sql("""
                SELECT * FROM agent_role
                WHERE uuid = :uuid
                AND deleted_at IS NULL
                """)
                .param("uuid", uuid)
                .query(RoleRow.class)
                .optional();
    }

    public RoleRow save(RoleRow row) {
        return jdbc.sql("""
                INSERT INTO agent_role (role_name, role_description)
                VALUES (:roleName, :roleDescription)
                RETURNING *
                """)
                .paramSource(row)
                .query(RoleRow.class)
                .single();
    }

    public Optional<RoleRow> patch(UUID uuid, Map<String, Object> changes) {
        if (changes.isEmpty()) return findById(uuid);

        for (String col : changes.keySet()) {
            if (!ALLOWED_COLUMNS.contains(col)){
                throw new IllegalArgumentException("Invalid column: " + col);
            }
        }

        StringBuilder sql = new StringBuilder("UPDATE agent_role SET ");
        changes.forEach((col, val) -> sql.append(col).append("= :").append(col).append(", "));

        sql.setLength(sql.length() - 2); // trim trailing comma and space
        sql.append(" WHERE uuid = :uuid AND deleted_at IS NULL RETURNING *");

        Map<String, Object> params = new HashMap<>(changes);
        params.put("uuid", uuid);

        return jdbc.sql(sql.toString())
                .params(params)
                .query(RoleRow.class)
                .optional();
    }

    public void softDelete(UUID uuid) {
        jdbc.sql("""
                UPDATE agent_role
                SET deleted_at = now()
                WHERE uuid = :uuid AND deleted_at IS NULL
        """)
        .param("uuid", uuid)
        .update();
    }
}
