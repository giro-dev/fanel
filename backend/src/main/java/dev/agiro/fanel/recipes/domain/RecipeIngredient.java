package dev.agiro.fanel.recipes.domain;

import dev.agiro.fanel.shared.domain.UuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "recipe_ingredient")
public class RecipeIngredient extends UuidEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;
    private String name;
    private Double quantity;
    private String unit;
    private String category;
    private Instant createdAt;

    protected RecipeIngredient() {}

    public RecipeIngredient(Recipe recipe, String name, Double quantity, String unit, String category) {
        this.recipe = recipe;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.category = category;
        this.createdAt = Instant.now();
    }

    public String getName() { return name; }
    public Double getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public String getCategory() { return category; }
    public Instant getCreatedAt() { return createdAt; }
}
