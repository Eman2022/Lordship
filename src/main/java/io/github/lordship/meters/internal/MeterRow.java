package io.github.lordship.meters.internal;

import io.github.lordship.meters.MeterMeasurement;
import io.github.lordship.meters.MeterType;
import io.github.lordship.meters.Meters;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt,
        MeterType utilityType,
        MeterMeasurement measurement,
        Boolean isMasterMeter,
        Integer rolloverMax,
        Double meterMultiplier,
        Integer readDueDay,
        Boolean isBimonthly
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
                this.utilityType,
                this.measurement,
                this.isMasterMeter,
                this.rolloverMax,
                this.meterMultiplier,
                this.readDueDay,
                this.isBimonthly
        );
    }

    public static MeterRow forInsert(
            UUID meterId,
            Double pointX,
            Double pointY,
            MeterType utilityType,
            MeterMeasurement measurement,
            Boolean isMasterMeter,
            Integer rolloverMax,
            Double meterMultiplier,
            Integer readDueDay,
            Boolean isBimonthly
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
                utilityType,
                measurement,
                isMasterMeter,
                rolloverMax,
                meterMultiplier,
                readDueDay,
                isBimonthly
        );
    }
}