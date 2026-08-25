package io.github.lordship.properties;

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
        String propertyZip,
        LocalDate purchaseDate,
        String propertyZoning,
        String propertyParcel,
        Integer yearBuilt,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {}
