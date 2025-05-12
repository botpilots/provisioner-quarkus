package io.hulsbo.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ingredients")
public class IngredientEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false, unique = true) // Assuming name should be unique and not null
    public String name;

    @Column(name = "fat_ratio", nullable = false) // Match the column name from your test
    public BigDecimal fat_ratio;

    @Column(name = "protein_ratio", nullable = false)
    public BigDecimal protein_ratio;

    @Column(name = "carbs_ratio", nullable = false)
    public BigDecimal carbs_ratio;

    @Column(name = "fiber_ratio", nullable = false)
    public BigDecimal fiber_ratio;

    @Column(name = "salt_ratio", nullable = false)
    public BigDecimal salt_ratio;

    // density_g_ml is nullable, meaning that the models default density 1.0 should be used.
    @Column(name = "density_g_ml")
    public BigDecimal density_g_ml;

    // Panache provides basic finders, persist, etc.
    // You can add custom finders here if needed, e.g.:
    // public static IngredientEntity findByName(String name){
    //     return find("name", name).firstResult();
    // }

    // Default constructor required by JPA
    public IngredientEntity() {}

    // Optional: Convenience constructor
    public IngredientEntity(String name, BigDecimal fat_ratio, BigDecimal protein_ratio, BigDecimal carbs_ratio, BigDecimal fiber_ratio, BigDecimal salt_ratio) {
        this.name = name;
        this.fat_ratio = fat_ratio;
        this.protein_ratio = protein_ratio;
        this.carbs_ratio = carbs_ratio;
        this.fiber_ratio = fiber_ratio;
        this.salt_ratio = salt_ratio;
    }
} 