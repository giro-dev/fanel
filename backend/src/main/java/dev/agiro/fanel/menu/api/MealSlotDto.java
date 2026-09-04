package dev.agiro.fanel.menu.api;

import java.time.LocalDate;
import java.util.UUID;

public record MealSlotDto(UUID id, UUID householdId, LocalDate date, MealType meal, String text) {
}
