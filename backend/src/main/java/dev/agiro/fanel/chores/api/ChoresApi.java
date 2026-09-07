package dev.agiro.fanel.chores.api;

import java.util.List;
import java.util.UUID;

public interface ChoresApi {
    List<ChoreDto> list(UUID householdId);
    ChoreDto create(UUID householdId, String title, UUID assigneeId);
    ChoreDto update(UUID householdId, UUID choreId, String title, UUID assigneeId, Boolean done);
    void delete(UUID householdId, UUID choreId);
}
