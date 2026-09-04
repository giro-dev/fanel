package dev.agiro.fanel.menu.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record WeekMenuDto(UUID householdId, int isoYear, int isoWeek, LocalDate from, LocalDate to,
                          List<MealSlotDto> slots) {
}
