package io.github.lordship.access.internal;

import io.github.lordship.access.Permission;

import java.time.LocalDateTime;
import java.util.UUID;

// represents a name type of a permission- later assigned to a role
public record PermissionRow (
        UUID uuid,
        String permissionName,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
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
