package dev.agiro.fanel.shopping.infra;

import dev.agiro.fanel.shopping.domain.ShoppingCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ShoppingCategoryRepository extends JpaRepository<ShoppingCategory, UUID> {
    /** Shared seed plus the household's own categories. */
    @Query("select c from ShoppingCategory c where c.householdId is null or c.householdId = :householdId "
            + "order by c.sortOrder, c.nameCa")
    List<ShoppingCategory> findVisible(@Param("householdId") UUID householdId);
}
