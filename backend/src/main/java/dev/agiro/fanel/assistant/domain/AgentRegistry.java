package dev.agiro.fanel.assistant.domain;

import dev.agiro.fanel.assistant.api.AgentDefinition;
import dev.agiro.fanel.assistant.domain.agents.DefaultAgent;
import dev.agiro.fanel.assistant.domain.tools.AssistantTools;
import dev.agiro.fanel.assistant.infra.AgentProperties;
import dev.agiro.fanel.assistant.infra.ChatClientFactory;
import dev.agiro.fanel.assistant.infra.PromptLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AgentRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(AgentRegistry.class);

    private final Map<String, Agent> agents;

    private final AssistantTools assistantTools;

    public AgentRegistry(AgentProperties properties,
                         ChatClientFactory chatClientFactory,
                         PromptLoader promptLoader,
                         AssistantTools assistantTools) {
        this.assistantTools = assistantTools;
        LOG.info("Loading {} agent configurations", properties.getAgents().size());
        this.agents = properties.getAgents().entrySet().stream()
                .peek(e -> LOG.debug("Agent config '{}' -> model={}", e.getKey(), e.getValue().getModel()))
                .filter(e -> e.getValue().getModel() != null)
                .map(e -> toAgent(e.getKey(), e.getValue(), chatClientFactory, promptLoader))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .peek(a -> LOG.info("Registered agent '{}'", a.definition().id()))
                .collect(Collectors.toMap(a -> a.definition().id(), a -> a));
    }

    public Optional<Agent> get(String id) {
        return Optional.ofNullable(agents.get(id));
    }

    public List<AgentDefinition> list() {
        return agents.values().stream().map(Agent::definition).toList();
    }

    private Optional<Agent> toAgent(String id, AgentProperties.AgentConfig config,
                                    ChatClientFactory chatClientFactory, PromptLoader promptLoader) {
        Object tools = config.getTools().contains("AssistantTools") ? assistantTools : null;
        return chatClientFactory.build(config.getModel(), null, tools)
                .map(chatClient -> {
                    AgentDefinition definition = new AgentDefinition(id,
                            config.getNameKey() != null ? config.getNameKey() : "assistant." + id + ".name",
                            config.getDescriptionKey() != null ? config.getDescriptionKey() : "assistant." + id + ".description",
                            config.isSupportsMedia(),
                            config.getTools());
                    return new DefaultAgent(definition, chatClient, promptLoader);
                });
    }
}
