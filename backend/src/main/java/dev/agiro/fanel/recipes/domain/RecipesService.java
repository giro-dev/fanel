package dev.agiro.fanel.recipes.domain;

import dev.agiro.fanel.recipes.api.IngredientDto;
import dev.agiro.fanel.recipes.api.RecipeDto;
import dev.agiro.fanel.recipes.api.RecipesApi;
import dev.agiro.fanel.recipes.infra.RecipeRepository;
import dev.agiro.fanel.shared.web.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RecipesService implements RecipesApi {
    private static final Logger log = LoggerFactory.getLogger(RecipesService.class);

    private final RecipeRepository recipes;

    public RecipesService(RecipeRepository recipes) {
        this.recipes = recipes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeDto> list(UUID householdId) {
        return recipes.findAllByHouseholdIdOrderByName(householdId).stream().map(RecipesService::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeDto get(UUID householdId, UUID recipeId) {
        return toDto(find(householdId, recipeId));
    }

    @Override
    public RecipeDto create(UUID householdId, String name, int servings, String notes, String description,
                            List<String> steps, List<String> tags, List<IngredientDto> ingredients,
                            String imageMimeType, String imageData) {
        log.debug("Creating recipe for household {}: name={}, servings={}, ingredients={}, steps={}, hasImage={}",
                householdId, name, servings,
                ingredients == null ? 0 : ingredients.size(),
                steps == null ? 0 : steps.size(),
                imageData != null && !imageData.isBlank());
        Recipe recipe = new Recipe(householdId, name, servings, notes, description, steps,
                tags, imageMimeType, imageData);
        if (ingredients != null) {
            for (IngredientDto ingredient : ingredients) {
                recipe.getIngredients().add(new RecipeIngredient(recipe, ingredient.name(),
                        ingredient.quantity(), ingredient.unit(), ingredient.category()));
            }
        }
        return toDto(recipes.save(recipe));
    }

    @Override
    public void delete(UUID householdId, UUID recipeId) {
        recipes.delete(find(householdId, recipeId));
    }

    private Recipe find(UUID householdId, UUID recipeId) {
        return recipes.findById(recipeId)
                .filter(r -> r.getHouseholdId().equals(householdId))
                .orElseThrow(() -> new EntityNotFoundException("Recipe not found: " + recipeId));
    }

    private static RecipeDto toDto(Recipe recipe) {
        List<IngredientDto> ingredients = recipe.getIngredients().stream()
                .map(i -> new IngredientDto(i.getId(), i.getName(), i.getQuantity(), i.getUnit(), i.getCategory()))
                .toList();
        // copy to detach from the Hibernate PersistentBag before serialization
        List<String> steps = recipe.getSteps() == null ? List.of() : new ArrayList<>(recipe.getSteps());
        List<String> tags = recipe.getTags() == null ? List.of() : new ArrayList<>(recipe.getTags());
        return new RecipeDto(recipe.getId(), recipe.getHouseholdId(), recipe.getName(), recipe.getServings(),
                recipe.getNotes(), recipe.getDescription(), steps, tags,
                recipe.getImageMimeType(), recipe.getImageData(), ingredients, recipe.getCreatedAt());
    }
}
