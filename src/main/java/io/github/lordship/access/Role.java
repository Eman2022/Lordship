package io.github.lordship.access;

import java.time.LocalDateTime;
import java.util.UUID;

// A name for a role - roles are granted permissions. Roles are granted to agents.
public record Role(
        UUID uuid,
        String roleName,
        String roleDescription,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {

    public boolean isSoftDeleted() {
        return deletedAt != null;
    }
}
