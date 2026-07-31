package io.github.lordship.vehicles.internal;

import io.github.lordship.vehicles.VehiclePolicy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record VehiclePolicyRow (
        UUID uuid,
        UUID propertyUuid,
        int freeVehicleLimit,
        BigDecimal extraVehicleFee,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public VehiclePolicy toPolicy() {
        return new VehiclePolicy(
                this.uuid,
                this.propertyUuid,
                this.freeVehicleLimit,
                this.extraVehicleFee,
                this.notes
        );
    }
}
