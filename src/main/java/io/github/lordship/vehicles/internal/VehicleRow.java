package io.github.lordship.vehicles.internal;

import io.github.lordship.vehicles.Vehicle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record VehicleRow (
        UUID uuid,
        UUID tenancyUuid,
        UUID propertyUuid,
        String make,
        String model,
        Integer year,
        String plateNumber,
        String plateState,
        String color,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
    public VehicleRow(UUID tenancyUuid, UUID propertyUuid, String plateNumber) {
        this(
                tenancyUuid,
                propertyUuid,
                null,
                null,
                null,
                plateNumber,
                null,
                null,
                null
        );
    }

    public Vehicle toVehicle() {
        return new Vehicle(
                this.uuid,
                this.tenancyUuid,
                this.propertyUuid,
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
    public VehicleRow(UUID tenancyUuid, UUID propertyUuid, String make, String model,
                      Integer year, String plateNumber, String plateState, String color, String notes) {
        this(null, tenancyUuid, propertyUuid, make, model, year, plateNumber, plateState, color, notes, null, null, null);
    }
}