package dev.agiro.fanel.household.api;

import java.time.Instant;
import java.util.UUID;

public record HouseholdDto(UUID id, String name, String locale, String timezone, Instant createdAt) {
}
