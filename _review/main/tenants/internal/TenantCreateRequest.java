package io.github.lordship.tenants.internal;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TenantCreateRequest(
    @NotNull
    UUID tenancyId,

    @NotNull
    UUID personId
) {

}
