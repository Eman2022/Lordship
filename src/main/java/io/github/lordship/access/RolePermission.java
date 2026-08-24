package io.github.lordship.access;

import io.github.lordship.access.internal.rbac.RolePermissionResponse;

import java.time.Instant;
import java.util.UUID;


public record RolePermission(
        UUID uuid,
        UUID roleId,
        UUID permissionId,
        Instant createdAt,
        Instant deletedAt
) {
    public RolePermissionResponse toResponse() {
        return new RolePermissionResponse(uuid, roleId, permissionId, createdAt, deletedAt);
    }
}