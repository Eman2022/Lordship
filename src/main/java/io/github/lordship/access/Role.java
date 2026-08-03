package io.github.lordship.access;

import java.time.OffsetDateTime;
import java.util.UUID;

// A name for a role - roles are granted permissions. Roles are granted to agents.
public record Role(
        UUID uuid,
        String roleName,
        String roleDescription,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {
    public boolean isSoftDeleted() {
        return deletedAt != null;
    }
}
