package io.github.lordship.meterbills;

import io.github.lordship.meters.MeterMeasurement;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MeterBills(
        UUID uuid,
        UUID billedMeter,
        Integer billedAmount,
        Double rateAmount,
        MeterMeasurement rateUnit,
        LocalDate periodStart,
        LocalDate periodEnd,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt
) {
    public boolean isSoftDeleted() {
        return deletedAt != null;
    }
}
