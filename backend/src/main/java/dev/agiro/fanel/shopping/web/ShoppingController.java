package dev.agiro.fanel.shopping.web;

import dev.agiro.fanel.shopping.api.ShoppingApi;
import dev.agiro.fanel.shopping.api.ShoppingItemDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/households/{householdId}/shopping")
public class ShoppingController {
    private final ShoppingApi shopping;

    public ShoppingController(ShoppingApi shopping) {
        this.shopping = shopping;
    }

    @GetMapping
    public List<ShoppingItemDto> list(@PathVariable UUID householdId) {
        return shopping.list(householdId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShoppingItemDto add(@PathVariable UUID householdId,
                               @Valid @RequestBody AddItem request) {
        return shopping.add(householdId, request.text());
    }

    @PatchMapping("/{id}")
    public ShoppingItemDto update(@PathVariable UUID householdId, @PathVariable UUID id,
                                  @Valid @RequestBody UpdateItem request) {
        return shopping.update(householdId, id, request.text(), request.done());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID householdId, @PathVariable UUID id) {
        shopping.delete(householdId, id);
    }

    @DeleteMapping("/done")
    public DeletedCount clearDone(@PathVariable UUID householdId) {
        return new DeletedCount(shopping.clearDone(householdId));
    }

    public record AddItem(@NotBlank @Size(max = 200) String text) {}
    public record UpdateItem(@Size(max = 200) String text, Boolean done) {}
    public record DeletedCount(int deleted) {}
}
