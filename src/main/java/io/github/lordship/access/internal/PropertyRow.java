package io.github.lordship.access.internal;

import io.github.lordship.properties.Property;
import jakarta.validation.constraints.Null;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PropertyRow(
        String propertyCode,
        String propertyName,
        String propertyAddress,
        String propertyCity,
        String propertyState,
        LocalDate purchaseDate,
        Integer yearBuilt,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {

    public Property toProperty() {
        return new Property(
                this.propertyCode,
                this.propertyName,
                this.propertyAddress,
                this.propertyCity,
                this.propertyState,
                this.purchaseDate,
                this.yearBuilt,
                this.createdAt,
                this.deletedAt
        );
}
public PropertyRow(String propertyName, String propertyAddress){
        this(
                null,
                propertyName,
                propertyAddress,
                null,
                null,
                null,
                null


        );
}

public PropertyRow(String propertyCode, String propertyName, String propertyAddress,
                   String propertyCity, String propertyState,
                   LocalDate purchaseDate, Integer yearBuilt) {
    this(
            propertyCode,
            propertyName,
            propertyAddress,
            propertyCity,
            propertyState,
            purchaseDate,
            yearBuilt,
            null,
            null
    );
    }
}
