package io.github.lordship.properties.internal;

import io.github.lordship.properties.Property;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PropertyRow(
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
) {

    public Property toProperty() {
        return new Property(
                this.uuid,
                this.propertyCode,
                this.propertyName,
                this.propertyAddress,
                this.propertyCity,
                this.propertyState,
                this.purchaseDate,
                this.yearBuilt,
                this.lateFeeRate,
                this.createdAt,
                this.deletedAt
        );
}
public PropertyRow(String propertyName, String propertyAddress){
        this(
                null,
                null,
                propertyName,
                propertyAddress,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
}

public PropertyRow(UUID uuid, String propertyCode, String propertyName, String propertyAddress,
                   String propertyCity, String propertyState,
                   LocalDate purchaseDate, Integer yearBuilt) {
    this(
            uuid,
            propertyCode,
            propertyName,
            propertyAddress,
            propertyCity,
            propertyState,
            purchaseDate,
            yearBuilt,
            null,
            null,
            null
    );
    }
}
