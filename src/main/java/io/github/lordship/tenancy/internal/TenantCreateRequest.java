package io.github.lordship.tenancy.internal;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TenantCreateRequest(
    @NotNull
    UUID tenancyId,

    @NotNull
    UUID personId
) {

}
