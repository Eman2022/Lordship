package io.github.lordship.meterbills.internal;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MeterBillsCreateRequest(
        @NotNull
        UUID billedMeter,

        @NotNull
        Integer billedAmount
) {
}
