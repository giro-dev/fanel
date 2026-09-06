package dev.agiro.fanel.calendar.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CalendarEventDto(UUID id, UUID householdId, String title, LocalDate date,
                               LocalTime time, UUID addedBy) {
}
