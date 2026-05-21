package io.github.lordship.access.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RoleRepository {

    private final JdbcClient jdbc;

    public RoleRepository(JdbcClient jdbc) { this.jdbc = jdbc;}

    public Optional<RoleRow> findByName(String roleName) {
        return jdbc.sql("""
                SELECT * FROM agent_role
                WHERE role_name = :permissionName
                and deleted_at IS NULL
                """)
                .param("permissionName", roleName)
                .query(RoleRow.class)
                .optional();
    }

    public RoleRow save(RoleRow row) {
        return jdbc.sql("""
                INSERT INTO agent_role (role_name)
                VALUES (:permissionName)
                RETURNING *
                """)
                .paramSource(row)
                .query(RoleRow.class)
                .single();
    }
}
