package dev.agiro.fanel.chores.web;

import dev.agiro.fanel.chores.api.ChoreDto;
import dev.agiro.fanel.chores.api.ChoresApi;
import dev.agiro.fanel.chores.api.RecurrenceFrequency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/households/{householdId}/chores")
public class ChoresController {
    private final ChoresApi chores;

    public ChoresController(ChoresApi chores) {
        this.chores = chores;
    }

    @GetMapping
    public List<ChoreDto> list(@PathVariable UUID householdId) {
        return chores.list(householdId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChoreDto create(@PathVariable UUID householdId, @Valid @RequestBody CreateChore request) {
        return chores.create(householdId, request.title(), request.assigneeId(), request.dueDate(),
                request.recurrenceFreq(), request.recurrenceInterval(), request.rotationMemberIds());
    }

    @PatchMapping("/{choreId}")
    public ChoreDto update(@PathVariable UUID householdId, @PathVariable UUID choreId,
                           @RequestBody UpdateChore request) {
        return chores.update(householdId, choreId, request.title(), request.assigneeId(), request.done());
    }

    @PutMapping("/{choreId}/recurrence")
    public ChoreDto updateRecurrence(@PathVariable UUID householdId, @PathVariable UUID choreId,
                                     @RequestBody UpdateRecurrence request) {
        return chores.updateRecurrence(householdId, choreId, request.dueDate(), request.recurrenceFreq(),
                request.recurrenceInterval(), request.rotationMemberIds());
    }

    @DeleteMapping("/{choreId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID householdId, @PathVariable UUID choreId) {
        chores.delete(householdId, choreId);
    }

    public record CreateChore(@NotBlank String title, UUID assigneeId, LocalDate dueDate,
                              RecurrenceFrequency recurrenceFreq, Integer recurrenceInterval,
                              List<UUID> rotationMemberIds) {}
    public record UpdateChore(String title, UUID assigneeId, Boolean done) {}
    public record UpdateRecurrence(LocalDate dueDate, RecurrenceFrequency recurrenceFreq, Integer recurrenceInterval,
                                   List<UUID> rotationMemberIds) {}
}
