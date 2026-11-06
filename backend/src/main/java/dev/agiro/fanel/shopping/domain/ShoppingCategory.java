package dev.agiro.fanel.shopping.domain;

import dev.agiro.fanel.shared.domain.UuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/** Bilingual shopping category. {@code householdId == null} marks the shared, read-only seed. */
@Entity
@Table(name = "shopping_category")
public class ShoppingCategory extends UuidEntity {
    @Column(name = "household_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID householdId;
    private String code;
    @Column(name = "name_ca", nullable = false)
    private String nameCa;
    @Column(name = "name_es", nullable = false)
    private String nameEs;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected ShoppingCategory() {}

    public ShoppingCategory(UUID householdId, String nameCa, String nameEs, int sortOrder) {
        this.householdId = householdId;
        this.nameCa = nameCa;
        this.nameEs = nameEs;
        this.sortOrder = sortOrder;
    }

    public UUID getHouseholdId() { return householdId; }
    public String getCode() { return code; }
    public String getNameCa() { return nameCa; }
    public String getNameEs() { return nameEs; }
    public int getSortOrder() { return sortOrder; }

    public String name(String language) {
        return CatalogLanguage.ES.equals(language) ? nameEs : nameCa;
    }
}
