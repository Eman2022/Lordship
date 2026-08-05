package io.github.lordship.meters;

import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Meters(
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
    Boolean isMasterMeter
) {
    public boolean isSoftDeleted() {
        return deletedAt != null;
    }
}
