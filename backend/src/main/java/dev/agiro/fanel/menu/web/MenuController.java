package dev.agiro.fanel.menu.web;

import dev.agiro.fanel.menu.api.MealSlotDto;
import dev.agiro.fanel.menu.api.MealType;
import dev.agiro.fanel.menu.api.MenuApi;
import dev.agiro.fanel.menu.api.WeekMenuDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/households/{householdId}/menu")
public class MenuController {
    private static final Pattern ISO_WEEK = Pattern.compile("^(\\d{4})-W(\\d{1,2})$");
    private final MenuApi menu;

    public MenuController(MenuApi menu) {
        this.menu = menu;
    }

    @GetMapping
    public WeekMenuDto week(@PathVariable UUID householdId,
                            @RequestParam(required = false) String week) {
        int year;
        int weekNumber;
        if (week == null || week.isBlank()) {
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            WeekFields fields = WeekFields.ISO;
            year = today.get(fields.weekBasedYear());
            weekNumber = today.get(fields.weekOfWeekBasedYear());
        } else {
            Matcher matcher = ISO_WEEK.matcher(week);
            if (!matcher.matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ISO week");
            }
            year = Integer.parseInt(matcher.group(1));
            weekNumber = Integer.parseInt(matcher.group(2));
        }
        return menu.week(householdId, year, weekNumber);
    }

    @PutMapping("/{date}/{meal}")
    public MealSlotDto set(@PathVariable UUID householdId, @PathVariable LocalDate date,
                           @PathVariable MealType meal, @Valid @RequestBody SetMeal request) {
        return menu.set(householdId, date, meal, request.text());
    }

    public record SetMeal(@Size(max = 500) String text) {}
}
