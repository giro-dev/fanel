package dev.agiro.fanel.calendar.web;

import dev.agiro.fanel.calendar.api.CalendarApi;
import dev.agiro.fanel.calendar.api.CalendarEventDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/households/{householdId}/calendar")
public class CalendarController {
    private final CalendarApi calendar;

    public CalendarController(CalendarApi calendar) {
        this.calendar = calendar;
    }

    @GetMapping
    public List<CalendarEventDto> list(@PathVariable UUID householdId,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return calendar.list(householdId, from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CalendarEventDto create(@PathVariable UUID householdId, @Valid @RequestBody CreateEvent request) {
        return calendar.create(householdId, request.title(), request.date(), request.time(), request.addedBy());
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID householdId, @PathVariable UUID eventId) {
        calendar.delete(householdId, eventId);
    }

    public record CreateEvent(@NotBlank String title, @NotNull LocalDate date, LocalTime time, UUID addedBy) {
    }
}
