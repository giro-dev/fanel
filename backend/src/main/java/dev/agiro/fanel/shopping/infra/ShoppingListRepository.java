package dev.agiro.fanel.shopping.infra;

import dev.agiro.fanel.shopping.domain.ShoppingList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, UUID> {
    List<ShoppingList> findAllByHouseholdId(UUID householdId);
    Optional<ShoppingList> findFirstByHouseholdIdOrderByName(UUID householdId);
}
