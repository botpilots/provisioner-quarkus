package io.hulsbo.mapper;

import io.hulsbo.entities.IngredientEntity;
import io.hulsbo.model.Ingredient;
import io.hulsbo.util.model.baseclass.NutrientsMap; // Import NutrientsMap
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.HashMap; // Import HashMap
import java.util.Map;     // Import Map
import java.util.Objects;

@ApplicationScoped
public class IngredientMapper {

    /**
     * Maps an IngredientEntity to an Ingredient domain object.
     * The ID of the domain object is set by its constructor and is not mapped from the entity.
     * Density is not directly mapped.
     *
     * @param entity The IngredientEntity from the database.
     * @return The corresponding Ingredient domain object, or null if entity is null.
     */
    public Ingredient toDomain(IngredientEntity entity) {
        if (entity == null) {
            return null;
        }

        Ingredient domain = new Ingredient(entity.id);
        domain.setName(entity.name); // Use setter from BaseClass

        // Create a map to hold nutrient updates
        Map<String, Double> nutrientUpdates = new HashMap<>();

        // Populate the map from the entity, converting BigDecimal to double
        // Assumes entity fields are not null; add null checks if necessary
        if (entity.protein_ratio != null) {
             nutrientUpdates.put("protein", entity.protein_ratio.doubleValue());
        }
        if (entity.fat_ratio != null) {
             nutrientUpdates.put("fat", entity.fat_ratio.doubleValue());
        }
        if (entity.carbs_ratio != null) {
             nutrientUpdates.put("carbs", entity.carbs_ratio.doubleValue());
        }
         if (entity.fiber_ratio != null) {
             nutrientUpdates.put("fiber", entity.fiber_ratio.doubleValue());
        }
        if (entity.salt_ratio != null) {
             nutrientUpdates.put("salt", entity.salt_ratio.doubleValue());
        }

        // Water takes up the remaining part, so it's 1 - sum(entity nutrients)
        double waterRatio = 1.0 - nutrientUpdates.values().stream().mapToDouble(Double::doubleValue).sum();
        nutrientUpdates.put("water", waterRatio);

        // Ignore density if null (should default to water i.e. 1.0)
        if (entity.density_g_ml != null) {
            nutrientUpdates.put("density", entity.density_g_ml.doubleValue());
        }

        // Set nutrients using the Ingredient's method, which handles validation
        // Consider adding try-catch if setNutrientRatios can throw exceptions
        // that shouldn't happen if DB constraints are valid.
        domain.setNutrientRatios(nutrientUpdates);

		// Normalize nutrient ratios and propagate changes
        domain.normalizeNutrientRatiosAndPropagate();

        return domain;
    }

    /**
     * Updates an existing IngredientEntity with data from an Ingredient domain object.
     * Does NOT update the ID.
     * Ignores density.
     *
     * @param source The source Ingredient object containing updated data.
     * @param target The IngredientEntity to be updated (must not be null).
     */
    public void updateEntity(Ingredient source, IngredientEntity target) {
        Objects.requireNonNull(target, "Target IngredientEntity cannot be null");
        if (source == null) {
            return; // Nothing to update from
        }

        // Update name
        if (source.getName() != null) {
            target.name = source.getName();
        }

        // Get nutrients from the source domain object
        NutrientsMap sourceMap = source.getNutrientsMap();

        // Update entity fields, converting double to BigDecimal
        target.protein_ratio = BigDecimal.valueOf(sourceMap.getOrDefault("protein", 0.0));
        target.fat_ratio = BigDecimal.valueOf(sourceMap.getOrDefault("fat", 0.0));
        target.carbs_ratio = BigDecimal.valueOf(sourceMap.getOrDefault("carbs", 0.0));
        target.fiber_ratio = BigDecimal.valueOf(sourceMap.getOrDefault("fiber", 0.0));
        target.salt_ratio = BigDecimal.valueOf(sourceMap.getOrDefault("salt", 0.0));
        // Water is 1 - sum(entity nutrients) so can't be set here
		// Density is taken as a field of Ingredient
		// target.density_g_ml = BigDecimal.valueOf(source.getDensity());

    }

    /**
     * Maps an Ingredient domain object to a *new* IngredientEntity.
     * Used primarily for creating new database entries.
     * Does not map ID or density.
     *
     * @param domain The Ingredient domain object.
     * @return The corresponding IngredientEntity, or null if domain is null.
     */
    public IngredientEntity toEntity(Ingredient domain) {
        if (domain == null) {
            return null;
        }

        IngredientEntity entity = new IngredientEntity();

        // Set name
        entity.name = domain.getName();

        // Get nutrients from the domain object
        NutrientsMap domainMap = domain.getNutrientsMap();

        // Set entity fields, converting double to BigDecimal
        entity.protein_ratio = BigDecimal.valueOf(domainMap.getOrDefault("protein", 0.0));
        entity.fat_ratio = BigDecimal.valueOf(domainMap.getOrDefault("fat", 0.0));
        entity.carbs_ratio = BigDecimal.valueOf(domainMap.getOrDefault("carbs", 0.0));
        entity.fiber_ratio = BigDecimal.valueOf(domainMap.getOrDefault("fiber", 0.0));
        entity.salt_ratio = BigDecimal.valueOf(domainMap.getOrDefault("salt", 0.0));
        // Water is 1 - sum(entity nutrients) so can't be set here
	
		// Density is taken as a field of Ingredient
		// entity.density_g_ml = BigDecimal.valueOf(domain.getDensity());

        // We do not set the ID here; it should be generated by the database.

        return entity;
    }

} 