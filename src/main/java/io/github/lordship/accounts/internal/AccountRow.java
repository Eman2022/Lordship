package io.github.lordship.accounts.internal;

import io.github.lordship.accounts.Account;
import io.github.lordship.accounts.AccountStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountRow(
        UUID uuid,
        UUID tenancyId,
        String accountStatus,
        BigDecimal balanceCached,
        boolean autopayEnabled,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {
    public Account toAccount() {
        return new Account(
                this.uuid,
                this.tenancyId,
                AccountStatus.valueOf(this.accountStatus),
                this.balanceCached,
                this.autopayEnabled,
                this.notes,
                this.createdAt,
                this.deletedAt
        );
    }

    public AccountRow(UUID tenancyId, String notes) {
        this(
                null,
                tenancyId,
                AccountStatus.ACTIVE.name(),
                BigDecimal.ZERO,
                false,
                notes,
                null,
                null
        );
    }
}
