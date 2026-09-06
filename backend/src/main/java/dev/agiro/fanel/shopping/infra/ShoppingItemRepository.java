package dev.agiro.fanel.shopping.infra;

import dev.agiro.fanel.shopping.domain.ShoppingItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, UUID> {
}
