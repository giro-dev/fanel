package dev.agiro.fanel.shopping.api;

import java.util.UUID;

public record ShoppingItemDto(UUID id, String name, boolean done) {
}
