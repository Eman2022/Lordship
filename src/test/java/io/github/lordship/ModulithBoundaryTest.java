package io.github.lordship;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

public class ModulithBoundaryTest {

    @Test
    void modulesRespectBoundaries() {
        ApplicationModules.of(LordshipApplication.class).verify();
    }
}
