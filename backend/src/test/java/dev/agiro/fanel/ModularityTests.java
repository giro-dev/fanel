package dev.agiro.fanel;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {
    @Test
    void verifiesModularity() {
        ApplicationModules modules = ApplicationModules.of(FanelApplication.class);
        modules.verify();
        new Documenter(modules).writeDocumentation();
    }
}
