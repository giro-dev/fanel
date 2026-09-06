package dev.agiro.fanel.chores.domain;

import dev.agiro.fanel.chores.api.ChoreDto;
import dev.agiro.fanel.chores.api.ChoresApi;
import dev.agiro.fanel.chores.infra.ChoreRepository;
import dev.agiro.fanel.shared.events.HouseholdEvent;
import dev.agiro.fanel.shared.web.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ChoresService implements ChoresApi {
    public static final String TOPIC = "chores";

    private final ChoreRepository chores;
    private final ApplicationEventPublisher events;

    public ChoresService(ChoreRepository chores, ApplicationEventPublisher events) {
        this.chores = chores;
        this.events = events;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChoreDto> list(UUID householdId) {
        return chores.findAllByHouseholdIdOrderByCreatedAtAsc(householdId).stream()
                .map(ChoresService::toDto).toList();
    }

    @Override
    public ChoreDto create(UUID householdId, String title, UUID assigneeId) {
        Chore saved = chores.save(new Chore(householdId, title, assigneeId));
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
        return toDto(saved);
    }

    @Override
    public ChoreDto update(UUID householdId, UUID choreId, String title, UUID assigneeId, Boolean done) {
        Chore chore = find(householdId, choreId);
        if (title != null) chore.setTitle(title);
        if (assigneeId != null) chore.setAssigneeId(assigneeId);
        if (done != null) chore.setDone(done);
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
        return toDto(chores.save(chore));
    }

    @Override
    public void delete(UUID householdId, UUID choreId) {
        chores.delete(find(householdId, choreId));
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
    }

    private Chore find(UUID householdId, UUID choreId) {
        return chores.findById(choreId)
                .filter(c -> c.getHouseholdId().equals(householdId))
                .orElseThrow(() -> new EntityNotFoundException("Chore not found: " + choreId));
    }

    private static ChoreDto toDto(Chore chore) {
        return new ChoreDto(chore.getId(), chore.getHouseholdId(), chore.getTitle(),
                chore.getAssigneeId(), chore.isDone(), chore.getCreatedAt());
    }
}
