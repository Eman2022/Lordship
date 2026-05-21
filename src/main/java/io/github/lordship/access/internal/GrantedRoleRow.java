package io.github.lordship.access.internal;

import java.time.LocalDateTime;
import java.util.UUID;

public record GrantedRoleRow(
        UUID uuid,
        UUID agentId,
        UUID roleId,
        UUID grantedBy,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {
    public GrantedRole toGrantedRole() {
        return new GrantedRole(
                uuid,
                agentId,
                roleId,
                grantedBy,
                createdAt,
                deletedAt
        );
    }
}