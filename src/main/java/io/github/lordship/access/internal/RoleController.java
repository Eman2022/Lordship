package io.github.lordship.access.internal;

import io.github.lordship.access.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

//    @PostMapping("/create")
//    public ResponseEntity<RoleCreationResponse> createNewRole()
}
