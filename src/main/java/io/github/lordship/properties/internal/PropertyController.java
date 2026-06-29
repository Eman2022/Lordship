package io.github.lordship.properties.internal;

import io.github.lordship.properties.Property;
import io.github.lordship.properties.PropertyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

@RestController
@RequestMapping("/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @PreAuthorize("hasAuthority('properties:create')")
    @PostMapping("/create")
    ResponseEntity<Property> createProperty(@Valid @RequestBody PropertyCreateRequest request) {
        Property property = propertyService.createProperty(request.propertyName(), request.propertyAddress());
        return new ResponseEntity<>(property, HttpStatus.CREATED);

    }

    @PreAuthorize("hasAuthority('properties:read')")
    @GetMapping("/{propertyCode}")
    ResponseEntity<Property> readProperty(@PathVariable("propertyCode") String propertyCode) {
        return propertyService.findByPropertyCode(propertyCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('properties:read')")
    @GetMapping("/getAll")
    public ResponseEntity<List<Property>> getAllProperties() {
        return ResponseEntity.ok(propertyService.findAll());
    }

    @PreAuthorize("hasAuthority('properties:edit')")
    @PatchMapping("/{uuid}")
    public ResponseEntity<Property> patchProperty(
            @PathVariable UUID uuid,
            @RequestBody Map<String, Object> request) {

        Map<String, Object> changes = new HashMap<>();

        if (request.containsKey("propertyName"))    changes.put("property_name", request.get("propertyName"));
        if (request.containsKey("propertyAddress")) changes.put("property_address", request.get("propertyAddress"));
        if (request.containsKey("propertyCity"))    changes.put("property_city", request.get("propertyCity"));
        if (request.containsKey("propertyState"))   changes.put("property_state", request.get("propertyState"));
        if (request.containsKey("purchaseDate"))    changes.put("purchaseDate", request.get("purchaseDate"));
        if (request.containsKey("yearBuilt"))       changes.put("year_built", request.get("yearBuilt"));
        if (request.containsKey("propertyManager")) changes.put("propertyManager", request.get("propertyManager"));

        return propertyService.patchProperty(uuid, changes)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('properties:delete')")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteProperty(@PathVariable UUID uuid) {
        return propertyService.deleteProperty(uuid) ?
                ResponseEntity.noContent().build() :
                ResponseEntity.notFound().build();
    }
}