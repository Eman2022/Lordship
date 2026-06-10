package io.github.lordship.access.internal;

import io.github.lordship.access.Role;
import io.github.lordship.access.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PreAuthorize("hasAuthority('agent_roles:edit')")
    @PostMapping("/create")
    public ResponseEntity<RoleCreationResponse> createNewRole(@Valid @RequestBody RoleCreationRequest roleCreationRequest) {
        Role role = roleService.createRole(roleCreationRequest.roleName(), roleCreationRequest.roleDescription());
        RoleCreationResponse roleCreationResponse = RoleCreationResponse.from(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(roleCreationResponse);
    }
}
