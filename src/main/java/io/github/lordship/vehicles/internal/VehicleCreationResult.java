package io.github.lordship.vehicles.internal;

import io.github.lordship.vehicles.Vehicle;

import java.util.List;

public record VehicleCreationResult(
        Vehicle vehicle,

        // let conflicts exist but FLAG on conflict
        boolean plateConflictFlagged,
        List<Vehicle> conflictingVehicles
) {}
