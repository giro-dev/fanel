package dev.agiro.fanel.shopping.api;

import java.util.List;
import java.util.UUID;

public interface ShoppingApi {
    List<ShoppingListDto> listLists(UUID householdId);
    ShoppingListDto getDefaultList(UUID householdId);
    ShoppingItemDto addItem(UUID householdId, UUID listId, String name);
    ShoppingItemDto setDone(UUID householdId, UUID itemId, boolean done);
    void removeItem(UUID householdId, UUID itemId);
    int clearPurchased(UUID householdId, UUID listId);
}
