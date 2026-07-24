package io.github.lordship.meters.internal;

import io.github.lordship.meters.Meters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record MeterRow(
        UUID uuid,
        UUID meterId,
        String title,
        String description,
        String measurement,
        Double pointX,
        Double pointY,
        LocalDate installedAt,
        LocalDateTime deletedAt
) {
    public Meters toMeters(){
        return new Meters(
                this.uuid,
                this.meterId,
                this.title,
                this.description,
                this.measurement,
                this.pointX,
                this.pointY,
                this.installedAt,
                this.deletedAt
        );
    }

    public static MeterRow forInsert(
            UUID meterId,
            String measurement,
            Double pointX,
            Double pointY
    ) {
        return new MeterRow(
                null,
                meterId,
                null,
                null,
                measurement,
                pointX,
                pointY,
                null,
                null
        );
    }

}