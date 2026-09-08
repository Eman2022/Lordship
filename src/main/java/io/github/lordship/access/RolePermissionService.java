package io.github.lordship.access;

import io.github.lordship.access.internal.permissions.PermissionRepository;
import io.github.lordship.access.internal.permissions.PermissionRow;
import io.github.lordship.access.internal.rbac.RolePermissionRepository;
import io.github.lordship.access.internal.rbac.RolePermissionRow;
import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.identity.AgentAuthorizationCache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final AuditService auditService;
    private final AgentAuthorizationCache authorizationCache;

    public RolePermissionService(RolePermissionRepository rolePermissionRepository,
                                 PermissionRepository permissionRepository,
                                 AuditService auditService,
                                 AgentAuthorizationCache authorizationCache) {
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
        this.auditService = auditService;
        this.authorizationCache = authorizationCache;
    }

    @Transactional
    public RolePermission appendPermission(UUID roleId, UUID permissionId) {
        Optional<RolePermissionRow> existing = rolePermissionRepository.findActiveGrant(roleId, permissionId);
        if (existing.isPresent()) {
            return existing.get().toRolePermission();
        }

        RolePermissionRow row = rolePermissionRepository.save(
                new RolePermissionRow(roleId, permissionId)
        );
        authorizationCache.invalidateAll();
        auditService.recordInsert("role_permission", row.uuid(), AuditMapper.toMap(row));
        return row.toRolePermission();
    }

    @Transactional
    public RolePermission appendPermissionByName(UUID roleId, String permissionName) {
        PermissionRow permissionRow = permissionRepository.findByName(permissionName)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permissionName));
        return appendPermission(roleId, permissionRow.uuid());
    }

    @Transactional
    public boolean revokeById(UUID rolePermissionId) {
        Optional<RolePermissionRow> before = rolePermissionRepository.findById(rolePermissionId);
        if (before.isEmpty()) return false;

        if (!rolePermissionRepository.revoke(rolePermissionId)) return false;

        authorizationCache.invalidateAll();
        auditService.recordDelete("role_permission", rolePermissionId, AuditMapper.toMap(before.get()));
        return true;
    }

    @Transactional
    public boolean revokePermission(UUID roleId, UUID permissionId) {
        return rolePermissionRepository.findActiveGrant(roleId, permissionId)
                .map(row -> revokeById(row.uuid()))
                .orElse(false);
    }

    @Transactional
    public boolean revokePermissionByName(UUID roleId, String permissionName) {
        PermissionRow permissionRow = permissionRepository.findByName(permissionName)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permissionName));
        return revokePermission(roleId, permissionRow.uuid());
    }

    // returns revoked rows too, so the caller can tell "never granted" from "revoked"
    public Optional<RolePermission> findById(UUID rolePermissionId) {
        return rolePermissionRepository.findById(rolePermissionId).map(RolePermissionRow::toRolePermission);
    }

    public List<RolePermission> findPermissionsForRole(UUID roleId) {
        return rolePermissionRepository.findByRoleId(roleId)
                .stream()
                .map(RolePermissionRow::toRolePermission)
                .toList();
    }

    // the reverse lookup: every role currently carrying a given permission
    public List<RolePermission> findRolesWithPermission(UUID permissionId) {
        return rolePermissionRepository.findByPermissionId(permissionId)
                .stream()
                .map(RolePermissionRow::toRolePermission)
                .toList();
    }
}