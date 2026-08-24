package io.github.lordship.access;

import java.util.UUID;

// A simple name for a permission. Permissions are granted to roles
public record Permission(
        UUID uuid,
        String permissionName
) { }