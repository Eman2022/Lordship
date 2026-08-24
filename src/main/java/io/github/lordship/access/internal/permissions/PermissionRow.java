package io.github.lordship.access.internal.permissions;

import io.github.lordship.access.Permission;

import java.util.UUID;

// represents a name type of a permission- later assigned to a role
public record PermissionRow (
        UUID uuid,
        String permissionName
) {

    public PermissionRow(String name) {
        this (
                null,
                name
        );
    }

    public Permission toPermission() {
        return new Permission(
                uuid,
                permissionName
        );
    }
}
