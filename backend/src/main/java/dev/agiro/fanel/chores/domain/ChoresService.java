package dev.agiro.fanel.chores.domain;

import dev.agiro.fanel.chores.api.ChoreDto;
import dev.agiro.fanel.chores.api.ChoresApi;
import dev.agiro.fanel.chores.api.RecurrenceFrequency;
import dev.agiro.fanel.chores.infra.ChoreRepository;
import dev.agiro.fanel.household.api.HouseholdApi;
import dev.agiro.fanel.shared.events.HouseholdEvent;
import dev.agiro.fanel.shared.security.CurrentAccess;
import dev.agiro.fanel.shared.web.EntityNotFoundException;
import dev.agiro.fanel.shared.web.ForbiddenException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ChoresService implements ChoresApi {
    public static final String TOPIC = "chores";

    private final ChoreRepository chores;
    private final ApplicationEventPublisher events;
    private final HouseholdApi household;
    private final CurrentAccess access;

    public ChoresService(ChoreRepository chores, ApplicationEventPublisher events,
                         HouseholdApi household, CurrentAccess access) {
        this.chores = chores;
        this.events = events;
        this.household = household;
        this.access = access;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChoreDto> list(UUID householdId) {
        return chores.findAllByHouseholdIdOrderByCreatedAtAsc(householdId).stream()
                .filter(chore -> canAccess(householdId, chore.getAssigneeId()))
                .map(ChoresService::toDto).toList();
    }

    @Override
    public ChoreDto create(UUID householdId, String title, UUID assigneeId, LocalDate dueDate,
                           RecurrenceFrequency recurrenceFreq, Integer recurrenceInterval,
                           List<UUID> rotationMemberIds) {
        requireCanManage(householdId, assigneeId);
        Chore saved = chores.save(new Chore(householdId, title, assigneeId, dueDate, recurrenceFreq,
                recurrenceInterval, rotationMemberIds));
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
        return toDto(saved);
    }

    @Override
    public ChoreDto update(UUID householdId, UUID choreId, String title, UUID assigneeId, Boolean done) {
        Chore chore = find(householdId, choreId);
        requireCanManage(householdId, chore.getAssigneeId());
        if (assigneeId != null) requireCanManage(householdId, assigneeId);
        if (title != null) chore.setTitle(title);
        if (assigneeId != null) chore.setAssigneeId(assigneeId);
        if (done != null) chore.setDone(done);
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
        return toDto(chores.save(chore));
    }

    @Override
    public ChoreDto updateRecurrence(UUID householdId, UUID choreId, LocalDate dueDate,
                                     RecurrenceFrequency recurrenceFreq, Integer recurrenceInterval,
                                     List<UUID> rotationMemberIds) {
        Chore chore = find(householdId, choreId);
        requireCanManage(householdId, chore.getAssigneeId());
        chore.setRecurrence(dueDate, recurrenceFreq, recurrenceInterval, rotationMemberIds);
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
        return toDto(chores.save(chore));
    }

    @Override
    public void delete(UUID householdId, UUID choreId) {
        Chore chore = find(householdId, choreId);
        requireCanManage(householdId, chore.getAssigneeId());
        chores.delete(chore);
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
    }

    /** Admins see/manage everything; unassigned chores are shared; others are scoped to oneself or one's children. */
    private boolean canAccess(UUID householdId, UUID assigneeId) {
        if (access.isAdmin() || assigneeId == null) return true;
        return access.memberId().map(id -> id.equals(assigneeId)
                || household.relatedChildIds(householdId, id).contains(assigneeId)).orElse(false);
    }

    private void requireCanManage(UUID householdId, UUID assigneeId) {
        if (!canAccess(householdId, assigneeId)) {
            throw new ForbiddenException("Not allowed to manage this chore");
        }
    }

    private Chore find(UUID householdId, UUID choreId) {
        return chores.findById(choreId)
                .filter(c -> c.getHouseholdId().equals(householdId))
                .orElseThrow(() -> new EntityNotFoundException("Chore not found: " + choreId));
    }

    private static ChoreDto toDto(Chore chore) {
        return new ChoreDto(chore.getId(), chore.getHouseholdId(), chore.getTitle(),
                chore.getAssigneeId(), chore.isDone(), chore.getCreatedAt(), chore.getDueDate(),
                chore.getRecurrenceFreq(), chore.getRecurrenceInterval(), chore.getRotationMemberIds());
    }
}
