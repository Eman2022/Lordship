package io.github.lordship.meters.internal;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record MeterUpdateRequest(
        @NotNull
        UUID uuid,

        UUID lotId,

        LocalDate startDate,

        LocalDate endDate
) {
}
