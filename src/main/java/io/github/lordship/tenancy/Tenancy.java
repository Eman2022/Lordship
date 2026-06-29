package io.github.lordship.tenancy;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record Tenancy(
    UUID uuid,
    UUID lotID,
    LocalDate startDate,
    LocalDate endDate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt
) {
    public boolean isSoftDeleted() {
        return deletedAt != null;
    }
}