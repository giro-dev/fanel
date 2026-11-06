package dev.agiro.fanel.shopping.domain;

import dev.agiro.fanel.shopping.infra.ShoppingCategoryRepository;
import dev.agiro.fanel.shopping.infra.ShoppingProductRepository;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Resolves free-text names to the bilingual catalog, so "Llet" and "Leche" end up as the same
 * product. Unknown names become household products/categories with the typed text in both
 * languages until someone (or the translator agent) fills in the other one.
 */
@Component
public class ShoppingCatalog {
    private final ShoppingProductRepository products;
    private final ShoppingCategoryRepository categories;

    ShoppingCatalog(ShoppingProductRepository products, ShoppingCategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    public record Resolution(ShoppingProduct product, boolean created) {}

    public Optional<ShoppingProduct> findProduct(UUID householdId, String name, String language) {
        return find(products.findVisible(householdId), name, language,
                ShoppingProduct::getNameCa, ShoppingProduct::getNameEs);
    }

    public Resolution resolveProduct(UUID householdId, String name, String language) {
        Optional<ShoppingProduct> match = findProduct(householdId, name, language);
        if (match.isPresent()) return new Resolution(match.get(), false);
        String trimmed = name.trim();
        return new Resolution(products.save(new ShoppingProduct(householdId, trimmed, trimmed, null, false)), true);
    }

    public Optional<ShoppingCategory> resolveCategory(UUID householdId, String name, String language) {
        if (name == null || name.isBlank()) return Optional.empty();
        List<ShoppingCategory> visible = categories.findVisible(householdId);
        Optional<ShoppingCategory> match = visible.stream()
                .filter(c -> key(c.name(language)).equals(key(name)))
                .findFirst()
                .or(() -> visible.stream()
                        .filter(c -> key(c.getNameCa()).equals(key(name)) || key(c.getNameEs()).equals(key(name)))
                        .findFirst());
        if (match.isPresent()) return match;
        String trimmed = name.trim();
        return Optional.of(categories.save(new ShoppingCategory(householdId, trimmed, trimmed, 1000)));
    }

    public List<ShoppingCategory> categories(UUID householdId) {
        return categories.findVisible(householdId);
    }

    public List<ShoppingProduct> products(UUID householdId) {
        return products.findVisible(householdId);
    }

    public Optional<ShoppingCategory> category(UUID householdId, UUID categoryId) {
        return categories.findById(categoryId)
                .filter(c -> c.getHouseholdId() == null || c.getHouseholdId().equals(householdId));
    }

    public Optional<ShoppingProduct> product(UUID householdId, UUID productId) {
        return products.findById(productId)
                .filter(p -> p.getHouseholdId() == null || p.getHouseholdId().equals(householdId));
    }

    /** Exact match in the user's language, then in the other one, then tolerant to plurals. */
    private static <T> Optional<T> find(List<T> candidates, String name, String language,
                                        Function<T, String> ca, Function<T, String> es) {
        String wanted = key(name);
        if (wanted.isEmpty()) return Optional.empty();
        Function<T, String> primary = CatalogLanguage.ES.equals(language) ? es : ca;
        Function<T, String> secondary = CatalogLanguage.ES.equals(language) ? ca : es;
        for (Function<T, String> field : List.of(primary, secondary)) {
            for (T candidate : candidates) {
                if (key(field.apply(candidate)).equals(wanted)) return Optional.of(candidate);
            }
        }
        Set<String> wantedForms = forms(wanted);
        for (T candidate : candidates) {
            for (Function<T, String> field : List.of(primary, secondary)) {
                Set<String> candidateForms = forms(key(field.apply(candidate)));
                candidateForms.retainAll(wantedForms);
                if (!candidateForms.isEmpty()) return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    static String key(String value) {
        if (value == null) return "";
        String lower = value.toLowerCase(Locale.ROOT).replace("l·l", "ll").replace("l.l", "ll");
        String stripped = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return stripped.replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** Naive singular/plural variants for Catalan and Spanish ("pomes" ~ "poma", "limones" ~ "limon"). */
    private static Set<String> forms(String key) {
        Set<String> forms = new LinkedHashSet<>();
        forms.add(key);
        if (key.length() > 3 && key.endsWith("s")) {
            forms.add(key.substring(0, key.length() - 1));
            if (key.endsWith("es")) {
                forms.add(key.substring(0, key.length() - 2));
                forms.add(key.substring(0, key.length() - 2) + "a");
            }
        }
        return forms;
    }
}
