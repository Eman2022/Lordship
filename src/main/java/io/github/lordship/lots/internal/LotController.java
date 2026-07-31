package io.github.lordship.lots.internal;

import io.github.lordship.lots.Lot;
import io.github.lordship.lots.LotCreationRequest;
import io.github.lordship.lots.LotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/lots")
public class LotController {

    private final LotService lotService;

    public LotController(LotService lotService) {
        this.lotService = lotService;
    }

    @PreAuthorize("hasAuthority('lots:create')")
    @PostMapping
    public ResponseEntity<LotResponse> createLot(@Valid @RequestBody LotCreationRequest request) {
        Lot lot = lotService.createLot(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(LotResponse.from(lot));
    }

    // The map / list feed for a property, ordered for display.
    @PreAuthorize("hasAuthority('lots:view')")
    @GetMapping
    public ResponseEntity<List<LotResponse>> listByProperty(@RequestParam("property") String propertyCode) {
        List<LotResponse> lots = lotService.findByProperty(propertyCode).stream()
                .map(LotResponse::from)
                .toList();
        return ResponseEntity.ok(lots);
    }

    @PreAuthorize("hasAuthority('lots:view')")
    @GetMapping("/{uuid}")
    public ResponseEntity<LotResponse> getLot(@PathVariable UUID uuid) {
        return lotService.findById(uuid)
                .map(LotResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('lots:edit')")
    @PutMapping("/{uuid}")
    public ResponseEntity<LotResponse> updateLot(@PathVariable UUID uuid,
                                                 @Valid @RequestBody LotUpdateRequest request) {
        return lotService.updateLot(uuid, request)
                .map(LotResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('lots:delete')")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteLot(@PathVariable UUID uuid) {
        return lotService.deleteLot(uuid)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}