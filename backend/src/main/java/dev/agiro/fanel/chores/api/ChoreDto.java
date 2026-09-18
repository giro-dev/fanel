package dev.agiro.fanel.chores.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ChoreDto(UUID id, UUID householdId, String title, UUID assigneeId, boolean done,
                       Instant createdAt, LocalDate dueDate, RecurrenceFrequency recurrenceFreq,
                       Integer recurrenceInterval, List<UUID> rotationMemberIds) {
}
