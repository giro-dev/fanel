package dev.agiro.fanel.shared.events;

import java.util.UUID;

/** Domain change notification used to push real-time updates to clients (SSE). */
public record HouseholdEvent(UUID householdId, String topic) {
}
