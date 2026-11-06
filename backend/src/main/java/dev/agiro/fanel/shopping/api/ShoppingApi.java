package dev.agiro.fanel.shopping.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShoppingApi {
    List<ShoppingListDto> listLists(UUID householdId);
    ShoppingListDto getList(UUID householdId, UUID listId);
    /** Read-only lookup of the default list (first by name); empty when the household has none. */
    Optional<ShoppingListDto> findDefaultList(UUID householdId);
    /** Returns the default list, creating one named "Compra" when the household has none yet. */
    ShoppingListDto getDefaultList(UUID householdId);
    ShoppingListDto createList(UUID householdId, String name);
    ShoppingListDto updateList(UUID householdId, UUID listId, String name);
    void deleteList(UUID householdId, UUID listId);

    /**
     * Adds an item, matching {@code name} against the bilingual catalog in Catalan or Spanish; if the
     * list already has that product, the existing item is reused (and marked pending) instead.
     */
    ShoppingItemDto addItem(UUID householdId, UUID listId, String name, Double quantity, String unit, String category, boolean recurring);
    default ShoppingItemDto addItem(UUID householdId, UUID listId, String name) {
        return addItem(householdId, listId, name, null, null, null, false);
    }
    ShoppingItemDto updateItem(UUID householdId, UUID itemId, String name, Double quantity, String unit, String category, Boolean recurring, Boolean done);
    ShoppingItemDto setDone(UUID householdId, UUID itemId, boolean done);
    void removeItem(UUID householdId, UUID itemId);
    int clearPurchased(UUID householdId, UUID listId);

    /** Read-only lookup of the catalog product a free-text name (Catalan or Spanish) refers to. */
    Optional<UUID> matchProduct(UUID householdId, String name);
    /** Shared bilingual categories plus the household's own, localized to the caller's language. */
    List<ShoppingCategoryDto> listCategories(UUID householdId);
    /** Shared bilingual products plus the household's own, localized to the caller's language. */
    List<ShoppingProductDto> listProducts(UUID householdId);
    /** Sets the Catalan/Spanish names and/or category of a household product; shared products are read-only. */
    ShoppingProductDto updateProduct(UUID householdId, UUID productId, String nameCa, String nameEs, UUID categoryId);
}
