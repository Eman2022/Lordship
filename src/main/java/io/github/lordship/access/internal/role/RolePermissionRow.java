package io.github.lordship.access.internal.role;

import java.time.OffsetDateTime;
import java.util.UUID;

// represents a permission assigned to a role
public record RolePermissionRow(
        UUID uuid,
        UUID roleId,
        UUID permissionId,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
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

