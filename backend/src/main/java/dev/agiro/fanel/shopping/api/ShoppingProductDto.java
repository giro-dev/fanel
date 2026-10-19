package dev.agiro.fanel.shopping.api;

import java.util.UUID;

/** {@code name}/{@code category} are localized to the caller's language; {@code shared} marks the read-only seed. */
public record ShoppingProductDto(UUID id, String name, String nameCa, String nameEs, UUID categoryId,
                                 String category, boolean translated, boolean shared) {
}
