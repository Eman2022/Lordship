package io.github.lordship.access.internal.rbac;

import io.github.lordship.access.RolePermission;
import io.github.lordship.access.RolePermissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/role-permissions")
public class RolePermissionController {

    public record RolePermissionRequest(
            @NotNull
            UUID roleId,

            @NotBlank
            String permissionName
    ) { }

    private final RolePermissionService rolePermissionService;

    public RolePermissionController(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @PreAuthorize("hasAuthority('role_permissions:edit')")
    @PostMapping("/append")
    public ResponseEntity<RolePermissionResponse> appendPermission(@Valid @RequestBody RolePermissionRequest rolePermissionRequest) {
        RolePermission rolePermission = rolePermissionService.appendPermissionByName(
                rolePermissionRequest.roleId(),
                rolePermissionRequest.permissionName());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(rolePermission.toResponse());
    }

    // the mirror image of /append - same body, so the client does not have to
    // look up the role_permission id just to take a permission away
    @PreAuthorize("hasAuthority('role_permissions:delete')")
    @PostMapping("/revoke")
    public ResponseEntity<Void> revokePermission(@Valid @RequestBody RolePermissionRequest rolePermissionRequest) {
        boolean revoked = rolePermissionService.revokePermissionByName(
                rolePermissionRequest.roleId(),
                rolePermissionRequest.permissionName());
        return revoked
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasAuthority('role_permissions:delete')")
    @DeleteMapping("/{rolePermissionId}")
    public ResponseEntity<Void> revokeById(@PathVariable UUID rolePermissionId) {
        boolean revoked = rolePermissionService.revokeById(rolePermissionId);
        return revoked
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasAuthority('role_permissions:view')")
    @GetMapping("/{rolePermissionId}")
    public ResponseEntity<RolePermissionResponse> findById(@PathVariable UUID rolePermissionId) {
        return rolePermissionService.findById(rolePermissionId)
                .map(RolePermission::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('role_permissions:view')")
    @GetMapping("/role/{roleId}")
    public ResponseEntity<List<RolePermissionResponse>> findPermissionsForRole(@PathVariable UUID roleId) {
        List<RolePermissionResponse> permissions = rolePermissionService.findPermissionsForRole(roleId)
                .stream()
                .map(RolePermission::toResponse)
                .toList();
        return ResponseEntity.ok(permissions);
    }

    @PreAuthorize("hasAuthority('role_permissions:view')")
    @GetMapping("/permission/{permissionId}")
    public ResponseEntity<List<RolePermissionResponse>> findRolesWithPermission(@PathVariable UUID permissionId) {
        List<RolePermissionResponse> roles = rolePermissionService.findRolesWithPermission(permissionId)
                .stream()
                .map(RolePermission::toResponse)
                .toList();
        return ResponseEntity.ok(roles);
    }
}