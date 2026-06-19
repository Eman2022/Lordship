package io.github.lordship.properties.internal;

import io.github.lordship.properties.Property;
import io.github.lordship.properties.PropertyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

}