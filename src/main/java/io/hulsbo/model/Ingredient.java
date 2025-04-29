package io.hulsbo.model;

import java.security.SecureRandom;
import java.util.Set;
import io.hulsbo.util.model.SafeID;

public class Ingredient extends BaseClass {


    public Ingredient() {

		// Not sure this is needed, an ingredient should not have any children.
        for (SafeID key : childMap.keySet()) {
            nutrientsMap.put(childMap.get(key).getChild().getName(), childMap.get(key).getRatio());
        }
    }

    /**
     * Sets the ratio for a specific nutrient.
     * Performs validation to ensure the sum does not exceed 100%.
     * Does NOT trigger normalization or propagation; call normalizeNutrientRatios() after all updates.
     *
     * @param nutrient The name of the nutrient (e.g., "protein", "fat").
     * @param ratio    The ratio (0.0 to 1.0).
     * @throws IllegalArgumentException if the nutrient key is invalid, value is negative, or sum exceeds 100%.
     */
    public void setNutrientRatio(String nutrient, double ratio) {
         this.nutrientsMap.updateNutrient(nutrient, ratio); // Uses the updated put/updateNutrient logic in NutrientsMap
    }
    
     /**
     * Normalizes the nutrient ratios in the Ingredient's NutrientsMap so they sum to 1.0,
     * recalculates energy density, and propagates updates to parent objects.
     * Should be called after one or more calls to setNutrientRatio().
     */
    public void normalizeNutrientRatiosAndPropagate() {
        this.nutrientsMap.normalizeRatios(); // Scale ratios if sum < 1.0
        this.setEnergyDensity(); // Recalculate energy density based on new ratios
        this.updateAndPropagate(); // Propagate the changes upwards (to Meal, Adventure, etc.)
    }

    // Override updateAndPropagate
    @Override
    protected void updateAndPropagate() {
        // 1. Perform Ingredient-specific recalculations *first* (if any)
        // Currently none needed.

        // 2. Then, call the base implementation to propagate upwards
        super.updateAndPropagate();
    }
}

