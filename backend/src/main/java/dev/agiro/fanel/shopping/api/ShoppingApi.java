package dev.agiro.fanel.shopping.api;

import java.util.List;
import java.util.UUID;

public interface ShoppingApi {
    List<ShoppingItemDto> list(UUID householdId);
    ShoppingItemDto add(UUID householdId, String text);
    ShoppingItemDto update(UUID householdId, UUID id, String text, Boolean done);
    void delete(UUID householdId, UUID id);
    int clearDone(UUID householdId);
}
