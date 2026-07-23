package io.github.lordship.accounts.internal;

import io.github.lordship.accounts.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record AccountUpdateRequest(

        @NotNull
        AccountStatus accountStatus,

        boolean autopayEnabled,

        String notes

) {
}
