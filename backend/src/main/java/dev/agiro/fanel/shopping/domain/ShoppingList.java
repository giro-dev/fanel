package dev.agiro.fanel.shopping.domain;

import dev.agiro.fanel.shared.domain.UuidEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shopping_list")
public class ShoppingList extends UuidEntity {
    @Column(name = "household_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID householdId;
    private String name;

    @OneToMany(mappedBy = "list", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<ShoppingItem> items = new ArrayList<>();

    protected ShoppingList() {}

    public ShoppingList(UUID householdId, String name) {
        this.householdId = householdId;
        this.name = name;
    }

    public UUID getHouseholdId() { return householdId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<ShoppingItem> getItems() { return items; }
}
