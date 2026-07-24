package io.github.lordship.meters.internal;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MeterCreateRequest(
    @NotNull
    UUID meterId
) {
}
