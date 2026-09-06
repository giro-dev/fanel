package dev.agiro.fanel.menu.api;

import java.util.List;
import java.util.UUID;

public interface MenuApi {
    MealPlanDto getWeek(UUID householdId, int isoYear, int isoWeek);
    MealSlotDto setSlot(UUID householdId, int isoYear, int isoWeek, int dayOfWeek, MealType mealType,
                        String text, UUID recipeId);
    void clearSlot(UUID householdId, int isoYear, int isoWeek, int dayOfWeek, MealType mealType);
    List<MealPlanDto> listAll(UUID householdId);
}
