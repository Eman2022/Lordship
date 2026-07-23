package io.github.lordship.vehicles.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record VehicleCreateRequest (
        @NotNull
        UUID tenancyUuid,

        @NotNull
        UUID propertyId,

        @NotBlank
        String make,

        @NotBlank
        String model,

        @NotNull
        Integer year,

        @NotBlank
        String plateNumber,

        @Size(min = 6, max = 10)
        String plateState,

        String color,

        String notes
) {}
