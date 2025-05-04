package io.hulsbo.model;

import java.util.UUID;
import java.util.Map;
import io.quarkus.logging.Log;

public class Ingredient extends BaseClass {


    public Ingredient() {

		// Not sure this is needed, an ingredient should not have any children.
        for (UUID key : childMap.keySet()) {
            nutrientsMap.put(childMap.get(key).getChild().getName(), childMap.get(key).getRatio());
        }
    }

    // Call the overloaded constructor of BaseClass if id is provided
    public Ingredient(UUID id) {
        super(id);
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
     * Sets multiple nutrient ratios based on the provided map.
     * Performs validation *before* applying any changes to ensure the final sum does not exceed 100%.
     * Does NOT trigger normalization or propagation; call normalizeNutrientRatiosAndPropagate() after successful execution.
     *
     * @param updates A map where keys are nutrient names and values are the new ratios (0.0 to 1.0).
     * @throws IllegalArgumentException if any nutrient key is invalid, any value is negative, or the final sum would exceed 100%.
     */
    public void setNutrientRatios(Map<String, Double> updates) {
        // 1. Validate input map keys and values
        for (Map.Entry<String, Double> entry : updates.entrySet()) {
            String key = entry.getKey();
            Double value = entry.getValue();
            if (!this.nutrientsMap.containsKey(key)) {
                throw new IllegalArgumentException("Invalid nutrient key in updates map: " + key);
            }
            if (value == null || value < 0.0 || value > 1.0) { // Also check for > 1.0 here
                 throw new IllegalArgumentException("Invalid nutrient value for key '" + key + "': " + value + ". Must be between 0.0 and 1.0.");
            }
        }

        // 2. Calculate potential final sum
        double potentialSum = 0.0;
        // Start with current sum
        for(double currentVal : this.nutrientsMap.values()) {
            potentialSum += currentVal;
        }

        // Adjust sum based on updates
        for (Map.Entry<String, Double> entry : updates.entrySet()) {
            String key = entry.getKey();
            double newValue = entry.getValue();
            double oldValue = this.nutrientsMap.getOrDefault(key, 0.0); // Get current value
            potentialSum = potentialSum - oldValue + newValue; // Adjust sum
        }

        // 3. Validate potential final sum
        double TOLERANCE = 1e-9; // Use the same tolerance as NutrientsMap
        if (potentialSum > 1.0 + TOLERANCE) {
            throw new IllegalArgumentException(String.format(
                "Updating nutrients would exceed 100%%. Final sum would be %.1f%%.",
                potentialSum * 100));
        }

        // 4. If valid, apply all updates
        for (Map.Entry<String, Double> entry : updates.entrySet()) {
             // Use internalPut to bypass the individual sum check within NutrientsMap.put
             this.nutrientsMap.internalPut(entry.getKey(), entry.getValue());
        }
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
    public void updateAndPropagate() {
        Log.infof("[Ingredient ID: %s] Entering updateAndPropagate.", getId());
        // Ingredient-specific logic (like setEnergyDensity) happens first
        this.setEnergyDensity();
        super.updateAndPropagate(); // Propagate upwards
        Log.infof("[Ingredient ID: %s] Exiting updateAndPropagate.", getId());
    }

    public double getTotalWeight() {
        // Implementation of getTotalWeight method
        return 0.0; // Placeholder return, actual implementation needed
    }
}

