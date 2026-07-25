package io.github.lordship.meters.internal;

import io.github.lordship.meters.MeterMeasurement;
import io.github.lordship.meters.MeterType;
import io.github.lordship.meters.Meters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record MeterRow(
        UUID uuid,
        UUID meterId,
        String title,
        String description,
        String serialNumber,
        Double pointX,
        Double pointY,
        LocalDate installedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt,
        String utilityType,
        String measurement
) {
    public Meters toMeters(){
        return new Meters(
                this.uuid,
                this.meterId,
                this.title,
                this.description,
                this.serialNumber,
                this.pointX,
                this.pointY,
                this.installedAt,
                this.createdAt,
                this.updatedAt,
                this.deletedAt,
                MeterType.valueOf(this.utilityType),
                MeterMeasurement.valueOf(this.measurement)
        );
    }

    public static MeterRow forInsert(
            UUID meterId,
            Double pointX,
            Double pointY,
            MeterType utilityType,
            MeterMeasurement measurement
    ) {
        return new MeterRow(
                null,
                meterId,
                null,
                null,
                null,
                pointX,
                pointY,
                null,
                null,
                null,
                null,
                utilityType.name(),
                measurement.name() // .name() is for the enum values
        );
    }

}