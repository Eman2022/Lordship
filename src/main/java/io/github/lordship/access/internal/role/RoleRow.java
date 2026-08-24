package io.github.lordship.access.internal.role;

import io.github.lordship.access.Role;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RoleRow(
        UUID uuid,
        String roleName,
        String roleDescription,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {

    public RoleRow(String name) {
        this(
            null,
            name,
            null,
            null,
            null
        );
    }

    public RoleRow(String name, String roleDescription) {
        this(
                null,
                name,
                roleDescription,
                null,
                null
        );
    }

    public Role toRole() {
        return new Role(
                this.uuid,
                this.roleName,
                this.roleDescription,
                this.createdAt,
                this.deletedAt
        );
    }
}
