package io.github.lordship.meters;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MeterRelation(
        UUID uuid,
        UUID parentMeter,
        UUID childMeter,
        Boolean hasUnmeteredRemainder,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}