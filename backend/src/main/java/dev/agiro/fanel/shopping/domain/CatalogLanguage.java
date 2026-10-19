package dev.agiro.fanel.shopping.domain;

import dev.agiro.fanel.household.api.HouseholdApi;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Picks the catalog language ({@code ca} or {@code es}) used to render product and category
 * names: the request's {@code Accept-Language} when it names a supported language, otherwise the
 * household locale, otherwise Catalan.
 */
@Component
public class CatalogLanguage {
    public static final String CA = "ca";
    public static final String ES = "es";
    private static final List<String> SUPPORTED = List.of(CA, ES);

    private final HouseholdApi households;

    CatalogLanguage(HouseholdApi households) {
        this.households = households;
    }

    public String current(UUID householdId) {
        String fromRequest = fromRequest();
        return fromRequest != null ? fromRequest : householdLanguage(householdId);
    }

    public String householdLanguage(UUID householdId) {
        try {
            var household = households.get(householdId);
            return normalize(household != null ? household.locale() : null);
        } catch (RuntimeException e) {
            return CA;
        }
    }

    public static String other(String language) {
        return ES.equals(language) ? CA : ES;
    }

    static String normalize(String tag) {
        if (tag == null || tag.isBlank()) return CA;
        return Locale.forLanguageTag(tag.replace('_', '-')).getLanguage().equals(ES) ? ES : CA;
    }

    private static String fromRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        String header = attributes.getRequest().getHeader("Accept-Language");
        if (header == null || header.isBlank()) return null;
        try {
            return Locale.lookupTag(Locale.LanguageRange.parse(header), SUPPORTED);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
