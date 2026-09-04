package dev.agiro.fanel.menu.domain;

import dev.agiro.fanel.household.api.HouseholdApi;
import dev.agiro.fanel.menu.api.MealSlotChanged;
import dev.agiro.fanel.menu.api.MealSlotDto;
import dev.agiro.fanel.menu.api.MealType;
import dev.agiro.fanel.menu.api.MenuApi;
import dev.agiro.fanel.menu.api.WeekMenuDto;
import dev.agiro.fanel.menu.infra.MealSlotRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MenuService implements MenuApi {
    private final HouseholdApi households;
    private final MealSlotRepository mealSlots;
    private final ApplicationEventPublisher events;

    public MenuService(HouseholdApi households, MealSlotRepository mealSlots,
                       ApplicationEventPublisher events) {
        this.households = households;
        this.mealSlots = mealSlots;
        this.events = events;
    }

    @Override
    @Transactional(readOnly = true)
    public WeekMenuDto week(UUID householdId, int isoYear, int isoWeek) {
        households.get(householdId);
        LocalDate from = weekStart(isoYear, isoWeek);
        LocalDate to = from.plusDays(6);
        List<MealSlotDto> slots = mealSlots
                .findAllByHouseholdIdAndDateBetweenOrderByDateAscMealAsc(householdId, from, to)
                .stream().map(MenuService::toDto).toList();
        return new WeekMenuDto(householdId, isoYear, isoWeek, from, to, slots);
    }

    @Override
    public MealSlotDto set(UUID householdId, LocalDate date, MealType meal, String text) {
        households.get(householdId);
        var existing = mealSlots.findByHouseholdIdAndDateAndMeal(householdId, date, meal);
        if (text == null || text.isBlank()) {
            existing.ifPresent(mealSlots::delete);
            events.publishEvent(new MealSlotChanged(householdId, date, meal));
            return new MealSlotDto(null, householdId, date, meal, "");
        }

        MealSlot slot = existing.orElseGet(() -> new MealSlot(householdId, date, meal, text));
        slot.updateText(text);
        MealSlot saved = mealSlots.save(slot);
        events.publishEvent(new MealSlotChanged(householdId, date, meal));
        return toDto(saved);
    }

    private static LocalDate weekStart(int isoYear, int isoWeek) {
        if (isoWeek < 1 || isoWeek > 53) {
            throw new IllegalArgumentException("ISO week must be between 1 and 53");
        }
        LocalDate from = LocalDate.of(isoYear, 1, 4)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, isoWeek)
                .with(DayOfWeek.MONDAY);
        if (from.get(IsoFields.WEEK_BASED_YEAR) != isoYear
                || from.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) != isoWeek) {
            throw new IllegalArgumentException("Invalid ISO week");
        }
        return from;
    }

    private static MealSlotDto toDto(MealSlot slot) {
        return new MealSlotDto(slot.getId(), slot.getHouseholdId(), slot.getDate(),
                slot.getMeal(), slot.getText());
    }
}
