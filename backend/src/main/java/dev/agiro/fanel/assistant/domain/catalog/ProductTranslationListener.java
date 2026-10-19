package dev.agiro.fanel.assistant.domain.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.agiro.fanel.assistant.api.AgentResponse;
import dev.agiro.fanel.assistant.api.AssistantApi;
import dev.agiro.fanel.assistant.domain.AgentRegistry;
import dev.agiro.fanel.shopping.api.ShoppingApi;
import dev.agiro.fanel.shopping.api.ShoppingCategoryDto;
import dev.agiro.fanel.shopping.api.ShoppingProductCreated;
import dev.agiro.fanel.shopping.api.ShoppingProductDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * When a product typed in one language is not in the catalog, asks the {@code product-translator}
 * agent for its name in the other language and its category. Without an active agent the
 * product simply keeps the typed name in both languages.
 */
@Component
class ProductTranslationListener {
    static final String AGENT = "product-translator";
    private static final Logger log = LoggerFactory.getLogger(ProductTranslationListener.class);

    private final AssistantApi assistant;
    private final AgentRegistry agents;
    private final ShoppingApi shopping;
    private final ObjectMapper mapper = new ObjectMapper();

    ProductTranslationListener(AssistantApi assistant, AgentRegistry agents, ShoppingApi shopping) {
        this.assistant = assistant;
        this.agents = agents;
        this.shopping = shopping;
    }

    @ApplicationModuleListener
    void on(ShoppingProductCreated event) {
        if (agents.get(AGENT).isEmpty()) return;
        List<ShoppingCategoryDto> categories = shopping.listCategories(event.householdId());
        String message = "Producte: " + event.name() + "\nIdioma: " + event.language() + "\nCategories:\n"
                + categories.stream()
                        .map(c -> "- " + c.id() + ": " + c.nameCa() + " / " + c.nameEs())
                        .collect(Collectors.joining("\n"));
        Optional<AgentResponse> response = assistant.run(event.householdId(), AGENT, message,
                Locale.forLanguageTag(event.language()));
        if (response.isEmpty() || response.get().text() == null || response.get().text().isBlank()) return;
        try {
            JsonNode json = mapper.readTree(stripFences(response.get().text()));
            String ca = text(json, "ca");
            String es = text(json, "es");
            if (ca == null || es == null) return;
            UUID categoryId = categoryId(json, categories, event);
            shopping.updateProduct(event.householdId(), event.productId(), ca, es, categoryId);
        } catch (Exception e) {
            log.warn("{} returned unusable output for '{}': {}", AGENT, event.name(), e.getMessage());
        }
    }

    /** Only fills the category when the product has none yet and the agent picked a known one. */
    private UUID categoryId(JsonNode json, List<ShoppingCategoryDto> categories, ShoppingProductCreated event) {
        String raw = text(json, "categoryId");
        if (raw == null) return null;
        boolean alreadyCategorized = shopping.listProducts(event.householdId()).stream()
                .filter(p -> p.id().equals(event.productId()))
                .map(ShoppingProductDto::categoryId)
                .anyMatch(id -> id != null);
        if (alreadyCategorized) return null;
        return categories.stream().map(ShoppingCategoryDto::id)
                .filter(id -> id.toString().equalsIgnoreCase(raw))
                .findFirst().orElse(null);
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) return null;
        return node.asText().trim();
    }

    private static String stripFences(String text) {
        String trimmed = text.trim();
        if (!trimmed.startsWith("```")) return trimmed;
        int start = trimmed.indexOf('\n');
        int end = trimmed.lastIndexOf("```");
        return start >= 0 && end > start ? trimmed.substring(start + 1, end).trim() : trimmed;
    }
}
