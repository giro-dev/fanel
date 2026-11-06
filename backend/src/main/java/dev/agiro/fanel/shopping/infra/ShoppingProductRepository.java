package dev.agiro.fanel.shopping.infra;

import dev.agiro.fanel.shopping.domain.ShoppingProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ShoppingProductRepository extends JpaRepository<ShoppingProduct, UUID> {
    /** Shared seed plus the household's own products, household ones first. */
    @Query("select p from ShoppingProduct p left join fetch p.category "
            + "where p.householdId is null or p.householdId = :householdId "
            + "order by case when p.householdId is null then 1 else 0 end, p.nameCa")
    List<ShoppingProduct> findVisible(@Param("householdId") UUID householdId);
}
