package io.github.lordship.config;

import io.github.lordship.access.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


@Component
public class ApplicationInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ApplicationInitializer.class);

    private final RoleService roleService;

    public ApplicationInitializer(RoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Running application initializer");
        ensureRolesExist();
        log.info("Application initializer completed");
    }

    private void ensureRolesExist() {
        int seeded = roleService.ensureDefaultRoles();
        if (seeded > 0) {
            log.warn("default role(s) were missing and have been re-seeded");
        } else {
            log.info("All default roles present.");
        }
    }
}
