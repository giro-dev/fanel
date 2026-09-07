package dev.agiro.fanel.menu.api;

import java.util.UUID;

/** Published when a meal slot is set with a recipe, so other modules (e.g. shopping) can react. */
public record MealPlanned(UUID householdId, UUID recipeId, int isoYear, int isoWeek,
                          int dayOfWeek, MealType mealType) {
}
