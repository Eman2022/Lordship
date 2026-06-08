package io.github.lordship.config;

import io.github.lordship.access.*;
import io.github.lordship.access.internal.AgentRegistrationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


@Component
public class ApplicationInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ApplicationInitializer.class);

    private final RoleService roleService;
    private final AgentService agentService;

    @Value("${lordship.root.password}")
    private String rootPassword;

    @Value("${lordship.root.email}")
    private String rootEmail;

    public ApplicationInitializer(RoleService roleService,
                                  AgentService agentService)
    {
        this.roleService = roleService;
        this.agentService = agentService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Running application initializer");
        printOutEnvironmentVars();
        ensureRolesExist();
        agentService.ensureRootAgentExists(rootEmail, rootPassword);
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

    private void printOutEnvironmentVars() {
        System.out.println("DB_URL: " + System.getenv("DB_URL"));
        System.out.println("DB_USERNAME: " + System.getenv("DB_USERNAME"));
        System.out.println("DB_PASSWORD: " + System.getenv("DB_PASSWORD"));
        System.out.println("ROOT_EMAIL: " + System.getenv("ROOT_EMAIL"));
        System.out.println("ROOT_PASSWORD: " + System.getenv("ROOT_PASSWORD"));

    }

}
