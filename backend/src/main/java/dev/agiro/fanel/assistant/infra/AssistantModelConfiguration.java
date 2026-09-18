package dev.agiro.fanel.assistant.infra;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssistantModelConfiguration {

    @Bean
    @ConditionalOnMissingBean(OpenAiChatModel.class)
    @ConditionalOnExpression("'${spring.ai.openai.api-key:}' != ''")
    public OpenAiChatModel openAiChatModel(@Value("${spring.ai.openai.api-key}") String apiKey,
                                            @Value("${spring.ai.openai.chat.base-url:https://api.openai.com/v1}") String baseUrl,
                                            @Value("${spring.ai.openai.chat.model:gpt-4o}") String model) {
        return OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .model(model)
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(AnthropicChatModel.class)
    @ConditionalOnExpression("'${spring.ai.anthropic.api-key:}' != ''")
    public AnthropicChatModel anthropicChatModel(@Value("${spring.ai.anthropic.api-key}") String apiKey,
                                                  @Value("${spring.ai.anthropic.chat.base-url:https://api.anthropic.com}") String baseUrl,
                                                  @Value("${spring.ai.anthropic.chat.model:claude-3-5-sonnet-20241022}") String model) {
        return AnthropicChatModel.builder()
                .options(AnthropicChatOptions.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .model(model)
                        .build())
                .build();
    }
}
