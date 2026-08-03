package io.github.lordship.vehicles.internal;

import io.github.lordship.vehicles.Vehicle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record VehicleRow (
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
) {
    public VehicleRow(UUID tenancyUuid, String plateNumber) {
        this(
                null,
                tenancyUuid,
                null,
                null,
                null,
                plateNumber,
                null,
                null,
                null,
                null,null
        );
    }

    public Vehicle toVehicle() {
        return new Vehicle(
                this.uuid,
                this.tenancyUuid,
                this.make,
                this.model,
                this.year,
                this.plateNumber,
                this.plateState,
                this.color,
                this.notes,
                this.createdAt,
                this.deletedAt
        );
}

    // Constructor for new vehicle inserts
    public VehicleRow(UUID tenancyUuid, String make, String model,
                      Integer year, String plateNumber, String plateState, String color, String notes) {
        this(null, tenancyUuid, make, model, year, plateNumber, plateState, color, notes, null,null);
    }
}