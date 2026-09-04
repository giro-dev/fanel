package dev.agiro.fanel.household.api;

import dev.agiro.fanel.household.domain.Household;

import java.time.Instant;
import java.util.UUID;

public record HouseholdDto(UUID id, String name, String locale, String timezone, Instant createdAt) {
    public static HouseholdDto from(Household household) {
        return new HouseholdDto(household.getId(), household.getName(), household.getLocale(),
                household.getTimezone(), household.getCreatedAt());
    }
}
