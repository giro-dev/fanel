package dev.agiro.fanel.calendar.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface CalendarApi {
    List<CalendarEventDto> list(UUID householdId, LocalDate from, LocalDate to);
    List<CalendarEventDto> listAll(UUID householdId);
    CalendarEventDto create(UUID householdId, String title, LocalDate date, LocalTime time, UUID addedBy);
    void delete(UUID householdId, UUID eventId);
}
