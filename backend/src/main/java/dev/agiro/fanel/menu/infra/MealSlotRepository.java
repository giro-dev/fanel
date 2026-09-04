package dev.agiro.fanel.menu.infra;

import dev.agiro.fanel.menu.domain.MealSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealSlotRepository extends JpaRepository<MealSlot, UUID> {
    List<MealSlot> findAllByHouseholdIdAndDateBetweenOrderByDateAscMealAsc(
            UUID householdId, LocalDate from, LocalDate to);

    Optional<MealSlot> findByHouseholdIdAndDateAndMeal(UUID householdId, LocalDate date,
                                                       dev.agiro.fanel.menu.api.MealType meal);
}
