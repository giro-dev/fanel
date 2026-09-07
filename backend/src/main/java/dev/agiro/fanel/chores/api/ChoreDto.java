package dev.agiro.fanel.chores.api;

import java.time.Instant;
import java.util.UUID;

public record ChoreDto(UUID id, UUID householdId, String title, UUID assigneeId, boolean done,
                       Instant createdAt) {
}
