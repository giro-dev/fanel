package dev.agiro.fanel.assistant.infra;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class PromptLoader {
    private final ResourceLoader resourceLoader;

    public PromptLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String load(String promptKey, Locale locale) {
        String key = promptKey == null ? "general" : promptKey;
        String[] paths = {
                "assistant/prompts/" + key + "_" + locale.getLanguage() + ".txt",
                "assistant/prompts/" + key + "_ca.txt",
                "assistant/prompts/" + key + ".txt"
        };
        for (String path : paths) {
            Resource resource = resourceLoader.getResource("classpath:" + path);
            if (resource.exists()) {
                try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                    return FileCopyUtils.copyToString(reader);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        return null;
    }
}
