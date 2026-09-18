package dev.agiro.fanel.shopping.api;

import java.util.UUID;

public record ShoppingItemDto(UUID id, String name, Double quantity, String unit, String category, boolean recurring, boolean done) {
    public ShoppingItemDto(UUID id, String name, boolean done) {
        this(id, name, null, null, null, false, done);
    }
}
