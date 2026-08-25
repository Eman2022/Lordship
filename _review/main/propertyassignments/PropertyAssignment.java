package io.github.lordship.propertyassignments;

import java.time.LocalDateTime;
import java.util.UUID;

public record PropertyAssignment (
        UUID uuid,
        UUID agentId,
        UUID propertyId,
        UUID assignedBy,
        LocalDateTime assignedAt,
        LocalDateTime removedAt
) {
}
