package io.github.lordship.homes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Home(
        UUID uuid,
        String name,
        UUID lotId,
        BigDecimal estimatedValue,
        LocalDate estimatedValueOn,
        Integer modelYear,
        String make,
        String model,
        Integer bedroomCount,
        BigDecimal bathroomCount,
        BigDecimal width,
        BigDecimal length,
        String dimensionsUnits,
        Integer sections,
        HomeCondition condition,
        String appearance,
        String note,
        String parcel,
        String vin,
        Boolean parkOwned,
        OffsetDateTime createdAt,
        UUID createdBy,
        OffsetDateTime deletedAt
) {
    public boolean isSoftDeleted() {
        return deletedAt != null;
    }
}
