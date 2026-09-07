package dev.agiro.fanel.menu.infra;

import dev.agiro.fanel.menu.domain.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealPlanRepository extends JpaRepository<MealPlan, UUID> {
    Optional<MealPlan> findByHouseholdIdAndIsoYearAndIsoWeek(UUID householdId, int isoYear, int isoWeek);
    List<MealPlan> findAllByHouseholdId(UUID householdId);
}
