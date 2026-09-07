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
    private boolean done;
    private Instant createdAt;

    protected ShoppingItem() {}

    public ShoppingItem(ShoppingList list, String name) {
        this.list = list;
        this.name = name;
        this.done = false;
        this.createdAt = Instant.now();
    }

    public ShoppingList getList() { return list; }
    public String getName() { return name; }
    public boolean isDone() { return done; }
    public Instant getCreatedAt() { return createdAt; }

    public void setDone(boolean done) { this.done = done; }
}
