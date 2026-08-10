package io.github.lordship.properties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {}
