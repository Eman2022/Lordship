package io.github.lordship.vehicles.internal;

import io.github.lordship.vehicles.Vehicle;

import java.time.LocalDateTime;
import java.util.UUID;

public record VehicleRow (
        UUID uuid,
        UUID tenancyUuid,
        UUID propertyId,
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
    public Vehicle toVehicle() {
        return new Vehicle(
                this.uuid,
                this.tenancyUuid,
                this.propertyId,
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
    public VehicleRow(UUID personUuid, UUID propertyId, String make, String model,
                      Integer year, String plateNumber, String plateState, String color, String notes) {
        this(null, personUuid, propertyId, make, model, year, plateNumber, plateState, color, notes, null, null, null);
    }
}