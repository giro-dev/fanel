package dev.agiro.fanel.shopping.api;

import java.util.UUID;

/** {@code name} is localized to the caller's language; {@code shared} marks the read-only seed. */
public record ShoppingCategoryDto(UUID id, String code, String name, String nameCa, String nameEs, boolean shared) {
}
