package dev.agiro.fanel.recipes.infra;

import dev.agiro.fanel.recipes.domain.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, UUID> {
    List<Recipe> findAllByHouseholdIdOrderByName(UUID householdId);
}
