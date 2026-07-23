package io.github.lordship.propertyassignments.internal;

import io.github.lordship.propertyassignments.PropertyAssignment;

import java.time.LocalDateTime;
import java.util.UUID;

public record PropertyAssignmentRow(
        UUID uuid,
        UUID agentId,
        UUID propertyId,
        UUID assignedBy,
        LocalDateTime assignedAt,
        LocalDateTime removedAt
) {
    public PropertyAssignment toPropertyAssignment(){
        return new PropertyAssignment(uuid, agentId, propertyId, assignedBy, assignedAt, removedAt);
    }

    public PropertyAssignmentRow(UUID agentId, UUID propertyId, UUID assignedBy) {
        this(
                null,
                agentId,
                propertyId,
                assignedBy,
                null,
                null
        );
    }
}
