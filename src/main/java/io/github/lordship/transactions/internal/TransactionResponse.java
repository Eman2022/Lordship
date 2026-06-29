package io.github.lordship.transactions.internal;

import io.github.lordship.transactions.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID uuid,
        UUID accountId,
        String type,
        BigDecimal amount,
        String description,
        LocalDate billingPeriod,
        LocalDateTime postedAt,
        LocalDateTime deletedAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.uuid(),
                transaction.accountId(),
                transaction.type().name(),
                transaction.amount(),
                transaction.description(),
                transaction.billingPeriod(),
                transaction.postedAt(),
                transaction.deletedAt()
        );
    }
}
