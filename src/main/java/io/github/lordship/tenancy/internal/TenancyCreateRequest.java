package io.github.lordship.tenancy.internal;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;


public record TenancyCreateRequest(
    @NotNull
    UUID lotId
) {
}