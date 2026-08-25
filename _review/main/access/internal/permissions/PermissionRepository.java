package io.github.lordship.access.internal.permissions;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class PermissionRepository {

    private final JdbcClient jdbc;

    public PermissionRepository(JdbcClient jdbcClient) {
        this.jdbc = jdbcClient;
    }

    public Optional<PermissionRow> findById(UUID id) {
        return jdbc.sql("""
                SELECT * FROM permission WHERE uuid = :uuid AND deleted_at IS NULL
                """)
                .param("uuid", id)
                .query(PermissionRow.class)
                .optional();
    }

    public Optional<PermissionRow> findByName(String permissionName) {
        return jdbc.sql("""
                SELECT * FROM permission
                WHERE permission_name = :permissionName
                """)
                .param("permissionName", permissionName)
                .query(PermissionRow.class)
                .optional();
    }

    public Set<PermissionRow> getAllPermissions() {
        return new HashSet<>(jdbc.sql("""
                SELECT * from permission 
                """)   // note: permissions can't be deleted
                .query(PermissionRow.class)
                .list());
    }

    public Set<PermissionRow> findActivePermissionsForAgent(UUID agentId) {
        return new HashSet<>(jdbc.sql("""
                SELECT DISTINCT p.*
                FROM permission p
                JOIN role_permission rp ON rp.permission_id = p.uuid
                JOIN granted_role gr ON gr.role_id = rp.role_id
                WHERE gr.agent_id = :agentId
                 AND gr.deleted_at IS NULL
                 AND rp.deleted_at IS NULL
                 AND p.uuid NOT IN (
                 SELECT permission_id
                 FROM denied_permission
                 WHERE agent_id = :agentId
                 AND deleted_at IS NULL
                )
                """).param("agentId", agentId)
                .query(PermissionRow.class)
                .list());
    }

}
