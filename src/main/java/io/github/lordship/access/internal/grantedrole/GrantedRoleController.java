package io.github.lordship.access.internal.grantedrole;

import io.github.lordship.access.GrantedRole;
import io.github.lordship.access.GrantedRoleService;
import io.github.lordship.audit.ActingAgent;
import io.github.lordship.audit.AuditContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/granted-roles")
public class GrantedRoleController {

    public record GrantedRoleRequest(
            @NotNull
            UUID agentId,

            @NotBlank
            @Size(max = 60)
            String roleName
    ) { }


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

    // the mirror image of /grant - same body, so the client does not have to
    // look up the grant id just to take a role away
    @PreAuthorize("hasAuthority('agent_roles:delete')")
    @PostMapping("/revoke")
    public ResponseEntity<Void> revokeRole(@Valid @RequestBody GrantedRoleRequest grantedRoleRequest) {
        boolean revoked = grantedRoleService.revokeRoleByName(
                grantedRoleRequest.agentId(),
                grantedRoleRequest.roleName(),
                ActingAgent.resolve(auditContext));
        return revoked
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasAuthority('agent_roles:delete')")
    @DeleteMapping("/{grantId}")
    public ResponseEntity<Void> revokeGrant(@PathVariable UUID grantId) {
        boolean revoked = grantedRoleService.revokeGrant(
                grantId,
                ActingAgent.resolve(auditContext));
        return revoked
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasAuthority('agent_roles:view')")
    @GetMapping("/{grantId}")
    public ResponseEntity<GrantedRoleResponse> findById(@PathVariable UUID grantId) {
        return grantedRoleService.findById(grantId)
                .map(GrantedRole::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('agent_roles:view')")
    @GetMapping("/agent/{agentId}")
    public ResponseEntity<List<GrantedRoleResponse>> findRolesForAgent(@PathVariable UUID agentId) {
        List<GrantedRoleResponse> roles = grantedRoleService.findRolesForAgent(agentId)
                .stream()
                .map(GrantedRole::toResponse)
                .toList();
        return ResponseEntity.ok(roles);
    }

    @PreAuthorize("hasAuthority('agent_roles:view')")
    @GetMapping("/role/{roleId}")
    public ResponseEntity<List<GrantedRoleResponse>> findAgentsWithRole(@PathVariable UUID roleId) {
        List<GrantedRoleResponse> grants = grantedRoleService.findAgentsWithRole(roleId)
                .stream()
                .map(GrantedRole::toResponse)
                .toList();
        return ResponseEntity.ok(grants);
    }
}
