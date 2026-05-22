package io.github.lordship.access.internal;

import io.github.lordship.access.GrantedRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record GrantedRoleRow(
        UUID uuid,
        UUID agentId,
        UUID roleId,
        UUID grantedBy,
        UUID revokedBy,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {
    public GrantedRoleRow(UUID agentId, UUID roleId, UUID grantedBy){
        this(
                null,
                agentId,
                roleId,
                grantedBy,
                null,
                null,
                null
        );
    }

    public GrantedRole toGrantedRole() {
        return new GrantedRole(
                uuid,
                agentId,
                roleId,
                grantedBy,
                revokedBy,
                createdAt,
                deletedAt
        );
    }


}