package dev.agiro.fanel.menu.domain;

import dev.agiro.fanel.shared.domain.UuidEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "meal_plan", uniqueConstraints = @UniqueConstraint(columnNames = {"household_id", "iso_year", "iso_week"}))
public class MealPlan extends UuidEntity {
    @Column(name = "household_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID householdId;
    @Column(name = "iso_year", nullable = false)
    private int isoYear;
    @Column(name = "iso_week", nullable = false)
    private int isoWeek;

    @OneToMany(mappedBy = "mealPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MealSlot> slots = new ArrayList<>();

    protected MealPlan() {}

    public MealPlan(UUID householdId, int isoYear, int isoWeek) {
        this.householdId = householdId;
        this.isoYear = isoYear;
        this.isoWeek = isoWeek;
    }

    public UUID getHouseholdId() { return householdId; }
    public int getIsoYear() { return isoYear; }
    public int getIsoWeek() { return isoWeek; }
    public List<MealSlot> getSlots() { return slots; }
}
