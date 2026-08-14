package io.github.lordship.transactions.internal;

import io.github.lordship.transactions.TransactionType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionCreationRequest(

        @NotNull
        UUID accountId,

        @NotNull
        TransactionType type,

        @NotNull
        BigDecimal amount,

        String description,

        @NotNull
        LocalDate billingPeriod

) {
}
