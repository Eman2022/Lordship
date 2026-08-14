package io.github.lordship.vehicles;

import java.math.BigDecimal;
import java.util.UUID;

public record VehiclePolicy (
        UUID uuid,
        UUID propertyUuid,
        int freeVehicleLimit,
        BigDecimal extraVehicleFee,
        String notes
) {}
