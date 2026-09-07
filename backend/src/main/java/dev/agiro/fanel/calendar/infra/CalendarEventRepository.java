package dev.agiro.fanel.calendar.infra;

import dev.agiro.fanel.calendar.domain.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {
    List<CalendarEvent> findAllByHouseholdIdOrderByDateAsc(UUID householdId);
    List<CalendarEvent> findAllByHouseholdIdAndDateBetweenOrderByDateAsc(UUID householdId, LocalDate from, LocalDate to);
}
