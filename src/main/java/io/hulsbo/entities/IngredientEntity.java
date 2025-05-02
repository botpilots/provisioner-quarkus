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
    public BigDecimal fatRatio;

    @Column(name = "protein_ratio", nullable = false)
    public BigDecimal proteinRatio;

    @Column(name = "carbs_ratio", nullable = false)
    public BigDecimal carbsRatio;

    @Column(name = "fiber_ratio", nullable = false)
    public BigDecimal fiberRatio;

    @Column(name = "salt_ratio", nullable = false)
    public BigDecimal saltRatio;

    // Panache provides basic finders, persist, etc.
    // You can add custom finders here if needed, e.g.:
    // public static IngredientEntity findByName(String name){
    //     return find("name", name).firstResult();
    // }

    // Default constructor required by JPA
    public IngredientEntity() {}

    // Optional: Convenience constructor
    public IngredientEntity(String name, BigDecimal fatRatio, BigDecimal proteinRatio, BigDecimal carbsRatio, BigDecimal fiberRatio, BigDecimal saltRatio) {
        this.name = name;
        this.fatRatio = fatRatio;
        this.proteinRatio = proteinRatio;
        this.carbsRatio = carbsRatio;
        this.fiberRatio = fiberRatio;
        this.saltRatio = saltRatio;
    }
} 