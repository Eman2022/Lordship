package io.github.lordship.tenants.internal;

import io.github.lordship.tenants.Tenant;
import io.github.lordship.tenants.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/tenants")
public class TenantController {
    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PreAuthorize("hasAuthority('tenants:create')")
    @PostMapping("/create")
    public ResponseEntity<TenantResponse> createTenant(@RequestBody @Valid TenantCreateRequest request) {
        Tenant tenant = tenantService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TenantResponse.from(tenant));
    }
    @PreAuthorize("hasAuthority('tenants:view')")
    @GetMapping("/{uuid}")
    public ResponseEntity<TenantResponse> getById(@PathVariable UUID uuid) {
        return tenantService.findById(uuid)
                .map(t -> ResponseEntity.ok(TenantResponse.from(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('tenants:edit')")
    @PatchMapping("/{uuid}")
    public ResponseEntity<TenantResponse> patchTenant(
            @PathVariable UUID uuid,
            @RequestBody Map<String, Object> request) {

        Map<String, Object> changes = new HashMap<>();

        if (request.containsKey("startDate")) {
            changes.put("start_date", request.get("startDate"));
        }

        if (request.containsKey("endDate")) {
            changes.put("end_date", request.get("endDate"));
        }

        try {
            return tenantService.patchTenant(uuid, changes)
                    .map(t -> ResponseEntity.ok(TenantResponse.from(t)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasAuthority('tenants:delete')")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteTenant(@PathVariable UUID uuid) {
        return tenantService.softDelete(uuid)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}