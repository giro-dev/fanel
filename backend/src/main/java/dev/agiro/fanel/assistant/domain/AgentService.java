package dev.agiro.fanel.assistant.domain;

import dev.agiro.fanel.assistant.api.AgentRequest;
import dev.agiro.fanel.assistant.api.AgentResponse;
import dev.agiro.fanel.shared.web.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
public class AgentService {
    private final AgentRegistry registry;

    public AgentService(AgentRegistry registry) {
        this.registry = registry;
    }

    public AgentResponse chat(UUID householdId, UUID memberId, AgentRequest request, Locale locale) {
        String agentId = request.agentId() != null && !request.agentId().isBlank() ? request.agentId() : "general";
        Agent agent = registry.get(agentId)
                .orElseThrow(() -> new EntityNotFoundException("Agent not found: " + agentId));
        return agent.execute(request, householdId, memberId, locale);
    }
}
