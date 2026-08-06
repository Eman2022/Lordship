package io.github.lordship.access.internal;

import io.github.lordship.access.Role;
import io.github.lordship.access.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PreAuthorize("hasAuthority('agent_roles:create')")
    @PostMapping
    public ResponseEntity<RoleCreationResponse> createNewRole(@Valid @RequestBody RoleCreationRequest roleCreationRequest) {
        Role role = roleService.createRole(roleCreationRequest.roleName(), roleCreationRequest.roleDescription());
        RoleCreationResponse roleCreationResponse = RoleCreationResponse.from(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(roleCreationResponse);
    }

    @PreAuthorize("hasAuthority('agent_roles:view')")
    @GetMapping("/{uuid}")
    public ResponseEntity<Role> getRole(@PathVariable UUID uuid) {
        return roleService.findById(uuid).map(role -> ResponseEntity.ok().body(role))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('agent_roles:delete')")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID uuid) {
        return roleService.deleteRole(uuid) ?
                ResponseEntity.noContent().build() :
                ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasAuthority('agent_roles:edit')")
    @PatchMapping("/{uuid}")
    public ResponseEntity<Role> editRole(@PathVariable UUID uuid, @RequestBody Map<String, Object> request) {
        Map<String, Object> changes = new HashMap<>();

        if (request.containsKey("roleName")) changes.put("role_name", request.get("roleName"));
        if (request.containsKey("roleDescription")) changes.put("role_description", request.get("roleDescription"));

        return roleService.patchRole(uuid, changes)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
