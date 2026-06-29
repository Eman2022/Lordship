package io.github.lordship.transactions.internal;

import io.github.lordship.transactions.Transaction;
import io.github.lordship.transactions.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionRow(
        UUID uuid,
        UUID accountId,
        String type,
        BigDecimal amount,
        String description,
        LocalDate billingPeriod,
        LocalDateTime postedAt,
        LocalDateTime deletedAt
) {
    public Transaction toTransaction() {
        return new Transaction(
                this.uuid,
                this.accountId,
                TransactionType.valueOf(this.type),
                this.amount,
                this.description,
                this.billingPeriod,
                this.postedAt,
                this.deletedAt
        );
    }

    public TransactionRow(UUID accountId, TransactionType type, BigDecimal amount,
                          String description, LocalDate billingPeriod) {
        this(
                null,
                accountId,
                type.name(),
                amount,
                description,
                billingPeriod,
                null,
                null
        );
    }
}
