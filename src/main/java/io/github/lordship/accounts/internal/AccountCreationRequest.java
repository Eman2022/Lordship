package io.github.lordship.accounts.internal;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AccountCreationRequest(

        @NotNull
        UUID tenancyId,

        String notes

) {
}
