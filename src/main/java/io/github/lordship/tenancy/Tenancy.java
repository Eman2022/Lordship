package io.github.lordship.tenancy;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record Tenancy(
    UUID uuid,
    UUID lotId,
    LocalDate startDate,
    LocalDate endDate,
    boolean noPersonalChecks,
    boolean noPartialPayments,
    boolean acceptPayments,
    boolean exemptFromLateFees,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt
) {
    public boolean isSoftDeleted() {
        return deletedAt != null;
    }
}