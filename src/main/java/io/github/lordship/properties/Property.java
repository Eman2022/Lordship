package io.github.lordship.properties;

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
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {}
