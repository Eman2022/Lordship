package io.github.lordship.vehicles.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record VehicleCreateRequest (
        @NotNull
        UUID tenancyUuid,

        @NotNull
        UUID propertyUuid,
//
//        @NotBlank
//        String make,
//
//        @NotBlank
//        String model,
//
//        @NotNull
//        Integer year,

        @NotBlank
        String plateNumber

//        @Size(min = 2, max = 2, message = "Plate state must be a 2-letter code")
//        String plateState,
//
//        String color,
//
//        String notes
) {}
