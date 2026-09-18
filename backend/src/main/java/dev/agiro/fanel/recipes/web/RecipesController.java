package dev.agiro.fanel.recipes.web;

import dev.agiro.fanel.recipes.api.IngredientDto;
import dev.agiro.fanel.recipes.api.RecipeDto;
import dev.agiro.fanel.recipes.api.RecipesApi;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/households/{householdId}/recipes")
public class RecipesController {
    private static final Logger log = LoggerFactory.getLogger(RecipesController.class);

    private final RecipesApi recipes;

    public RecipesController(RecipesApi recipes) {
        this.recipes = recipes;
    }

    @GetMapping
    public List<RecipeDto> list(@PathVariable UUID householdId) {
        return recipes.list(householdId);
    }

    @GetMapping("/{recipeId}")
    public RecipeDto get(@PathVariable UUID householdId, @PathVariable UUID recipeId) {
        return recipes.get(householdId, recipeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeDto create(@PathVariable UUID householdId, @Valid @RequestBody CreateRecipe request) {
        log.debug("POST /recipes for household {}: name={}, servings={}, ingredients={}, steps={}, hasImage={}",
                householdId, request.name(), request.servings(),
                request.ingredients() == null ? 0 : request.ingredients().size(),
                request.steps() == null ? 0 : request.steps().size(),
                request.imageData() != null && !request.imageData().isBlank());
        return recipes.create(householdId, request.name(), request.servings(), request.notes(),
                request.description(), request.steps(), request.tags(), request.ingredients(),
                request.imageMimeType(), request.imageData());
    }

    @DeleteMapping("/{recipeId}")
    public void delete(@PathVariable UUID householdId, @PathVariable UUID recipeId) {
        recipes.delete(householdId, recipeId);
    }

    public record CreateRecipe(@NotBlank String name, int servings, String notes, String description,
                               List<String> steps, List<String> tags, List<IngredientDto> ingredients,
                               String imageMimeType, String imageData) {
    }
}
