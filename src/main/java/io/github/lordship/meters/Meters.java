package io.github.lordship.meters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record Meters(
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
    public boolean isSoftDeleted() {
        return deletedAt != null;
    }
}
