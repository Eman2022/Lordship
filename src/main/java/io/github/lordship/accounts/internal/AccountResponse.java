package io.github.lordship.accounts.internal;

import io.github.lordship.accounts.Account;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponse(
        UUID uuid,
        UUID tenancyId,
        String accountStatus,
        BigDecimal balanceCached,
        boolean autopayEnabled,
        String notes,
        boolean noPersonalChecks,
        boolean noPartialPayments,
        boolean acceptPayments,
        boolean exemptFromLateFees,
        LocalDateTime createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.uuid(),
                account.tenancyId(),
                account.accountStatus().name(),
                account.balanceCached(),
                account.autopayEnabled(),
                account.notes(),
                account.noPersonalChecks(),
                account.noPartialPayments(),
                account.acceptPayments(),
                account.exemptFromLateFees(),
                account.createdAt()
        );
    }
}
