package dev.agiro.fanel.shopping.domain;

import dev.agiro.fanel.shared.events.HouseholdEvent;
import dev.agiro.fanel.shared.web.ConflictException;
import dev.agiro.fanel.shared.web.EntityNotFoundException;
import dev.agiro.fanel.shopping.api.ShoppingApi;
import dev.agiro.fanel.shopping.api.ShoppingCategoryDto;
import dev.agiro.fanel.shopping.api.ShoppingItemDto;
import dev.agiro.fanel.shopping.api.ShoppingListDto;
import dev.agiro.fanel.shopping.api.ShoppingProductCreated;
import dev.agiro.fanel.shopping.api.ShoppingProductDto;
import dev.agiro.fanel.shopping.infra.ShoppingItemRepository;
import dev.agiro.fanel.shopping.infra.ShoppingListRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ShoppingService implements ShoppingApi {
    public static final String TOPIC = "shopping";
    private static final String DEFAULT_LIST_NAME = "Compra";

    private final ShoppingListRepository lists;
    private final ShoppingItemRepository items;
    private final ShoppingCatalog catalog;
    private final CatalogLanguage languages;
    private final ApplicationEventPublisher events;

    public ShoppingService(ShoppingListRepository lists, ShoppingItemRepository items, ShoppingCatalog catalog,
                           CatalogLanguage languages, ApplicationEventPublisher events) {
        this.lists = lists;
        this.items = items;
        this.catalog = catalog;
        this.languages = languages;
        this.events = events;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShoppingListDto> listLists(UUID householdId) {
        return lists.findAllByHouseholdId(householdId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ShoppingListDto getList(UUID householdId, UUID listId) {
        return toDto(findList(householdId, listId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShoppingListDto> findDefaultList(UUID householdId) {
        return lists.findFirstByHouseholdIdOrderByName(householdId).map(this::toDto);
    }

    @Override
    public ShoppingListDto getDefaultList(UUID householdId) {
        return findDefaultList(householdId)
                .orElseGet(() -> toDto(lists.save(new ShoppingList(householdId, DEFAULT_LIST_NAME))));
    }

    @Override
    public ShoppingListDto createList(UUID householdId, String name) {
        ShoppingList list = lists.save(new ShoppingList(householdId, name));
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
        return toDto(list);
    }

    @Override
    public ShoppingListDto updateList(UUID householdId, UUID listId, String name) {
        ShoppingList list = findList(householdId, listId);
        list.setName(name);
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
        return toDto(lists.save(list));
    }

    @Override
    public void deleteList(UUID householdId, UUID listId) {
        ShoppingList list = findList(householdId, listId);
        lists.delete(list);
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
    }

    /**
     * Adds an item linked to the bilingual catalog. When the list already holds the same product
     * (typed in either language) that item is reused instead of creating a duplicate.
     */
    @Override
    public ShoppingItemDto addItem(UUID householdId, UUID listId, String name, Double quantity, String unit,
                                   String category, boolean recurring) {
        ShoppingList list = findList(householdId, listId);
        String language = languages.current(householdId);
        ShoppingProduct product = resolveProduct(householdId, name, language);
        ShoppingCategory categoryRef = catalog.resolveCategory(householdId, category, language).orElse(null);
        adoptCategory(product, categoryRef);
        Optional<ShoppingItem> existing = list.getItems().stream()
                .filter(item -> item.getProduct() != null && item.getProduct().getId().equals(product.getId()))
                .min(Comparator.comparing(ShoppingItem::isDone));
        ShoppingItem item;
        if (existing.isPresent()) {
            item = existing.get();
            item.setDone(false);
            if (quantity != null) item.setQuantity(quantity);
            if (unit != null && !unit.isBlank()) item.setUnit(unit);
            if (recurring) item.setRecurring(true);
        } else {
            item = new ShoppingItem(list, name.trim(), quantity, unit, category, recurring);
            item.setProduct(product);
            list.getItems().add(item);
        }
        if (categoryRef != null) {
            item.setCategoryRef(categoryRef);
            item.setCategory(categoryRef.name(language));
        }
        ShoppingItem saved = items.save(item);
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
        return itemDto(saved, language);
    }

    @Override
    public ShoppingItemDto updateItem(UUID householdId, UUID itemId, String name, Double quantity, String unit,
                                      String category, Boolean recurring, Boolean done) {
        ShoppingItem item = findItem(householdId, itemId);
        String language = languages.current(householdId);
        if (name != null && !name.isBlank() && !name.trim().equals(item.localizedName(language))) {
            item.setName(name.trim());
            item.setProduct(resolveProduct(householdId, name, language));
        }
        if (quantity != null) item.setQuantity(quantity);
        if (unit != null) item.setUnit(unit);
        if (category != null) {
            ShoppingCategory categoryRef = catalog.resolveCategory(householdId, category, language).orElse(null);
            item.setCategoryRef(categoryRef);
            item.setCategory(categoryRef != null ? categoryRef.name(language) : null);
            adoptCategory(item.getProduct(), categoryRef);
        }
        if (recurring != null) item.setRecurring(recurring);
        if (done != null) item.setDone(done);
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
        return itemDto(items.save(item), language);
    }

    @Override
    public ShoppingItemDto setDone(UUID householdId, UUID itemId, boolean done) {
        ShoppingItem item = findItem(householdId, itemId);
        item.setDone(done);
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
        return itemDto(items.save(item), languages.current(householdId));
    }

    @Override
    public void removeItem(UUID householdId, UUID itemId) {
        ShoppingItem item = findItem(householdId, itemId);
        item.getList().getItems().remove(item);
        items.delete(item);
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
    }

    @Override
    public int clearPurchased(UUID householdId, UUID listId) {
        ShoppingList list = findList(householdId, listId);
        List<ShoppingItem> purchased = list.getItems().stream().filter(ShoppingItem::isDone).toList();
        for (ShoppingItem item : purchased) {
            if (item.isRecurring()) {
                item.setDone(false);
                items.save(item);
            } else {
                list.getItems().remove(item);
                items.delete(item);
            }
        }
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
        return purchased.size();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> matchProduct(UUID householdId, String name) {
        return catalog.findProduct(householdId, name, languages.current(householdId)).map(ShoppingProduct::getId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShoppingCategoryDto> listCategories(UUID householdId) {
        String language = languages.current(householdId);
        return catalog.categories(householdId).stream().map(c -> categoryDto(c, language)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShoppingProductDto> listProducts(UUID householdId) {
        String language = languages.current(householdId);
        return catalog.products(householdId).stream().map(p -> productDto(p, language)).toList();
    }

    @Override
    public ShoppingProductDto updateProduct(UUID householdId, UUID productId, String nameCa, String nameEs,
                                            UUID categoryId) {
        ShoppingProduct product = catalog.product(householdId, productId)
                .orElseThrow(() -> new EntityNotFoundException("Shopping product not found: " + productId));
        if (product.isShared()) {
            throw new ConflictException("Shared catalog products are read-only: " + productId);
        }
        if (nameCa != null && !nameCa.isBlank()) product.setNameCa(nameCa.trim());
        if (nameEs != null && !nameEs.isBlank()) product.setNameEs(nameEs.trim());
        if ((nameCa != null && !nameCa.isBlank()) || (nameEs != null && !nameEs.isBlank())) {
            product.setTranslated(true);
        }
        if (categoryId != null) {
            product.setCategory(catalog.category(householdId, categoryId)
                    .orElseThrow(() -> new EntityNotFoundException("Shopping category not found: " + categoryId)));
        }
        events.publishEvent(new HouseholdEvent(householdId, TOPIC));
        return productDto(product, languages.current(householdId));
    }

    private ShoppingProduct resolveProduct(UUID householdId, String name, String language) {
        ShoppingCatalog.Resolution resolution = catalog.resolveProduct(householdId, name, language);
        if (resolution.created()) {
            events.publishEvent(new ShoppingProductCreated(householdId, resolution.product().getId(),
                    name.trim(), language));
        }
        return resolution.product();
    }

    /** A household product without category learns it from the first item that sets one. */
    private static void adoptCategory(ShoppingProduct product, ShoppingCategory category) {
        if (product != null && category != null && !product.isShared() && product.getCategory() == null) {
            product.setCategory(category);
        }
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

    private ShoppingListDto toDto(ShoppingList list) {
        String language = languages.current(list.getHouseholdId());
        return new ShoppingListDto(list.getId(), list.getHouseholdId(), list.getName(),
                list.getItems().stream().map(item -> itemDto(item, language)).toList());
    }

    private static ShoppingItemDto itemDto(ShoppingItem item, String language) {
        ShoppingCategory category = item.effectiveCategory();
        return new ShoppingItemDto(item.getId(), item.localizedName(language), item.getQuantity(), item.getUnit(),
                item.localizedCategory(language), item.isRecurring(), item.isDone(),
                item.getProduct() != null ? item.getProduct().getId() : null,
                category != null ? category.getId() : null);
    }

    private static ShoppingCategoryDto categoryDto(ShoppingCategory category, String language) {
        return new ShoppingCategoryDto(category.getId(), category.getCode(), category.name(language),
                category.getNameCa(), category.getNameEs(), category.getHouseholdId() == null);
    }

    private static ShoppingProductDto productDto(ShoppingProduct product, String language) {
        ShoppingCategory category = product.getCategory();
        return new ShoppingProductDto(product.getId(), product.name(language), product.getNameCa(),
                product.getNameEs(), category != null ? category.getId() : null,
                category != null ? category.name(language) : null, product.isTranslated(), product.isShared());
    }
}
