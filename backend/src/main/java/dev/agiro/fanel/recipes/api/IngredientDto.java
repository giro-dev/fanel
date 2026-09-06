package dev.agiro.fanel.recipes.api;

import java.util.UUID;

public record IngredientDto(UUID id, String name, Double quantity, String unit, String category) {
}
