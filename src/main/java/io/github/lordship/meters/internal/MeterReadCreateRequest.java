package io.github.lordship.meters.internal;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MeterReadCreateRequest(
        @NotNull
        UUID targetedMeter,

        @NotNull
        Integer meterAmount,

        @NotNull
        OffsetDateTime readAt,

        @NotNull
        Boolean isEstimated,

        @NotNull
        Integer rolloverCount
) {
}
