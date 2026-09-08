package dev.agiro.fanel.shopping.domain;

import dev.agiro.fanel.shared.domain.UuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "shopping_item")
public class ShoppingItem extends UuidEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "list_id", nullable = false)
    private ShoppingList list;
    private String name;
    private Double quantity;
    private String unit;
    private String category;
    private boolean recurring;
    private boolean done;
    private Instant createdAt;

    protected ShoppingItem() {}

    public ShoppingItem(ShoppingList list, String name, Double quantity, String unit, String category, boolean recurring) {
        this.list = list;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.category = category;
        this.recurring = recurring;
        this.done = false;
        this.createdAt = Instant.now();
    }

    public ShoppingItem(ShoppingList list, String name) {
        this(list, name, null, null, null, false);
    }

    public ShoppingList getList() { return list; }
    public String getName() { return name; }
    public Double getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public String getCategory() { return category; }
    public boolean isRecurring() { return recurring; }
    public boolean isDone() { return done; }
    public Instant getCreatedAt() { return createdAt; }

    public void setName(String name) { this.name = name; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setCategory(String category) { this.category = category; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }
    public void setDone(boolean done) { this.done = done; }
}
