package io.github.lordship.meterbills;

import java.time.LocalDateTime;
import java.util.UUID;

public record MeterBills(
        UUID uuid,
        UUID billedMeter,
        Integer billedAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
    public boolean isSoftDeleted() {
        return deletedAt != null;
    }
}
