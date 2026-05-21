package io.github.lordship.access;

import java.time.LocalDateTime;
import java.util.UUID;


public record Agent(
    UUID uuid,
    UUID personId,
    String workPhone,
    String workEmail,
    LocalDateTime createdAt,
    LocalDateTime deletedAt
) {

    public boolean isSoftDeleted() {
        return deletedAt != null;
    }

}
