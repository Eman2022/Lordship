package io.github.lordship.access.internal.grantedrole;

import io.github.lordship.access.GrantedRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GrantedRoleRow(
        UUID uuid,
        UUID agentId,
        UUID roleId,
        UUID grantedBy,
        UUID revokedBy,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
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