package dev.agiro.fanel.shopping.api;

import java.util.UUID;

/**
 * A household product was created from free text in {@code language} ("ca" or "es"); its name
 * in the other language is still a copy and it may lack a category.
 */
public record ShoppingProductCreated(UUID householdId, UUID productId, String name, String language) {
}
