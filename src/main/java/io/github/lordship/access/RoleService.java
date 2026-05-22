package io.github.lordship.access;

import io.github.lordship.config.ApplicationInitializer;
import io.github.lordship.access.internal.RoleRepository;
import io.github.lordship.access.internal.RoleRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    public RoleService(RoleRepository roleRepository){
        this.roleRepository = roleRepository;
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
}
