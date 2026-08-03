package io.github.lordship.access.internal;

import io.github.lordship.access.Permission;

import java.time.OffsetDateTime;
import java.util.UUID;

// represents a name type of a permission- later assigned to a role
public record PermissionRow (
        UUID uuid,
        String permissionName,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {
    public PermissionRow(String name) {
        this (
                null,
                name,
                null,
                null
        );
    }
    public Permission toPermission() {
        return new Permission(
                uuid,
                permissionName,
                createdAt,
                deletedAt
        );
    }
}
