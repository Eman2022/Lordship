package io.github.lordship.tenants;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Tenant(
        UUID uuid,
        UUID tenancyId,
        UUID personId,
        LocalDate startDate,
        LocalDate endDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt
) {
}