package io.github.lordship.tenancy.internal;

import io.github.lordship.tenancy.Tenancy;
import io.github.lordship.tenancy.TenancyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Validated
@RestController
@RequestMapping("/tenancies")
public class TenancyController {

    private final TenancyService tenancyService;

    public TenancyController(TenancyService tenancyService) {
        this.tenancyService = tenancyService;
    }

    @PreAuthorize("hasAuthority('tenancies:create')")
    @PostMapping("/create")
    public ResponseEntity<TenancyResponse> createTenancy(@RequestBody @Valid TenancyCreateRequest tenancyCreateRequest) {
        Tenancy tenancy = tenancyService.create(tenancyCreateRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(TenancyResponse.from(tenancy));
    }

    @PreAuthorize("hasAuthority('tenancies:read')")
    @GetMapping("/{uuid}")
    public ResponseEntity<TenancyResponse> getById(@PathVariable UUID uuid) {
        return tenancyService.findTenancyById(uuid)
                .map(t -> ResponseEntity.ok(TenancyResponse.from(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('tenancies:read')")
    @GetMapping("/lot/{lotId}")
    public ResponseEntity<List<TenancyResponse>> getByLot(@PathVariable UUID lotId) {
        List<TenancyResponse> responses = tenancyService.findActiveTenancyByLot(lotId)
                .stream()
                .map(TenancyResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasAuthority('tenancies:update')")
    @PatchMapping("/{uuid}")
    public ResponseEntity<TenancyResponse> patchTenancy(
            @PathVariable UUID uuid,
            @RequestBody Map<String, Object> request) {

        Map<String, Object> changes = new HashMap<>();

        if (request.containsKey("startDate")) changes.put("start_date", request.get("startDate"));
        if (request.containsKey("endDate")) changes.put("end_date", request.get("endDate"));
        if (request.containsKey("lotId")) changes.put("lot_id", request.get("lotId"));

        return tenancyService.patchTenancy(uuid, request)
                .map(t -> ResponseEntity.ok(TenancyResponse.from(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('tenancies:update')")
    @PatchMapping("/{uuid}/close")
    public ResponseEntity<TenancyResponse> close(@PathVariable UUID uuid,
                                 @RequestBody @Valid TenancyUpdateRequest request) {
        Tenancy tenancy = tenancyService.endTenancy(uuid, request.endDate());
        return ResponseEntity.ok(TenancyResponse.from(tenancy));
    }

    // may not actually use "Tenancy"
    @PreAuthorize("hasAuthority('tenancies:delete')")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteTenancy(@PathVariable UUID uuid) {
        return tenancyService.softDelete(uuid)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}