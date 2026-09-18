package dev.agiro.fanel.chores.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ChoresApi {
    List<ChoreDto> list(UUID householdId);

    ChoreDto create(UUID householdId, String title, UUID assigneeId, LocalDate dueDate,
                    RecurrenceFrequency recurrenceFreq, Integer recurrenceInterval, List<UUID> rotationMemberIds);

    default ChoreDto create(UUID householdId, String title, UUID assigneeId) {
        return create(householdId, title, assigneeId, null, null, null, null);
    }

    /** Partial update of title/assignee/done; leaves the recurrence configuration untouched. Setting
     * {@code done = true} on a recurring chore rolls it to its next occurrence (due date advanced, assignee
     * rotated) instead of leaving it marked done; see {@code Chore#complete()}. */
    ChoreDto update(UUID householdId, UUID choreId, String title, UUID assigneeId, Boolean done);

    /** Replaces the chore's recurrence configuration outright. Pass {@code recurrenceFreq = null} to make the
     * chore non-recurring again. */
    ChoreDto updateRecurrence(UUID householdId, UUID choreId, LocalDate dueDate, RecurrenceFrequency recurrenceFreq,
                             Integer recurrenceInterval, List<UUID> rotationMemberIds);

    void delete(UUID householdId, UUID choreId);
}
