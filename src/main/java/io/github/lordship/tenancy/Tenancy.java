package io.github.lordship.tenancy;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

public record Tenancy(
    UUID uuid,
    UUID lotNumber,
    UUID accountNumber,
    Date startDate,
    Date endDate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt
) {
    public boolean isSoftDeleted() {
        return deletedAt != null;
    }
}