package dev.agiro.fanel.assistant.api;

import java.util.List;

public record AgentRequest(String agentId, String conversationId, String message, List<Attachment> attachments) {
}
