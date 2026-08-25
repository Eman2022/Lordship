package io.github.lordship.properties.internal;

import io.github.lordship.properties.Property;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PropertyRow(
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
) {

    public Property toProperty() {
        return new Property(
                this.uuid,
                this.propertyCode,
                this.propertyName,
                this.propertyAddress,
                this.propertyCity,
                this.propertyState,
                this.propertyZip,
                this.purchaseDate,
                this.propertyZoning,
                this.propertyParcel,
                this.yearBuilt,
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
                null,
                null,
                null
        );
}

public PropertyRow(UUID uuid, String propertyCode, String propertyName, String propertyAddress,
                   String propertyCity, String propertyState, String propertyZip,
                   LocalDate purchaseDate, String propertyZoning, String propertyParcel, Integer yearBuilt) {
    this(
            uuid,
            propertyCode,
            propertyName,
            propertyAddress,
            propertyCity,
            propertyState,
            propertyZip,
            purchaseDate,
            propertyZoning,
            propertyParcel,
            yearBuilt,
            null,
            null
    );
    }
}
