package dev.agiro.fanel.assistant.infra;

import dev.agiro.fanel.assistant.domain.ModelProfile;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "fanel.assistant")
public class AgentProperties {
    private Map<String, AgentConfig> agents = Collections.emptyMap();

    public Map<String, AgentConfig> getAgents() {
        return agents;
    }

    public void setAgents(Map<String, AgentConfig> agents) {
        this.agents = agents == null ? Collections.emptyMap() : agents;
    }

    public static class AgentConfig {
        private String nameKey;
        private String descriptionKey;
        private String promptKey;
        private boolean supportsMedia;
        private List<String> tools = List.of("AssistantTools");
        private ModelProfile model;

        public String getNameKey() { return nameKey; }
        public void setNameKey(String nameKey) { this.nameKey = nameKey; }

        public String getDescriptionKey() { return descriptionKey; }
        public void setDescriptionKey(String descriptionKey) { this.descriptionKey = descriptionKey; }

        public String getPromptKey() { return promptKey; }
        public void setPromptKey(String promptKey) { this.promptKey = promptKey; }

        public boolean isSupportsMedia() { return supportsMedia; }
        public void setSupportsMedia(boolean supportsMedia) { this.supportsMedia = supportsMedia; }

        public List<String> getTools() { return tools; }
        public void setTools(List<String> tools) { this.tools = tools; }

        public ModelProfile getModel() { return model; }
        public void setModel(ModelProfile model) { this.model = model; }
    }
}
