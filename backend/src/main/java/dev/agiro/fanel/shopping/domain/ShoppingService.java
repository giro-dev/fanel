package dev.agiro.fanel.shopping.domain;

import dev.agiro.fanel.household.api.HouseholdApi;
import dev.agiro.fanel.shared.web.EntityNotFoundException;
import dev.agiro.fanel.shopping.api.ShoppingApi;
import dev.agiro.fanel.shopping.api.ShoppingItemDto;
import dev.agiro.fanel.shopping.api.ShoppingListChanged;
import dev.agiro.fanel.shopping.infra.ShoppingItemRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ShoppingService implements ShoppingApi {
    private final HouseholdApi households;
    private final ShoppingItemRepository items;
    private final ApplicationEventPublisher events;

    public ShoppingService(HouseholdApi households, ShoppingItemRepository items,
                           ApplicationEventPublisher events) {
        this.households = households;
        this.items = items;
        this.events = events;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShoppingItemDto> list(UUID householdId) {
        requireHousehold(householdId);
        return items.findAllByHouseholdIdOrderByDoneAscCreatedAtAsc(householdId)
                .stream().map(ShoppingService::toDto).toList();
    }

    @Override
    public ShoppingItemDto add(UUID householdId, String text) {
        requireHousehold(householdId);
        ShoppingItem item = items.save(new ShoppingItem(householdId, text));
        events.publishEvent(new ShoppingListChanged(householdId));
        return toDto(item);
    }

    @Override
    public ShoppingItemDto update(UUID householdId, UUID id, String text, Boolean done) {
        requireHousehold(householdId);
        ShoppingItem item = items.findByIdAndHouseholdId(id, householdId)
                .orElseThrow(() -> new EntityNotFoundException("Shopping item not found: " + id));
        if (text != null) item.updateText(text);
        if (done != null) item.setDone(done);
        ShoppingItem saved = items.save(item);
        events.publishEvent(new ShoppingListChanged(householdId));
        return toDto(saved);
    }

    @Override
    public void delete(UUID householdId, UUID id) {
        requireHousehold(householdId);
        ShoppingItem item = items.findByIdAndHouseholdId(id, householdId)
                .orElseThrow(() -> new EntityNotFoundException("Shopping item not found: " + id));
        items.delete(item);
        events.publishEvent(new ShoppingListChanged(householdId));
    }

    @Override
    public int clearDone(UUID householdId) {
        requireHousehold(householdId);
        int deleted = items.deleteAllByHouseholdIdAndDoneTrue(householdId);
        events.publishEvent(new ShoppingListChanged(householdId));
        return deleted;
    }

    private void requireHousehold(UUID householdId) {
        households.get(householdId);
    }

    private static ShoppingItemDto toDto(ShoppingItem item) {
        return new ShoppingItemDto(item.getId(), item.getHouseholdId(), item.getText(),
                item.isDone(), item.getCreatedAt());
    }
}
