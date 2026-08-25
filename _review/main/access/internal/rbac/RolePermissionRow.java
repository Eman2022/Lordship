package io.github.lordship.access.internal.rbac;

import io.github.lordship.access.RolePermission;

import java.time.Instant;
import java.util.UUID;

public record RolePermissionRow(
        UUID uuid,
        UUID roleId,
        UUID permissionId,
        Instant createdAt,
        Instant deletedAt
) {

    public RolePermissionRow(UUID roleId, UUID permissionId) {
        this(null, roleId, permissionId, null, null);
    }

    public RolePermission toRolePermission() {
        return new RolePermission(uuid, roleId, permissionId, createdAt, deletedAt);
    }
}