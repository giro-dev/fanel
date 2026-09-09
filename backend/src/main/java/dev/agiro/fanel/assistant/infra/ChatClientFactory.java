package dev.agiro.fanel.assistant.infra;

import dev.agiro.fanel.assistant.domain.ModelProfile;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientBuilderConfigurer;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ChatClientFactory {
    private final ChatClientBuilderConfigurer configurer;
    private final ObjectProvider<OpenAiChatModel> openAi;
    private final ObjectProvider<OllamaChatModel> ollama;
    private final ObjectProvider<AnthropicChatModel> anthropic;
    private final ChatMemory chatMemory;
    private final ObservationRegistry observationRegistry;

    public ChatClientFactory(ChatClientBuilderConfigurer configurer,
                               ObjectProvider<OpenAiChatModel> openAi,
                               ObjectProvider<OllamaChatModel> ollama,
                               ObjectProvider<AnthropicChatModel> anthropic,
                               ChatMemory chatMemory,
                               ObservationRegistry observationRegistry) {
        this.configurer = configurer;
        this.openAi = openAi;
        this.ollama = ollama;
        this.anthropic = anthropic;
        this.chatMemory = chatMemory;
        this.observationRegistry = observationRegistry;
    }

    public Optional<ChatClient> build(ModelProfile profile, String systemPrompt, Object tools) {
        ChatModel chatModel = resolve(profile);
        if (chatModel == null) {
            return Optional.empty();
        }

        ChatClient.Builder builder = ChatClient.builder(chatModel, observationRegistry, null, null, null);
        builder = configurer.configure(builder);

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            builder.defaultSystem(systemPrompt);
        }

        builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());

        if (tools != null) {
            builder.defaultTools(tools);
        }

        return Optional.of(builder.build());
    }

    private ChatModel resolve(ModelProfile profile) {
        if (profile == null || profile.provider() == null) {
            return null;
        }
        return switch (profile.provider().toLowerCase()) {
            case "openai" -> openAi.getIfAvailable();
            case "ollama" -> ollama.getIfAvailable();
            case "anthropic" -> anthropic.getIfAvailable();
            default -> null;
        };
    }
}
