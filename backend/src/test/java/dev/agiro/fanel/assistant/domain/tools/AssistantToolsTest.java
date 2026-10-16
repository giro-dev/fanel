package dev.agiro.fanel.assistant.domain.tools;

import dev.agiro.fanel.calendar.api.CalendarApi;
import dev.agiro.fanel.chores.api.ChoresApi;
import dev.agiro.fanel.household.api.HouseholdApi;
import dev.agiro.fanel.menu.api.MenuApi;
import dev.agiro.fanel.recipes.api.RecipesApi;
import dev.agiro.fanel.shopping.api.ShoppingApi;
import dev.agiro.fanel.shopping.api.ShoppingItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssistantToolsTest {

    private ShoppingApi shopping;
    private AssistantTools tools;

    @BeforeEach
    void setUp() {
        shopping = mock(ShoppingApi.class);
        tools = new AssistantTools(mock(RecipesApi.class), shopping, mock(MenuApi.class),
                mock(ChoresApi.class), mock(CalendarApi.class), mock(HouseholdApi.class));
    }

    @Test
    void updateShoppingItemDelegatesToUpdateItemWithoutAdding() {
        UUID householdId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        ShoppingItemDto updated = mock(ShoppingItemDto.class);
        when(shopping.updateItem(householdId, itemId, null, null, null, "Fruita", null, null))
                .thenReturn(updated);

        ShoppingItemDto result = tools.updateShoppingItem(householdId, itemId,
                null, null, null, "Fruita", null);

        assertThat(result).isSameAs(updated);
        verify(shopping).updateItem(householdId, itemId, null, null, null, "Fruita", null, null);
        verify(shopping, never()).addItem(any(), any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void removeShoppingItemDelegatesToRemoveItem() {
        UUID householdId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        tools.removeShoppingItem(householdId, itemId);

        verify(shopping).removeItem(householdId, itemId);
    }
}
