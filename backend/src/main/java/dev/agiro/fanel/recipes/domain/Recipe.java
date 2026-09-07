package dev.agiro.fanel.recipes.domain;

import dev.agiro.fanel.shared.domain.UuidEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "recipe")
public class Recipe extends UuidEntity {
    @Column(name = "household_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID householdId;
    private String name;
    private int servings;
    private String notes;
    private Instant createdAt;

    @ElementCollection
    @CollectionTable(name = "recipe_tag", joinColumns = @JoinColumn(name = "recipe_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    protected Recipe() {}

    public Recipe(UUID householdId, String name, int servings, String notes, List<String> tags) {
        this.householdId = householdId;
        this.name = name;
        this.servings = servings;
        this.notes = notes;
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
        this.createdAt = Instant.now();
    }

    public UUID getHouseholdId() { return householdId; }
    public String getName() { return name; }
    public int getServings() { return servings; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public List<String> getTags() { return tags; }
    public List<RecipeIngredient> getIngredients() { return ingredients; }
}
