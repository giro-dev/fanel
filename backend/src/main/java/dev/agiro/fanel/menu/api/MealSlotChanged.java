package dev.agiro.fanel.menu.api;

import java.time.LocalDate;
import java.util.UUID;

public record MealSlotChanged(UUID householdId, LocalDate date, MealType meal) {
}
