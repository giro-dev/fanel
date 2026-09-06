package dev.agiro.fanel.menu.domain;

import dev.agiro.fanel.menu.api.MealPlanDto;
import dev.agiro.fanel.menu.api.MealPlanned;
import dev.agiro.fanel.menu.api.MealSlotDto;
import dev.agiro.fanel.menu.api.MealType;
import dev.agiro.fanel.menu.api.MenuApi;
import dev.agiro.fanel.menu.infra.MealPlanRepository;
import dev.agiro.fanel.menu.infra.MealSlotRepository;
import dev.agiro.fanel.shared.events.HouseholdEvent;
import dev.agiro.fanel.shared.web.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MenuService implements MenuApi {
    public static final String TOPIC = "menu";

    private final MealPlanRepository plans;
    private final MealSlotRepository slots;
    private final ApplicationEventPublisher events;

    public MenuService(MealPlanRepository plans, MealSlotRepository slots, ApplicationEventPublisher events) {
        this.plans = plans;
        this.slots = slots;
        this.events = events;
    }

    @Override
    @Transactional(readOnly = true)
    public MealPlanDto getWeek(UUID householdId, int isoYear, int isoWeek) {
        return plans.findByHouseholdIdAndIsoYearAndIsoWeek(householdId, isoYear, isoWeek)
                .map(MenuService::toDto)
                .orElse(new MealPlanDto(null, householdId, isoYear, isoWeek, List.of()));
    }

    @Override
    public MealSlotDto setSlot(UUID householdId, int isoYear, int isoWeek, int dayOfWeek, MealType mealType,
                               String text, UUID recipeId) {
        validateDay(dayOfWeek);
        MealPlan plan = plans.findByHouseholdIdAndIsoYearAndIsoWeek(householdId, isoYear, isoWeek)
                .orElseGet(() -> plans.save(new MealPlan(householdId, isoYear, isoWeek)));
        MealSlot slot = slots.findByMealPlanIdAndDayOfWeekAndMealType(plan.getId(), dayOfWeek, mealType)
                .orElseGet(() -> new MealSlot(plan, dayOfWeek, mealType, null));
        slot.setText(text);
        slot.setRecipeId(recipeId);
        MealSlot saved = slots.save(slot);
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
        if (recipeId != null) {
            events.publishEvent(new MealPlanned(householdId, recipeId, isoYear, isoWeek, dayOfWeek, mealType));
        }
        return toDto(saved);
    }

    @Override
    public void clearSlot(UUID householdId, int isoYear, int isoWeek, int dayOfWeek, MealType mealType) {
        MealPlan plan = plans.findByHouseholdIdAndIsoYearAndIsoWeek(householdId, isoYear, isoWeek)
                .orElseThrow(() -> new EntityNotFoundException("Meal plan not found"));
        slots.findByMealPlanIdAndDayOfWeekAndMealType(plan.getId(), dayOfWeek, mealType)
                .ifPresent(slots::delete);
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MealPlanDto> listAll(UUID householdId) {
        return plans.findAllByHouseholdId(householdId).stream().map(MenuService::toDto).toList();
    }

    private static void validateDay(int dayOfWeek) {
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new IllegalArgumentException("dayOfWeek must be between 1 and 7");
        }
    }

    private static MealPlanDto toDto(MealPlan plan) {
        List<MealSlotDto> slotDtos = plan.getSlots().stream()
                .map(MenuService::toDto)
                .sorted(Comparator.comparing(MealSlotDto::dayOfWeek).thenComparing(s -> s.mealType().ordinal()))
                .toList();
        return new MealPlanDto(plan.getId(), plan.getHouseholdId(), plan.getIsoYear(), plan.getIsoWeek(), slotDtos);
    }

    private static MealSlotDto toDto(MealSlot slot) {
        return new MealSlotDto(slot.getId(), slot.getDayOfWeek(), slot.getMealType(), slot.getText(), slot.getRecipeId());
    }
}
