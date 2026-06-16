package io.github.lordship.accounts.internal;

import io.github.lordship.accounts.AccountStatus;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AccountUpdateRequest(

        @NotNull
        AccountStatus accountStatus,

        @NotNull
        BigDecimal balance,

        boolean autopayEnabled,

        String notes

) {
}
