package io.github.lordship.accounts;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record Account(
        UUID uuid,
        UUID tenancyId,
        AccountStatus accountStatus,
        BigDecimal balance,
        boolean autopayEnabled,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {
    public boolean isSoftDeleted() {
        return deletedAt != null;
    }
}
