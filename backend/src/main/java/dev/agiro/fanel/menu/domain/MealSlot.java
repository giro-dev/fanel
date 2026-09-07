package dev.agiro.fanel.menu.domain;

import dev.agiro.fanel.menu.api.MealType;
import dev.agiro.fanel.shared.domain.UuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "meal_slot", uniqueConstraints = @UniqueConstraint(columnNames = {"meal_plan_id", "day_of_week", "meal_type"}))
public class MealSlot extends UuidEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlan mealPlan;
    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;
    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false)
    private MealType mealType;
    private String text;
    @Column(name = "recipe_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID recipeId;

    protected MealSlot() {}

    public MealSlot(MealPlan mealPlan, int dayOfWeek, MealType mealType, String text) {
        this.mealPlan = mealPlan;
        this.dayOfWeek = dayOfWeek;
        this.mealType = mealType;
        this.text = text;
    }

    public int getDayOfWeek() { return dayOfWeek; }
    public MealType getMealType() { return mealType; }
    public String getText() { return text; }
    public UUID getRecipeId() { return recipeId; }

    public void setText(String text) { this.text = text; }
    public void setRecipeId(UUID recipeId) { this.recipeId = recipeId; }
}
