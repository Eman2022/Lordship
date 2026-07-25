package io.github.lordship.meters;

import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt,
    MeterType utilityType,
    MeterMeasurement measurement
) {
    public boolean isSoftDeleted() {
        return deletedAt != null;
    }
}
