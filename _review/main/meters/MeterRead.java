package io.github.lordship.meters;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MeterRead(
        UUID uuid,
        UUID targetedMeter,
        Integer meterAmount,
        OffsetDateTime readAt,
        Boolean isEstimated,
        Integer rolloverCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt
) {
    public boolean isSoftDeleted() {
        return deletedAt != null;
    }
}
