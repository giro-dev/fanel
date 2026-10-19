package dev.agiro.fanel.shopping.api;

import java.util.UUID;

/** {@code name}/{@code category} are localized to the caller's language from the bilingual catalog. */
public record ShoppingItemDto(UUID id, String name, Double quantity, String unit, String category, boolean recurring,
                              boolean done, UUID productId, UUID categoryId) {
    public ShoppingItemDto(UUID id, String name, boolean done) {
        this(id, name, null, null, null, false, done, null, null);
    }
}
