package io.github.lordship.audit.internal;

import jakarta.validation.constraints.*;


public record AgentAuditLogPageRequest(

        @PositiveOrZero
        Integer page,

        @Min(1)
        @Max(100)
        Integer pageSize,

        @NotBlank
        String sortBy,

        boolean ascending
) {
}
