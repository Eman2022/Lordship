package io.github.lordship.access.internal;

import java.time.LocalDateTime;
import java.util.UUID;

// record of a role being granted to an agent
public record GrantedRole(
    UUID uuid,
    UUID agentId,
    UUID roleId,
    UUID grantedBy,
    LocalDateTime createdAt,
    LocalDateTime deletedAt
) {
}
