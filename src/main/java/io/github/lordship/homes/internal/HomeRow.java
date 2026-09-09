package io.github.lordship.homes.internal;

import io.github.lordship.homes.Home;
import io.github.lordship.homes.HomeCondition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record HomeRow(
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
    public HomeRow {
        if (estimatedValueOn != null && estimatedValue == null) {
            throw new IllegalArgumentException("estimatedValue is required when a valuation date is recorded");
        }
    }

    public Home toHome() {
        return new Home(
                this.uuid,
                this.name,
                this.lotId,
                this.estimatedValue,
                this.estimatedValueOn,
                this.modelYear,
                this.make,
                this.model,
                this.bedroomCount,
                this.bathroomCount,
                this.width,
                this.length,
                this.dimensionsUnits,
                this.sections,
                this.condition,
                this.appearance,
                this.note,
                this.parcel,
                this.vin,
                this.parkOwned,
                this.createdAt,
                this.createdBy,
                this.deletedAt
        );
    }

    // Convenience constructor for insert
    public HomeRow(UUID lotId, UUID createdBy) {
        this(
                null,
                null,
                lotId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                createdBy,
                null
        );
    }
}
