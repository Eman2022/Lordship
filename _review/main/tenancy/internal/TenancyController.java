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
@RequestMapping("/tenancy")
public class TenancyController {

    private final TenancyService tenancyService;

    public TenancyController(TenancyService tenancyService) {
        this.tenancyService = tenancyService;
    }

    @PreAuthorize("hasAuthority('tenancy:create')")
    @PostMapping("/create")
    public ResponseEntity<TenancyResponse> createTenancy(@RequestBody @Valid TenancyCreateRequest tenancyCreateRequest) {
        Tenancy tenancy = tenancyService.create(tenancyCreateRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(TenancyResponse.from(tenancy));
    }

    @PreAuthorize("hasAuthority('tenancy:view')")
    @GetMapping("/{uuid}")
    public ResponseEntity<TenancyResponse> getById(@PathVariable UUID uuid) {
        return tenancyService.findTenancyById(uuid)
                .map(t -> ResponseEntity.ok(TenancyResponse.from(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('tenancy:view')")
    @GetMapping("/lot/{lotId}")
    public ResponseEntity<List<TenancyResponse>> getByLot(@PathVariable UUID lotId) {
        List<TenancyResponse> responses = tenancyService.findActiveTenancyByLot(lotId)
                .stream()
                .map(TenancyResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasAuthority('tenancy:edit')")
    @PatchMapping("/{uuid}")
    public ResponseEntity<TenancyResponse> patchTenancy(
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
            return tenancyService.patchTenancy(uuid, changes)
                    .map(t -> ResponseEntity.ok(TenancyResponse.from(t)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasAuthority('tenancy:edit')")
    @PatchMapping("/{uuid}/close")
    public ResponseEntity<TenancyResponse> close(@PathVariable UUID uuid,
                                                 @RequestBody @Valid TenancyUpdateRequest request) {
        Tenancy tenancy = tenancyService.endTenancy(uuid, request.endDate());
        return ResponseEntity.ok(TenancyResponse.from(tenancy));
    }

    @PreAuthorize("hasAuthority('tenancy:delete')")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteTenancy(@PathVariable UUID uuid) {
        return tenancyService.softDelete(uuid)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}