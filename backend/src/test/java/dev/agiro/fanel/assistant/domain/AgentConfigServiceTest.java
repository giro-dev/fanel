package dev.agiro.fanel.assistant.domain;

import dev.agiro.fanel.assistant.api.AgentConfigUpdate;
import dev.agiro.fanel.assistant.infra.AgentConfigRepository;
import dev.agiro.fanel.assistant.infra.AgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentConfigServiceTest {

    private AgentConfigRepository repository;
    private AgentConfigService service;

    private static ModelProfile defaults() {
        return new ModelProfile("openai", "gpt-4o", 0.3, null, null);
    }

    @BeforeEach
    void setUp() {
        repository = mock(AgentConfigRepository.class);
        when(repository.findById("menu-planner")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        AgentProperties properties = new AgentProperties();
        AgentProperties.AgentConfig config = new AgentProperties.AgentConfig();
        config.setModel(defaults());
        properties.setAgents(Map.of("menu-planner", config));
        service = new AgentConfigService(repository, properties);
    }

    @Test
    void effectiveProfileMergesReasoningEffortFromOverride() {
        AgentConfigEntity override = new AgentConfigEntity("menu-planner");
        override.setReasoningEffort("none");

        ModelProfile effective = service.effectiveProfile(defaults(), override);

        assertThat(effective.provider()).isEqualTo("openai");
        assertThat(effective.model()).isEqualTo("gpt-4o");
        assertThat(effective.reasoningEffort()).isEqualTo("none");
    }

    @Test
    void effectiveProfileKeepsDefaultReasoningEffortWhenOverrideNull() {
        AgentConfigEntity override = new AgentConfigEntity("menu-planner");
        ModelProfile withEffort = new ModelProfile("openai", "gpt-5.6-luna", 0.3, null, "low");

        assertThat(service.effectiveProfile(withEffort, override).reasoningEffort()).isEqualTo("low");
        assertThat(service.effectiveProfile(null, override).reasoningEffort()).isNull();
    }

    @Test
    void upsertRejectsUnknownReasoningEffort() {
        AgentConfigUpdate update = new AgentConfigUpdate(null, null, null, null, null, "turbo");

        assertThatThrownBy(() -> service.upsert("menu-planner", update, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown reasoning effort: turbo");
    }

    @Test
    void upsertClearsReasoningEffortOnBlankAndLowercasesIt() {
        AgentConfigEntity cleared = service.upsert("menu-planner",
                new AgentConfigUpdate(null, null, null, null, null, ""), "admin");
        assertThat(cleared.getReasoningEffort()).isNull();

        AgentConfigEntity upper = service.upsert("menu-planner",
                new AgentConfigUpdate(null, null, null, null, null, "NONE"), "admin");
        assertThat(upper.getReasoningEffort()).isEqualTo("none");
    }
}
