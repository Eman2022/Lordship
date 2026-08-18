package io.github.lordship.access;

import io.github.lordship.access.internal.rbac.GrantedRoleResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

// record of a role being granted to an agent
public record GrantedRole(
    UUID uuid,
    UUID agentId,
    UUID roleId,
    UUID grantedBy,
    UUID revokedBy,
    OffsetDateTime createdAt,
    OffsetDateTime deletedAt
) {
    public GrantedRoleResponse toResponse() {
        return new GrantedRoleResponse(uuid, agentId, roleId, grantedBy);
    }
}