package io.github.lordship.vehicles.internal;

import io.github.lordship.vehicles.Vehicle;
import io.github.lordship.vehicles.VehiclePolicy;
import io.github.lordship.vehicles.VehicleRegistrationResult;
import io.github.lordship.vehicles.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/vehicles")
public class VehicleController {
    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PreAuthorize("hasAuthority('vehicles:create')")
    @PostMapping("/register")
    public ResponseEntity<VehicleRegistrationResult> registerVehicle(
            @Valid @RequestBody VehicleCreateRequest request) {
        VehicleRegistrationResult result = vehicleService.registerVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PreAuthorize("hasAuthority('vehicles:view')")
    @GetMapping("/{uuid}")
    public ResponseEntity<Vehicle> getVehicle(@PathVariable UUID uuid) {
        return vehicleService.findById(uuid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('vehicles:view')")
    @GetMapping("/tenancy/{tenancyUuid}")
    public ResponseEntity<List<Vehicle>> getVehiclesByTenancy(@PathVariable UUID tenancyUuid) {
        return ResponseEntity.ok(vehicleService.findByTenancy(tenancyUuid));
    }

    @PreAuthorize("hasAuthority('vehicles:view')")
    @GetMapping("/property/{propertyCode}")
    public ResponseEntity<List<Vehicle>> getVehiclesByProperty(@PathVariable UUID propertyCode) {
        return ResponseEntity.ok(vehicleService.findByProperty(propertyCode));
    }

    @PreAuthorize("hasAuthority('vehicles:edit')")
    @PatchMapping("/{uuid}")
    public ResponseEntity<Vehicle> patchVehicle(
            @PathVariable UUID uuid,
            @RequestBody Map<String, Object> request) {

        Map<String, Object> changes = new HashMap<>();
        if (request.containsKey("make"))        changes.put("make", request.get("make"));
        if (request.containsKey("model"))       changes.put("model", request.get("model"));
        if (request.containsKey("year"))        changes.put("year", request.get("year"));
        if (request.containsKey("plateNumber")) changes.put("plate_number", request.get("plateNumber"));
        if (request.containsKey("plateState"))  changes.put("plate_state", request.get("plateState"));
        if (request.containsKey("color"))       changes.put("color", request.get("color"));
        if (request.containsKey("notes"))       changes.put("notes", request.get("notes"));

        return vehicleService.patchVehicle(uuid, changes)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('vehicles:delete')")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable UUID uuid) {
        return vehicleService.deleteVehicle(uuid) ?
                ResponseEntity.noContent().build() :
                ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasAuthority('vehicles:edit')")
    @PutMapping("/policy/{propertyCode}")
    public ResponseEntity<VehiclePolicy> setPolicy(
            @PathVariable UUID propertyCode,
            @RequestBody Map<String, Object> request) {

        int freeLimit = (int) request.getOrDefault("freeVehicleLimit", 2);
        BigDecimal fee = new BigDecimal(request.getOrDefault("extraVehicleFee", "0.00").toString());
        String notes = (String) request.getOrDefault("notes", null);

        return ResponseEntity.ok(vehicleService.setPolicy(propertyCode, freeLimit, fee, notes));
    }

    @PreAuthorize("hasAuthority('vehicles:view')")
    @GetMapping("/policy/{propertyCode}")
    public ResponseEntity<VehiclePolicy> getPolicy(@PathVariable UUID propertyCode) {
        return vehicleService.getPolicy(propertyCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
