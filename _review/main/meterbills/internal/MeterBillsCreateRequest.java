package io.github.lordship.meterbills.internal;

import io.github.lordship.meters.MeterMeasurement;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MeterBillsCreateRequest(
        @NotNull
        UUID billedMeter,

        @NotNull
        BigDecimal billedAmount,

        @NotNull
        BigDecimal rateAmount,

        @NotNull
        MeterMeasurement rateUnit,

        @NotNull
        LocalDate periodStart,

        @NotNull
        LocalDate periodEnd
) {
}
