package dev.agiro.fanel.shopping.domain;

import dev.agiro.fanel.shared.events.HouseholdEvent;
import dev.agiro.fanel.shared.web.EntityNotFoundException;
import dev.agiro.fanel.shopping.api.ShoppingApi;
import dev.agiro.fanel.shopping.api.ShoppingItemDto;
import dev.agiro.fanel.shopping.api.ShoppingListDto;
import dev.agiro.fanel.shopping.infra.ShoppingItemRepository;
import dev.agiro.fanel.shopping.infra.ShoppingListRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ShoppingService implements ShoppingApi {
    public static final String TOPIC = "shopping";
    private static final String DEFAULT_LIST_NAME = "Compra";

    private final ShoppingListRepository lists;
    private final ShoppingItemRepository items;
    private final ApplicationEventPublisher events;

    public ShoppingService(ShoppingListRepository lists, ShoppingItemRepository items,
                           ApplicationEventPublisher events) {
        this.lists = lists;
        this.items = items;
        this.events = events;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShoppingListDto> listLists(UUID householdId) {
        return lists.findAllByHouseholdId(householdId).stream().map(ShoppingService::toDto).toList();
    }

    @Override
    public ShoppingListDto getDefaultList(UUID householdId) {
        ShoppingList list = lists.findFirstByHouseholdIdOrderByName(householdId)
                .orElseGet(() -> lists.save(new ShoppingList(householdId, DEFAULT_LIST_NAME)));
        return toDto(list);
    }

    @Override
    public ShoppingItemDto addItem(UUID householdId, UUID listId, String name) {
        ShoppingList list = findList(householdId, listId);
        ShoppingItem saved = items.save(new ShoppingItem(list, name));
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
        return toDto(saved);
    }

    @Override
    public ShoppingItemDto setDone(UUID householdId, UUID itemId, boolean done) {
        ShoppingItem item = findItem(householdId, itemId);
        item.setDone(done);
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
        return toDto(items.save(item));
    }

    @Override
    public void removeItem(UUID householdId, UUID itemId) {
        ShoppingItem item = findItem(householdId, itemId);
        items.delete(item);
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
    }

    @Override
    public int clearPurchased(UUID householdId, UUID listId) {
        ShoppingList list = findList(householdId, listId);
        List<ShoppingItem> purchased = list.getItems().stream().filter(ShoppingItem::isDone).toList();
        items.deleteAll(purchased);
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
        return purchased.size();
    }

    private ShoppingList findList(UUID householdId, UUID listId) {
        ShoppingList list = lists.findById(listId)
                .orElseThrow(() -> new EntityNotFoundException("Shopping list not found: " + listId));
        if (!list.getHouseholdId().equals(householdId)) {
            throw new EntityNotFoundException("Shopping list not found: " + listId);
        }
        return list;
    }

    private ShoppingItem findItem(UUID householdId, UUID itemId) {
        ShoppingItem item = items.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Shopping item not found: " + itemId));
        if (!item.getList().getHouseholdId().equals(householdId)) {
            throw new EntityNotFoundException("Shopping item not found: " + itemId);
        }
        return item;
    }

    private static ShoppingListDto toDto(ShoppingList list) {
        return new ShoppingListDto(list.getId(), list.getHouseholdId(), list.getName(),
                list.getItems().stream().map(ShoppingService::toDto).toList());
    }

    private static ShoppingItemDto toDto(ShoppingItem item) {
        return new ShoppingItemDto(item.getId(), item.getName(), item.isDone());
    }
}
