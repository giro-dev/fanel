package dev.agiro.fanel.assistant.domain;

import dev.agiro.fanel.assistant.api.AgentDefinition;
import dev.agiro.fanel.assistant.api.AgentRequest;
import dev.agiro.fanel.assistant.api.AgentResponse;

import java.util.Locale;
import java.util.UUID;

public interface Agent {
    AgentDefinition definition();

    AgentResponse execute(AgentRequest request, UUID householdId, UUID memberId, Locale locale);
}
