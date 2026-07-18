package io.github.lordship.properties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record Property(
        UUID uuid,
        String propertyCode,
        String propertyName,
        String propertyAddress,
        String propertyCity,
        String propertyState,
        LocalDate purchaseDate,
        Integer yearBuilt,
        BigDecimal lateFeeRate,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {}
