package io.github.lordship.access;

import io.github.lordship.access.internal.rbac.RoleRepository;
import io.github.lordship.access.internal.rbac.RoleRow;
import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// We'll use this service to create roles and assign permissions to roles

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final AuditService auditService;

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    public RoleService(RoleRepository roleRepository,
                       AuditService auditService) {
        this.roleRepository = roleRepository;
        this.auditService = auditService;
    }

    // In access/RoleService.java
    @Transactional
    public int ensureDefaultRoles() {
        int count = 0;
        for (String roleName : List.of("Admin", "Office Staff", "Unassigned", "Property Manager")) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new RoleRow(roleName));
                log.warn("Re-seeded missing role: {}", roleName);
                count++;
            }
        }
        return count;
    }

    @Transactional
    public Role createRole(String roleName, String roleDescription) {
        RoleRow roleRow = roleRepository.save(new RoleRow(roleName, roleDescription));
        auditService.recordInsert("agent_role", roleRow.uuid(), AuditMapper.toMap(roleRow));
        return roleRow.toRole();
    }

    public Optional<Role> findById(UUID uuid) {
        return roleRepository.findById(uuid).map(RoleRow::toRole);
    }

    @Transactional
    public Optional<Role> patchRole(UUID roleId, Map<String, Object> changes){
        Optional<RoleRow> before = roleRepository.findById(roleId);

        if (before.isEmpty()) return Optional.empty();

        RoleRow beforeRow = before.get();
        if (changes.containsKey("role_name")) {
            Object roleName = changes.get("role_name");
            if (roleName instanceof String s && !s.isBlank()) {
                changes.put("role_name", s);
            } else {
                changes.put("role_name", null);
            }
        }

        if (changes.containsKey("role_description")) {
            Object roleDescription = changes.get("role_description");
            if (roleDescription instanceof String s && !s.isBlank()) {
                changes.put("role_description", s);
            } else {
                changes.put("role_description", null);
            }
        }

        Optional<RoleRow> after = roleRepository.patch(roleId, changes);
        if (after.isEmpty()) return Optional.empty();

        RoleRow afterRow = after.get();
        var diff = AuditMapper.diff(beforeRow, afterRow);
        if (!diff.before().isEmpty()) {
            auditService.recordUpdate("agent_role", roleId, diff.before(), diff.after());
        }
        return Optional.of(afterRow.toRole());
    }

    @Transactional
    public boolean deleteRole(UUID uuid) {
        return roleRepository.findById(uuid).map(roleRow -> {
            if (!roleRepository.softDelete(uuid)) {
                return false;
            }
            auditService.recordDelete("agent_role", uuid, AuditMapper.toMap(roleRow));
            return true;
        }).orElse(false);
    }
}
