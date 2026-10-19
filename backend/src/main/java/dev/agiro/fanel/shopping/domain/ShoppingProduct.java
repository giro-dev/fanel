package dev.agiro.fanel.shopping.domain;

import dev.agiro.fanel.shared.domain.UuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * A product of the bilingual catalog, shared by every item that refers to the same thing whatever
 * language it was typed in. {@code householdId == null} marks the shared, read-only seed;
 * {@code translated == false} means one of the names is still a copy of the other.
 */
@Entity
@Table(name = "shopping_product")
public class ShoppingProduct extends UuidEntity {
    @Column(name = "household_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID householdId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ShoppingCategory category;
    @Column(name = "name_ca", nullable = false)
    private String nameCa;
    @Column(name = "name_es", nullable = false)
    private String nameEs;
    private boolean translated;

    protected ShoppingProduct() {}

    public ShoppingProduct(UUID householdId, String nameCa, String nameEs, ShoppingCategory category,
                           boolean translated) {
        this.householdId = householdId;
        this.nameCa = nameCa;
        this.nameEs = nameEs;
        this.category = category;
        this.translated = translated;
    }

    public UUID getHouseholdId() { return householdId; }
    public ShoppingCategory getCategory() { return category; }
    public String getNameCa() { return nameCa; }
    public String getNameEs() { return nameEs; }
    public boolean isTranslated() { return translated; }
    public boolean isShared() { return householdId == null; }

    public void setCategory(ShoppingCategory category) { this.category = category; }
    public void setNameCa(String nameCa) { this.nameCa = nameCa; }
    public void setNameEs(String nameEs) { this.nameEs = nameEs; }
    public void setTranslated(boolean translated) { this.translated = translated; }

    public String name(String language) {
        return CatalogLanguage.ES.equals(language) ? nameEs : nameCa;
    }
}
