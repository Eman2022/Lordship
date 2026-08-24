package io.github.lordship.access.internal.rbac;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RolePermissionRepository {

    private final JdbcClient jdbc;

    public RolePermissionRepository(JdbcClient jdbcClient) {
        this.jdbc = jdbcClient;
    }

    public RolePermissionRow save(RolePermissionRow row) {
        return jdbc.sql("""
                INSERT INTO role_permission (role_id, permission_id)
                VALUES (:roleId, :permissionId)
                RETURNING *
                """)
                .paramSource(row)
                .query(RolePermissionRow.class)
                .single();
    }

    public Optional<RolePermissionRow> findById(UUID uuid) {
        return jdbc.sql("""
                SELECT * FROM role_permission
                WHERE uuid = :uuid
                """)
                .param("uuid", uuid)
                .query(RolePermissionRow.class)
                .optional();
    }

    public List<RolePermissionRow> findByRoleId(UUID roleId) {
        return jdbc.sql("""
                SELECT * FROM role_permission
                WHERE role_id = :roleId
                AND deleted_at IS NULL
                """)
                .param("roleId", roleId)
                .query(RolePermissionRow.class)
                .list();
    }

    public List<RolePermissionRow> findByPermissionId(UUID permissionId) {
        return jdbc.sql("""
                SELECT * FROM role_permission
                WHERE permission_id = :permissionId
                AND deleted_at IS NULL
                """)
                .param("permissionId", permissionId)
                .query(RolePermissionRow.class)
                .list();
    }

    public Optional<RolePermissionRow> findActiveGrant(UUID roleId, UUID permissionId) {
        return jdbc.sql("""
                SELECT * FROM role_permission
                WHERE role_id = :roleId
                AND permission_id = :permissionId
                AND deleted_at IS NULL
                """)
                .param("roleId", roleId)
                .param("permissionId", permissionId)
                .query(RolePermissionRow.class)
                .optional();
    }

    public boolean revoke(UUID uuid) {
        return jdbc.sql("""
                UPDATE role_permission
                SET deleted_at = now()
                WHERE uuid = :uuid
                AND deleted_at IS NULL
                """)
                .param("uuid", uuid)
                .update() > 0;
    }
}