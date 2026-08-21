package io.github.lordship.access.internal.rbac;

import io.github.lordship.access.GrantedRole;
import io.github.lordship.access.GrantedRoleService;
import io.github.lordship.audit.ActingAgent;
import io.github.lordship.audit.AuditContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/roles")
public class GrantedRoleController {

    private final GrantedRoleService grantedRoleService;
    private final AuditContext auditContext;

    public GrantedRoleController(GrantedRoleService grantedRoleService,
                                 AuditContext auditContext) {
        this.grantedRoleService = grantedRoleService;
        this.auditContext = auditContext;
    }

    @PreAuthorize("hasAuthority('agent_roles:edit')")
    @PostMapping("/grant")
    public ResponseEntity<GrantedRoleResponse> grantRole(@Valid @RequestBody GrantedRoleRequest grantedRoleRequest) {
        GrantedRole grantedRole = grantedRoleService.grantRoleByName(
                grantedRoleRequest.agentId(),
                grantedRoleRequest.roleName(),
                ActingAgent.resolve(auditContext));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(grantedRole.toResponse());
    }
}