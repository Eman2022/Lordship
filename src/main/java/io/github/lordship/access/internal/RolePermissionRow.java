package io.github.lordship.access.internal;

import java.time.LocalDateTime;
import java.util.UUID;

// represents a permission assigned to a role
public record RolePermissionRow(
        UUID uuid,
        UUID roleId,
        UUID permissionId,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {
    public RolePermissionRow(UUID roleId, UUID permissionId){
        this(
          null,
          roleId,
          permissionId,
          null,
          null
        );
    }
}

