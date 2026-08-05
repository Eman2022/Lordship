package io.github.lordship.meters.internal;

import io.github.lordship.meters.MeterService;
import io.github.lordship.meters.Meters;
import io.github.lordship.meters.MeterService;
import io.github.lordship.meters.internal.MeterCreateRequest;
import io.github.lordship.tenancy.internal.TenancyResponse;
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
@RequestMapping("/meters")
public class MeterController {

    public final MeterService meterService;
    public MeterController(MeterService meterService) {
        this.meterService = meterService;
    }

    @PreAuthorize("hasAuthority('meters:create')")
    @PostMapping("/create")
    public ResponseEntity<MeterResponse> createMeter(@RequestBody @Valid MeterCreateRequest meterCreateRequest) {
        Meters meters = meterService.create(meterCreateRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(MeterResponse.from(meters));
    }

    @PreAuthorize("hasAuthority('meters:view')")
    @GetMapping("/{uuid}")
    public ResponseEntity<MeterResponse> getById(@PathVariable UUID uuid) {
        return meterService.findMetersById(uuid)
                .map(m -> ResponseEntity.ok(MeterResponse.from(m)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('meters:view')")
    @GetMapping("/lot/{lotId}")
    public ResponseEntity<List<MeterResponse>> getMeterByLot(@PathVariable UUID meterId) {
        List<MeterResponse> responses = meterService.findActiveMetersByLot(meterId)
                .stream()
                .map(MeterResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasAuthority('meters:edit')")
    @PatchMapping("/{uuid}")
    public ResponseEntity<MeterResponse> patchMeter(
            @PathVariable UUID uuid,
            @RequestBody Map<String, Object> request) {

        Map<String, Object> changes = new HashMap<>();

        if (request.containsKey("title")) {
            changes.put("title", request.get("title"));
        }

        if (request.containsKey("description")) {
            changes.put("description", request.get("description"));
        }

        if (request.containsKey("installedAt")) {
            changes.put("installed_at", request.get("installedAt"));
        }

        try {
            return meterService.patchMeter(uuid, changes)
                    .map(t -> ResponseEntity.ok(MeterResponse.from(t)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
