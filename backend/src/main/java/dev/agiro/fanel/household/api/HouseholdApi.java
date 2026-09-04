package dev.agiro.fanel.household.api;

import java.util.List;
import java.util.UUID;

public interface HouseholdApi {
    List<HouseholdDto> list();
    HouseholdDto get(UUID id);
}
