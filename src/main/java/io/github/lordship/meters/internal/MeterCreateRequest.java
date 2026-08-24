package io.github.lordship.meters.internal;

import io.github.lordship.meters.MeterMeasurement;
import io.github.lordship.meters.MeterType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MeterCreateRequest(
    @NotNull
    UUID meterId,

    @NotNull
    Double pointX,

    @NotNull
    Double pointY,

    @NotNull
    MeterType utilityType,

    @NotNull
    MeterMeasurement measurement,

    @NotNull
    Boolean isMasterMeter,

    @NotNull
    Integer rolloverMax,

    @NotNull
    Double meterMultiplier,

    @NotNull
    Integer readDueDay,

    @NotNull
    Boolean isBimonthly
) {
}
