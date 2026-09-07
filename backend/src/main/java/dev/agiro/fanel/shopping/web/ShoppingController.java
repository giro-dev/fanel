package dev.agiro.fanel.shopping.web;

import dev.agiro.fanel.shopping.api.ShoppingApi;
import dev.agiro.fanel.shopping.api.ShoppingItemDto;
import dev.agiro.fanel.shopping.api.ShoppingListDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/households/{householdId}/shopping")
public class ShoppingController {
    private final ShoppingApi shopping;

    public ShoppingController(ShoppingApi shopping) {
        this.shopping = shopping;
    }

    @GetMapping("/lists")
    public List<ShoppingListDto> listLists(@PathVariable UUID householdId) {
        return shopping.listLists(householdId);
    }

    @GetMapping("/lists/default")
    public ShoppingListDto defaultList(@PathVariable UUID householdId) {
        return shopping.getDefaultList(householdId);
    }

    @PostMapping("/lists/{listId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ShoppingItemDto addItem(@PathVariable UUID householdId, @PathVariable UUID listId,
                                   @Valid @RequestBody AddItem request) {
        return shopping.addItem(householdId, listId, request.name());
    }

    @PatchMapping("/items/{itemId}")
    public ShoppingItemDto setDone(@PathVariable UUID householdId, @PathVariable UUID itemId,
                                   @Valid @RequestBody SetDone request) {
        return shopping.setDone(householdId, itemId, request.done());
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable UUID householdId, @PathVariable UUID itemId) {
        shopping.removeItem(householdId, itemId);
    }

    @PostMapping("/lists/{listId}/clear-purchased")
    public Map<String, Integer> clearPurchased(@PathVariable UUID householdId, @PathVariable UUID listId) {
        return Map.of("removed", shopping.clearPurchased(householdId, listId));
    }

    public record AddItem(@NotBlank String name) {}
    public record SetDone(@NotNull Boolean done) {}
}
