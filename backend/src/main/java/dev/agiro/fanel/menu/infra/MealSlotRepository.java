package dev.agiro.fanel.menu.infra;

import dev.agiro.fanel.menu.api.MealType;
import dev.agiro.fanel.menu.domain.MealSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MealSlotRepository extends JpaRepository<MealSlot, UUID> {
    Optional<MealSlot> findByMealPlanIdAndDayOfWeekAndMealType(UUID mealPlanId, int dayOfWeek, MealType mealType);
}
