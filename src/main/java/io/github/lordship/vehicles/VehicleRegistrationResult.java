package io.github.lordship.vehicles;

import java.math.BigDecimal;
import java.util.List;

public record VehicleRegistrationResult (
        Vehicle vehicle,
        BigDecimal applicableFee,
        boolean plateConflictFlagged,
        List<Vehicle> conflictingVehicles
) {}
