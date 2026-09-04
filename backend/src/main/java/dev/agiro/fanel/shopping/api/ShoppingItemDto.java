package dev.agiro.fanel.shopping.api;

import java.time.Instant;
import java.util.UUID;

public record ShoppingItemDto(UUID id, UUID householdId, String text, boolean done, Instant createdAt) {
}
