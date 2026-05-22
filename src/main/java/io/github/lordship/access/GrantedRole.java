package io.github.lordship.access;

import java.time.LocalDateTime;
import java.util.UUID;

// record of a role being granted to an agent
public record GrantedRole(
    UUID uuid,
    UUID agentId,
    UUID roleId,
    UUID grantedBy,
    UUID revokedBy,
    LocalDateTime createdAt,
    LocalDateTime deletedAt
) {
}
