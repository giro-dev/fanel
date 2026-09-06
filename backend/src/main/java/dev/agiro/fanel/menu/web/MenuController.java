package dev.agiro.fanel.menu.web;

import dev.agiro.fanel.menu.api.MealPlanDto;
import dev.agiro.fanel.menu.api.MealSlotDto;
import dev.agiro.fanel.menu.api.MealType;
import dev.agiro.fanel.menu.api.MenuApi;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/households/{householdId}/menu")
public class MenuController {
    private final MenuApi menu;

    public MenuController(MenuApi menu) {
        this.menu = menu;
    }

    @GetMapping
    public MealPlanDto getWeek(@PathVariable UUID householdId,
                               @RequestParam int year, @RequestParam int week) {
        return menu.getWeek(householdId, year, week);
    }

    @PutMapping("/slots")
    public MealSlotDto setSlot(@PathVariable UUID householdId,
                               @RequestParam int year, @RequestParam int week,
                               @Valid @RequestBody SetSlot request) {
        return menu.setSlot(householdId, year, week, request.dayOfWeek(), request.mealType(),
                request.text(), request.recipeId());
    }

    @DeleteMapping("/slots")
    public void clearSlot(@PathVariable UUID householdId,
                          @RequestParam int year, @RequestParam int week,
                          @RequestParam int dayOfWeek, @RequestParam MealType mealType) {
        menu.clearSlot(householdId, year, week, dayOfWeek, mealType);
    }

    public record SetSlot(@NotNull Integer dayOfWeek, @NotNull MealType mealType, String text, UUID recipeId) {
    }
}
