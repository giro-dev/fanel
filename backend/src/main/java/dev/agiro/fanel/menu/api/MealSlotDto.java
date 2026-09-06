package dev.agiro.fanel.menu.api;

import java.util.UUID;

public record MealSlotDto(UUID id, int dayOfWeek, MealType mealType, String text, UUID recipeId) {
}
