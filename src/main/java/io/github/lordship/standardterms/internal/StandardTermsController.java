package io.github.lordship.standardterms.internal;

import io.github.lordship.identity.AgentPrincipal;
import io.github.lordship.identity.LordshipPrincipal;
import io.github.lordship.shared.AgreementType;
import io.github.lordship.standardterms.StandardTerms;
import io.github.lordship.standardterms.StandardTermsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/standard-terms")
public class StandardTermsController {

    // JSON field -> column. Twenty-three explicit ifs would be worse than the loop.
    private static final Map<String, String> PATCHABLE_COLUMNS = Map.ofEntries(
            Map.entry("name", "name"),
            Map.entry("targetRate", "target_rate"),
            Map.entry("carFee", "car_fee"),
            Map.entry("allowedCars", "allowed_cars"),
            Map.entry("carsMax", "cars_max"),
            Map.entry("petFee", "pet_fee"),
            Map.entry("allowedPets", "allowed_pets"),
            Map.entry("paymentDueDay", "payment_due_day"),
            Map.entry("gracePeriodDays", "grace_period_days"),
            Map.entry("ruleViolationFeeMethod", "rule_violation_fee_method"),
            Map.entry("ruleViolationFeeAmount", "rule_violation_fee_amount"),
            Map.entry("nsfFeeMethod", "nsf_fee_method"),
            Map.entry("nsfFeeAmount", "nsf_fee_amount"),
            Map.entry("lateFeeMethod", "late_fee_method"),
            Map.entry("lateFeeAmount", "late_fee_amount"),
            Map.entry("waterMethod", "water_method"),
            Map.entry("waterFlatAmount", "water_flat_amount"),
            Map.entry("powerMethod", "power_method"),
            Map.entry("powerFlatAmount", "power_flat_amount"),
            Map.entry("sewerMethod", "sewer_method"),
            Map.entry("sewerFlatAmount", "sewer_flat_amount"),
            Map.entry("trashMethod", "trash_method"),
            Map.entry("trashFlatAmount", "trash_flat_amount"),
            Map.entry("note", "note"));

    private final StandardTermsService standardTermsService;

    public StandardTermsController(StandardTermsService standardTermsService) {
        this.standardTermsService = standardTermsService;
    }

    public record CreateGlobalTemplateRequest(
            @NotBlank String name,
            @NotNull AgreementType agreementType) {}

    public record CopyTemplateRequest(@NotNull UUID propertyId ) {}

    // The deal types this property may offer. Pass agreementType to narrow to one.
    @PreAuthorize("hasAuthority('standard_terms:view')")
    @GetMapping
    public ResponseEntity<List<StandardTermsResponse>> listByProperty(
            @RequestParam("property") UUID property,
            @RequestParam(value = "agreementType", required = false) AgreementType agreementType) {

        List<StandardTerms> found = (agreementType == null)
                ? standardTermsService.findByProperty(property)
                : standardTermsService.findForProperty(property, agreementType).stream().toList();

        return ResponseEntity.ok(found.stream().map(StandardTermsResponse::from).toList());
    }

    // Admin-only: the pool properties copy from.
    @PreAuthorize("hasAuthority('standard_terms:manage_global')")
    @GetMapping("/global")
    public ResponseEntity<List<StandardTermsResponse>> listGlobalTemplates() {
        return ResponseEntity.ok(
                standardTermsService.findGlobalTemplates().stream()
                        .map(StandardTermsResponse::from)
                        .toList());
    }

    @PreAuthorize("hasAuthority('standard_terms:view')")
    @GetMapping("/{uuid}")
    public ResponseEntity<StandardTermsResponse> getStandardTerms(@PathVariable UUID uuid) {
        return standardTermsService.findById(uuid)
                .map(StandardTermsResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('standard_terms:manage_global')")
    @PostMapping("/global")
    public ResponseEntity<StandardTermsResponse> createGlobalTemplate(
            @Valid @RequestBody CreateGlobalTemplateRequest request) {

        StandardTerms created =
                standardTermsService.createGlobalTemplate(request.name(), request.agreementType());
        return ResponseEntity.status(HttpStatus.CREATED).body(StandardTermsResponse.from(created));
    }

    // Copying a template in is what authorizes a property to offer that agreement type.
    @PreAuthorize("hasAuthority('standard_terms:manage_global')")
    @PostMapping("/{templateId}/copy")
    public ResponseEntity<StandardTermsResponse> copyTemplateToProperty(
            @PathVariable UUID templateId,
            @Valid @RequestBody CopyTemplateRequest request) {

        return standardTermsService.copyTemplateToProperty(templateId, request.propertyId())
                .map(StandardTermsResponse::from)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('standard_terms:edit')")
    @PatchMapping("/{uuid}")
    public ResponseEntity<StandardTermsResponse> patchStandardTerms(
            @PathVariable UUID uuid,
            @RequestBody Map<String, Object> request) {

        Map<String, Object> changes = new HashMap<>();
        PATCHABLE_COLUMNS.forEach((jsonField, column) -> {
            if (request.containsKey(jsonField)) {
                changes.put(column, request.get(jsonField));
            }
        });

        return standardTermsService.patchStandardTerms(uuid, changes)
                .map(StandardTermsResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('standard_terms:delete')")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteStandardTerms(@PathVariable UUID uuid) {
        return standardTermsService.deleteStandardTerms(uuid)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Void> badRequest() {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Void> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
}