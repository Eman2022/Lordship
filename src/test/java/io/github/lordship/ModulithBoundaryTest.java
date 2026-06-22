package io.github.lordship;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.test.ApplicationModuleTest;



public class ModulithBoundaryTest {

    @Test
    void modulesRespectBoundaries() {
        ApplicationModules.of(LordshipApplication.class)
                .verify();
    }
}
