package dev.agiro.fanel.menu.api;

import java.time.LocalDate;
import java.util.UUID;

public interface MenuApi {
    WeekMenuDto week(UUID householdId, int isoYear, int isoWeek);
    MealSlotDto set(UUID householdId, LocalDate date, MealType meal, String text);
}
