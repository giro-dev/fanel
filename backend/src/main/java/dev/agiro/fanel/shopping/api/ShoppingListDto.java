package dev.agiro.fanel.shopping.api;

import java.util.List;
import java.util.UUID;

public record ShoppingListDto(UUID id, UUID householdId, String name, List<ShoppingItemDto> items) {
}
