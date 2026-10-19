package dev.agiro.fanel.shopping.domain;

import dev.agiro.fanel.shopping.infra.ShoppingItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Links items created before the bilingual catalog to a product and merges the pending
 * duplicates that appeared when the same thing was typed in Catalan and in Spanish.
 */
@Component
class ShoppingCatalogBackfill implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ShoppingCatalogBackfill.class);

    private final ShoppingItemRepository items;
    private final ShoppingCatalog catalog;
    private final CatalogLanguage languages;

    ShoppingCatalogBackfill(ShoppingItemRepository items, ShoppingCatalog catalog, CatalogLanguage languages) {
        this.items = items;
        this.catalog = catalog;
        this.languages = languages;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<ShoppingItem> unlinked = items.findAllByProductIsNull();
        if (unlinked.isEmpty()) return;
        Map<UUID, String> householdLanguage = new HashMap<>();
        Set<ShoppingList> touched = new LinkedHashSet<>();
        for (ShoppingItem item : unlinked) {
            UUID householdId = item.getList().getHouseholdId();
            String language = householdLanguage.computeIfAbsent(householdId, languages::householdLanguage);
            ShoppingProduct product = catalog.resolveProduct(householdId, item.getName(), language).product();
            item.setProduct(product);
            if (item.getCategory() != null && !item.getCategory().isBlank()) {
                ShoppingCategory category = catalog.resolveCategory(householdId, item.getCategory(), language)
                        .orElse(null);
                item.setCategoryRef(category);
                if (category != null && !product.isShared() && product.getCategory() == null) {
                    product.setCategory(category);
                }
            }
            touched.add(item.getList());
        }
        int merged = 0;
        for (ShoppingList list : touched) {
            merged += mergeDuplicates(list);
        }
        log.info("Linked {} shopping items to the bilingual catalog, merged {} duplicates", unlinked.size(), merged);
    }

    private int mergeDuplicates(ShoppingList list) {
        Map<UUID, ShoppingItem> kept = new HashMap<>();
        List<ShoppingItem> duplicates = list.getItems().stream()
                .filter(item -> item.getProduct() != null)
                .sorted(Comparator.comparing(ShoppingItem::isDone).thenComparing(ShoppingItem::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .filter(item -> kept.putIfAbsent(item.getProduct().getId(), item) != null)
                .toList();
        for (ShoppingItem duplicate : duplicates) {
            ShoppingItem keeper = kept.get(duplicate.getProduct().getId());
            if (keeper.getQuantity() == null) keeper.setQuantity(duplicate.getQuantity());
            if (keeper.getUnit() == null) keeper.setUnit(duplicate.getUnit());
            if (keeper.getCategoryRef() == null) keeper.setCategoryRef(duplicate.getCategoryRef());
            if (duplicate.isRecurring()) keeper.setRecurring(true);
            list.getItems().remove(duplicate);
            items.delete(duplicate);
        }
        return duplicates.size();
    }
}
