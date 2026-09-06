package dev.agiro.fanel.calendar.domain;

import dev.agiro.fanel.calendar.api.CalendarApi;
import dev.agiro.fanel.calendar.api.CalendarEventDto;
import dev.agiro.fanel.calendar.infra.CalendarEventRepository;
import dev.agiro.fanel.shared.events.HouseholdEvent;
import dev.agiro.fanel.shared.web.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CalendarService implements CalendarApi {
    public static final String TOPIC = "calendar";

    private final CalendarEventRepository events;
    private final ApplicationEventPublisher publisher;

    public CalendarService(CalendarEventRepository events, ApplicationEventPublisher publisher) {
        this.events = events;
        this.publisher = publisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarEventDto> list(UUID householdId, LocalDate from, LocalDate to) {
        List<CalendarEvent> found = (from != null && to != null)
                ? events.findAllByHouseholdIdAndDateBetweenOrderByDateAsc(householdId, from, to)
                : events.findAllByHouseholdIdOrderByDateAsc(householdId);
        return found.stream().map(CalendarService::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarEventDto> listAll(UUID householdId) {
        return list(householdId, null, null);
    }

    @Override
    public CalendarEventDto create(UUID householdId, String title, LocalDate date, LocalTime time, UUID addedBy) {
        CalendarEvent saved = events.save(new CalendarEvent(householdId, title, date, time, addedBy));
        publisher.publishEvent(new HouseholdEvent(householdId, TOPIC));
        return toDto(saved);
    }

    @Override
    public void delete(UUID householdId, UUID eventId) {
        CalendarEvent event = events.findById(eventId)
                .filter(e -> e.getHouseholdId().equals(householdId))
                .orElseThrow(() -> new EntityNotFoundException("Calendar event not found: " + eventId));
        events.delete(event);
        publisher.publishEvent(new HouseholdEvent(householdId, TOPIC));
    }

    private static CalendarEventDto toDto(CalendarEvent event) {
        return new CalendarEventDto(event.getId(), event.getHouseholdId(), event.getTitle(),
                event.getDate(), event.getTime(), event.getAddedBy());
    }
}
