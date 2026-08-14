package io.github.lordship.access;

import io.github.lordship.access.internal.rbac.GrantedRoleRepository;
import io.github.lordship.access.internal.rbac.GrantedRoleRow;
import io.github.lordship.access.internal.rbac.RoleRepository;
import io.github.lordship.access.internal.rbac.RoleRow;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


// assigns roles to agents

@Service
public class GrantedRoleService {

    private final GrantedRoleRepository grantedRoleRepository;
    private final RoleRepository roleRepository;

    public GrantedRoleService(GrantedRoleRepository grantedRoleRepository, RoleRepository roleRepository) {
        this.grantedRoleRepository = grantedRoleRepository;
        this.roleRepository = roleRepository;
    }

    public GrantedRole grantRole(UUID agentId, UUID roleId, UUID grantedBy) {
        return grantedRoleRepository.save(
                new GrantedRoleRow(agentId, roleId, grantedBy)
        ).toGrantedRole();
    }

    public GrantedRole grantRoleByName(UUID agentId, String roleName, UUID grantedBy) {
        RoleRow row = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
        return grantRole(agentId, row.uuid(), grantedBy);
    }

    public List<GrantedRole> findRolesForAgent(UUID agentId) {
        return grantedRoleRepository.findByAgentId(agentId)
                .stream()
                .map(GrantedRoleRow::toGrantedRole)
                .toList();
    }
}
