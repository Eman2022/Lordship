package io.github.lordship.vehicles.internal;

import io.github.lordship.vehicles.Vehicle;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleRow (
        UUID uuid,
        UUID tenancyId,
        String make,
        String model,
        Integer year,
        String plateNumber,
        String plateState,
        String color,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {
    public VehicleRow(UUID tenancyId, String plateNumber) {
        this(
                null,
                tenancyId,
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
                this.tenancyId,
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