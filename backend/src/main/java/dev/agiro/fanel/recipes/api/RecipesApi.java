package dev.agiro.fanel.recipes.api;

import java.util.List;
import java.util.UUID;

public interface RecipesApi {
    List<RecipeDto> list(UUID householdId);
    RecipeDto get(UUID householdId, UUID recipeId);
    RecipeDto create(UUID householdId, String name, int servings, String notes,
                     List<String> tags, List<IngredientDto> ingredients);
    void delete(UUID householdId, UUID recipeId);
}
