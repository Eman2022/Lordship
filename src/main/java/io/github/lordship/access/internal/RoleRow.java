package io.github.lordship.access.internal;

import io.github.lordship.access.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record RoleRow(
        UUID uuid,
        String roleName,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {
    public RoleRow(String name) {
        this(
            null,
            name,
            null,
            null
        );
    }

    public Role toRole() {
        return new Role(
                this.uuid,
                this.roleName,
                this.createdAt,
                this.deletedAt
        );
    }
}
