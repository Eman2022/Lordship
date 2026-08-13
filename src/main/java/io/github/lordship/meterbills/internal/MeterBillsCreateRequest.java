package io.github.lordship.meterbills.internal;

import io.github.lordship.meters.MeterMeasurement;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record MeterBillsCreateRequest(
        @NotNull
        UUID billedMeter,

        @NotNull
        Integer billedAmount,

        @NotNull
        Double rateAmount,

        @NotNull
        MeterMeasurement rateUnit,

        @NotNull
        LocalDate periodStart,

        @NotNull
        LocalDate periodEnd
) {
}
