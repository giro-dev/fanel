package dev.agiro.fanel.shopping.domain;

import dev.agiro.fanel.shared.domain.UuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shopping_item")
public class ShoppingItem extends UuidEntity {
    @Column(name = "household_id", length = 36, nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID householdId;
    @Column(nullable = false, length = 200)
    private String text;
    @Column(nullable = false)
    private boolean done;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant doneAt;

    protected ShoppingItem() {}

    public ShoppingItem(UUID householdId, String text) {
        this.householdId = householdId;
        this.text = text;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getHouseholdId() { return householdId; }
    public String getText() { return text; }
    public boolean isDone() { return done; }
    public Instant getCreatedAt() { return createdAt; }

    public void updateText(String text) {
        this.text = text;
    }

    public void setDone(boolean done) {
        this.done = done;
        doneAt = done ? Instant.now() : null;
    }
}
