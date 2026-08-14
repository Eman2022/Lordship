package io.github.lordship.vehicles;

import java.time.LocalDateTime;
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
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {}
