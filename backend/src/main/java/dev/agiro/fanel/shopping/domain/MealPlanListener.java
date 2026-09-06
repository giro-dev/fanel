package dev.agiro.fanel.shopping.domain;

import dev.agiro.fanel.menu.api.MealPlanned;
import dev.agiro.fanel.recipes.api.IngredientDto;
import dev.agiro.fanel.recipes.api.RecipeDto;
import dev.agiro.fanel.recipes.api.RecipesApi;
import dev.agiro.fanel.shopping.api.ShoppingApi;
import dev.agiro.fanel.shopping.api.ShoppingListDto;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reacts to {@link MealPlanned} by adding the planned recipe's ingredients to the household's
 * default shopping list, so the menu feeds the shopping list without the two modules calling
 * each other directly (see ADR 0001).
 */
@Component
class MealPlanListener {
    private final RecipesApi recipes;
    private final ShoppingApi shopping;

    MealPlanListener(RecipesApi recipes, ShoppingApi shopping) {
        this.recipes = recipes;
        this.shopping = shopping;
    }

    @ApplicationModuleListener
    void on(MealPlanned event) {
        RecipeDto recipe = recipes.get(event.householdId(), event.recipeId());
        ShoppingListDto list = shopping.getDefaultList(event.householdId());
        Set<String> existing = list.items().stream()
                .map(item -> item.name().toLowerCase())
                .collect(Collectors.toSet());
        for (IngredientDto ingredient : recipe.ingredients()) {
            if (existing.add(ingredient.name().toLowerCase())) {
                shopping.addItem(event.householdId(), list.id(), ingredient.name());
            }
        }
    }
}
