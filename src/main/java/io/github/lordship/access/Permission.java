package io.github.lordship.access;

import java.time.OffsetDateTime;
import java.util.UUID;

// A simple name for a permission. Permissions are granted to roles
public record Permission(
        UUID uuid,
        String permissionName,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
    ) {
    public boolean isSoftDeleted() {
        return deletedAt != null;
    }
}