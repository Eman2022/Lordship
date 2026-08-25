package io.github.lordship.propertyassignments.internal;

import io.github.lordship.audit.AuditContext;
import io.github.lordship.identity.AgentPrincipal;
import io.github.lordship.identity.LordshipPrincipal;
import io.github.lordship.propertyassignments.PropertyAssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/property-assignments")
public class PropertyAssignmentController {

    private final PropertyAssignmentService propertyAssignmentService;

    public PropertyAssignmentController(PropertyAssignmentService propertyAssignmentService) {
        this.propertyAssignmentService = propertyAssignmentService;
    }

    @PreAuthorize("hasAuthority('assignments:assign')")
    @PostMapping("/assign")
    public ResponseEntity<PropertyAssignmentResponse> assignPropertyToAgent(@Valid @RequestBody PropertyAssignmentRequest req, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof AgentPrincipal principal)){
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        PropertyAssignmentResponse response = PropertyAssignmentResponse.from(
            propertyAssignmentService.assign(req.agentId(), req.propertyId(), principal.agentUuid())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('assignments:remove')")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> removeAssignment(@PathVariable UUID uuid) {
        return propertyAssignmentService.endAssignment(uuid) ?
                ResponseEntity.noContent().build() :
                ResponseEntity.notFound().build();
    }
}
