package io.github.lordship.propertyassignments.internal;

import io.github.lordship.audit.AuditContext;
import io.github.lordship.propertyassignments.PropertyAssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ResponseEntity<PropertyAssignmentResponse> assignPropertyToAgent(@Valid @RequestBody PropertyAssignmentRequest req, AuditContext auditContext) {
        PropertyAssignmentResponse response = PropertyAssignmentResponse.from(
            propertyAssignmentService.assignAgentToProperty(req.agentId(), req.propertyId(), auditContext.getActingUserId())
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('assignments:remove')")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> removeAssignment(@PathVariable UUID uuid) {
        return propertyAssignmentService.removeAssignment(uuid) ?
                ResponseEntity.noContent().build() :
                ResponseEntity.notFound().build();
    }
}
