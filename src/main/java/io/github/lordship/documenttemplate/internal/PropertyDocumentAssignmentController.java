package io.github.lordship.documenttemplate.internal;

import io.github.lordship.documenttemplate.DocumentAudit;
import io.github.lordship.documenttemplate.DocumentAuditService;
import io.github.lordship.documenttemplate.PropertyDocumentAssignmentService;
import io.github.lordship.shared.AgreementType;
import io.github.lordship.shared.InstrumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Which documents a park may generate. Separate from
 * {@code DocumentTemplateController} because assigning is a different act from
 * authoring: one decides what a park can produce, the other decides what the
 * words say, and they are done by different people at different times.
 *
 * <p>Assignment is a reference, not a copy -- an edit to the global document
 * reaches every park assigned to it. Only the assignment itself lives here;
 * a park's own clause exclusions and additions are a separate feature.
 */
@RestController
@RequestMapping("/api/property-documents")
public class PropertyDocumentAssignmentController {

    private static final Map<String, String> PATCHABLE_COLUMNS = Map.ofEntries(
            Map.entry("note", "note"));

    private final PropertyDocumentAssignmentService assignmentService;
    private final DocumentAuditService documentAuditService;

    public PropertyDocumentAssignmentController(PropertyDocumentAssignmentService assignmentService,
                                                DocumentAuditService documentAuditService) {
        this.assignmentService = assignmentService;
        this.documentAuditService = documentAuditService;
    }

    // The two types are not supplied: they come off the template, so a caller
    // cannot assign a lease template and call it a notice.
    public record AssignDocumentRequest(
            @NotNull UUID propertyId,
            @NotNull UUID documentTemplateId) { }

    /** Everything this park can generate. The setup screen's whole content. */
    @PreAuthorize("hasAuthority('property_document:view')")
    @GetMapping
    public ResponseEntity<List<PropertyDocumentAssignmentResponse>> listByProperty(
            @RequestParam("property") UUID propertyId) {

        return ResponseEntity.ok(
                assignmentService.findByProperty(propertyId).stream()
                        .map(PropertyDocumentAssignmentResponse::from)
                        .toList());
    }

    /**
     * What this park uses for one kind of deal. Answers with 404 when the park
     * cannot generate that document at all, which is the same question generate
     * asks before it starts.
     */
    @PreAuthorize("hasAuthority('property_document:view')")
    @GetMapping("/resolve")
    public ResponseEntity<PropertyDocumentAssignmentResponse> resolve(
            @RequestParam("property") UUID propertyId,
            @RequestParam("agreementType") AgreementType agreementType,
            @RequestParam("instrumentType") InstrumentType instrumentType) {

        return assignmentService.findForProperty(propertyId, agreementType, instrumentType)
                .map(PropertyDocumentAssignmentResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * "Can this park actually generate a complete document for everyone living
     * here?" Measures each assigned document against the distinct deals in
     * force, and reports any configuration whose lease would come out missing a
     * paragraph, with the number of tenancies on it.
     *
     * <p>Empty gaps everywhere is the answer you want. This is the check that
     * catches a park configured differently from the others -- the case the
     * template-only checks cannot see, because they never look at a tenant.
     */
    @PreAuthorize("hasAuthority('property_document:view')")
    @GetMapping("/audit")
    public ResponseEntity<DocumentAudit> audit(@RequestParam("property") UUID propertyId) {
        return ResponseEntity.ok(documentAuditService.auditProperty(propertyId));
    }

    @PreAuthorize("hasAuthority('property_document:view')")
    @GetMapping("/{uuid}")
    public ResponseEntity<PropertyDocumentAssignmentResponse> getAssignment(@PathVariable UUID uuid) {
        return assignmentService.findById(uuid)
                .map(PropertyDocumentAssignmentResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 409 when the park already has a document for this kind of deal: "generate
    // the lease" has to resolve to exactly one, and the message names the one
    // already in place so the fix is obvious.
    @PreAuthorize("hasAuthority('property_document:assign')")
    @PostMapping
    public ResponseEntity<PropertyDocumentAssignmentResponse> assign(
            @Valid @RequestBody AssignDocumentRequest request) {

        return assignmentService.assign(request.propertyId(), request.documentTemplateId())
                .map(PropertyDocumentAssignmentResponse::from)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('property_document:assign')")
    @PatchMapping("/{uuid}")
    public ResponseEntity<PropertyDocumentAssignmentResponse> patchAssignment(
            @PathVariable UUID uuid,
            @RequestBody Map<String, Object> request) {

        Map<String, Object> changes = new HashMap<>();
        PATCHABLE_COLUMNS.forEach((jsonField, column) -> {
            if (request.containsKey(jsonField)) {
                changes.put(column, request.get(jsonField));
            }
        });

        return assignmentService.patchAssignment(uuid, changes)
                .map(PropertyDocumentAssignmentResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Stops this park generating that document from now on. Anything already
    // generated keeps its own wording and is untouched.
    @PreAuthorize("hasAuthority('property_document:unassign')")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> unassign(@PathVariable UUID uuid) {
        return assignmentService.unassign(uuid)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", String.valueOf(e.getMessage())));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> conflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", String.valueOf(e.getMessage())));
    }
}