package io.github.lordship.propertyassignments.internal;

import jakarta.validation.constraints.NotNull;


import java.util.UUID;

public record PropertyAssignmentRequest(
        @NotNull
        UUID agentId,

        @NotNull
        UUID propertyId
) {
}
