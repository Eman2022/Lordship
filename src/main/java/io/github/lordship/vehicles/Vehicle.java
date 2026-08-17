package io.github.lordship.vehicles;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Vehicle(
        UUID uuid,
        UUID tenancyUuid,
        String make,
        String model,
        Integer year,
        String plateNumber,
        String plateState,
        String color,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {}
