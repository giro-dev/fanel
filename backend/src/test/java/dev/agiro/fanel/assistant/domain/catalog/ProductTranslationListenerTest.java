package dev.agiro.fanel.assistant.domain.catalog;

import dev.agiro.fanel.assistant.api.AgentResponse;
import dev.agiro.fanel.assistant.api.AssistantApi;
import dev.agiro.fanel.assistant.domain.Agent;
import dev.agiro.fanel.assistant.domain.AgentRegistry;
import dev.agiro.fanel.shopping.api.ShoppingApi;
import dev.agiro.fanel.shopping.api.ShoppingCategoryDto;
import dev.agiro.fanel.shopping.api.ShoppingProductCreated;
import dev.agiro.fanel.shopping.api.ShoppingProductDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductTranslationListenerTest {
    private final UUID household = UUID.randomUUID();
    private final UUID product = UUID.randomUUID();
    private final UUID pantry = UUID.randomUUID();
    private AssistantApi assistant;
    private ShoppingApi shopping;
    private ProductTranslationListener listener;

    @BeforeEach
    void setUp() {
        assistant = mock(AssistantApi.class);
        shopping = mock(ShoppingApi.class);
        AgentRegistry agents = mock(AgentRegistry.class);
        when(agents.get("product-translator")).thenReturn(Optional.of(mock(Agent.class)));
        listener = new ProductTranslationListener(assistant, agents, shopping);
        when(shopping.listCategories(household)).thenReturn(List.of(
                new ShoppingCategoryDto(pantry, "pantry", "Rebost", "Rebost", "Despensa", true)));
        when(shopping.listProducts(household)).thenReturn(List.of(
                new ShoppingProductDto(product, "Quinoa", "Quinoa", "Quinoa", null, null, false, false)));
    }

    @Test
    void storesTranslationAndCategoryFromTheAgent() {
        when(assistant.run(eq(household), eq("product-translator"), anyString(), any(Locale.class)))
                .thenReturn(Optional.of(new AgentResponse("product-translator", "c1",
                        "```json\n{\"ca\":\"Fesols\",\"es\":\"Alubias\",\"categoryId\":\"" + pantry + "\"}\n```", List.of(), List.of())));

        listener.on(new ShoppingProductCreated(household, product, "Fesols", "ca"));

        verify(shopping).updateProduct(household, product, "Fesols", "Alubias", pantry);
    }

    @Test
    void keepsTheProductUntouchedWithoutAnActiveAgent() {
        when(assistant.run(any(), any(), any(), any())).thenReturn(Optional.empty());

        listener.on(new ShoppingProductCreated(household, product, "Fesols", "ca"));

        verify(shopping, never()).updateProduct(any(), any(), any(), any(), any());
    }
}
