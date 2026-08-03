package io.github.lordship.transactions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Transaction(
        UUID uuid,
        UUID accountId,
        TransactionType type,
        BigDecimal amount,
        String description,
        LocalDate billingPeriod,
        OffsetDateTime postedAt,
        OffsetDateTime deletedAt
) {
}
