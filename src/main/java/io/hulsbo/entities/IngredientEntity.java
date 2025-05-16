package io.hulsbo.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "ingredients")
public class IngredientEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false, unique = true)
    public String name;
    
    @Column(name = "name_sv", unique = true)
    public String name_sv;

    @Column(name = "fat_ratio", nullable = false)
    public BigDecimal fat_ratio;

    @Column(name = "protein_ratio", nullable = false)
    public BigDecimal protein_ratio;

    @Column(name = "carbs_ratio", nullable = false)
    public BigDecimal carbs_ratio;

    @Column(name = "fiber_and_ash_ratio", nullable = false)
    public BigDecimal fiber_and_ash_ratio;

    @Column(name = "salt_ratio", nullable = false)
    public BigDecimal salt_ratio;

    @Column(name = "density_g_ml")
    public BigDecimal density_g_ml;
    
    @Column(name = "groups")
    public String groups;
    
    @Column(name = "g_per_pcs")
    public BigDecimal g_per_pcs;
    
    @Column(name = "default_measurement")
    public String default_measurement;
    
    @Column(name = "date_created", nullable = false)
    public ZonedDateTime date_created;
    
    @Column(name = "date_modified")
    public ZonedDateTime date_modified;
    
    @Column(name = "created_by_user_id")
    public UUID created_by_user_id;

    // Panache provides basic finders, persist, etc.
    // You can add custom finders here if needed, e.g.:
    // public static IngredientEntity findByName(String name){
    //     return find("name", name).firstResult();
    // }

    // Default constructor required by JPA
    public IngredientEntity() {}

    // Optional: Convenience constructor
    public IngredientEntity(String name, String name_sv, BigDecimal fat_ratio, BigDecimal protein_ratio, 
                           BigDecimal carbs_ratio, BigDecimal fiber_and_ash_ratio, BigDecimal salt_ratio,
                           BigDecimal density_g_ml, String groups, BigDecimal g_per_pcs, String default_measurement) {
        this.name = name;
        this.name_sv = name_sv;
        this.fat_ratio = fat_ratio;
        this.protein_ratio = protein_ratio;
        this.carbs_ratio = carbs_ratio;
        this.fiber_and_ash_ratio = fiber_and_ash_ratio;
        this.salt_ratio = salt_ratio;
        this.density_g_ml = density_g_ml;
        this.groups = groups;
        this.g_per_pcs = g_per_pcs;
        this.default_measurement = default_measurement;
        this.date_created = ZonedDateTime.now();
    }
}