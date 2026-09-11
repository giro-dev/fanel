package dev.agiro.fanel.recipes.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecipeDto(UUID id, UUID householdId, String name, int servings, String notes,
                        String description, List<String> steps, List<String> tags,
                        String imageMimeType, String imageData,
                        List<IngredientDto> ingredients, Instant createdAt) {
}
