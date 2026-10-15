package dev.agiro.fanel.assistant.infra;

import dev.agiro.fanel.assistant.domain.ModelProfile;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;

import static org.assertj.core.api.Assertions.assertThat;

class ChatClientFactoryTest {

    @Test
    void openAiProfileBuildsOpenAiOptionsWithReasoningEffort() {
        ChatOptions options = ChatClientFactory.options(
                new ModelProfile("openai", "gpt-5.6-luna", 0.3, 512, "none")).build();

        assertThat(options).isInstanceOf(OpenAiChatOptions.class);
        OpenAiChatOptions openAi = (OpenAiChatOptions) options;
        assertThat(openAi.getModel()).isEqualTo("gpt-5.6-luna");
        assertThat(openAi.getTemperature()).isEqualTo(0.3);
        assertThat(openAi.getMaxTokens()).isEqualTo(512);
        assertThat(openAi.getReasoningEffort()).isEqualTo("none");
    }

    @Test
    void openAiProfileWithoutEffortLeavesItNull() {
        ChatOptions options = ChatClientFactory.options(
                new ModelProfile("openai", "gpt-4o", 0.3, null, null)).build();

        assertThat(options).isInstanceOf(OpenAiChatOptions.class);
        assertThat(((OpenAiChatOptions) options).getReasoningEffort()).isNull();
    }

    @Test
    void nonOpenAiProfileBuildsGenericOptions() {
        ChatOptions options = ChatClientFactory.options(
                new ModelProfile("ollama", "llama3.2", 0.4, null, "low")).build();

        assertThat(options).isNotInstanceOf(OpenAiChatOptions.class);
        assertThat(options.getModel()).isEqualTo("llama3.2");
    }
}
