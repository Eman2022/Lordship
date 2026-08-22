package io.github.lordship.meters.internal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MeterReadCreateRequest(
        @NotNull
        Integer meterAmount,

        @NotNull
        @PastOrPresent // prevent dates set in future
        OffsetDateTime readAt,

        @NotNull
        Boolean isEstimated,

        @NotNull
        Integer rolloverCount
) {
}
