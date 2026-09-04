package dev.agiro.fanel.menu.domain;

import dev.agiro.fanel.menu.api.MealType;
import dev.agiro.fanel.shared.domain.UuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "meal_slot", uniqueConstraints = @UniqueConstraint(
        name = "uk_meal_slot_household_date_meal", columnNames = {"household_id", "date", "meal"}))
public class MealSlot extends UuidEntity {
    @Column(name = "household_id", length = 36, nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID householdId;
    @Column(nullable = false)
    private LocalDate date;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MealType meal;
    @Column(nullable = false, length = 500)
    private String text;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected MealSlot() {}

    public MealSlot(UUID householdId, LocalDate date, MealType meal, String text) {
        this.householdId = householdId;
        this.date = date;
        this.meal = meal;
        this.text = text;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getHouseholdId() { return householdId; }
    public LocalDate getDate() { return date; }
    public MealType getMeal() { return meal; }
    public String getText() { return text; }

    public void updateText(String text) {
        this.text = text;
    }
}
