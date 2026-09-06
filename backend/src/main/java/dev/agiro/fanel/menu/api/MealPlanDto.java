package dev.agiro.fanel.menu.api;

import java.util.List;
import java.util.UUID;

public record MealPlanDto(UUID id, UUID householdId, int isoYear, int isoWeek, List<MealSlotDto> slots) {
}
