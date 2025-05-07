package io.hulsbo.model;

import io.hulsbo.util.model.baseclass.ChildWrapper;
import java.util.Set;
import java.util.UUID;
import io.quarkus.logging.Log;

public class Meal extends BaseClass {

public UUID putChild(Ingredient newIngredient) {
    return super.putChild(newIngredient, 0.0, 0.0);
}

    /**
     * <p>This function does three things:</p>
     * <ol>
     *      <li>
     *        Modifies the absWeight (grams) of an ingredient.
     *      </li>
     *      <li>
     *        Recalculate totalAbsWeight of all the ingredients.
     *      </li>
     *      <li>
     *        Reassess the ratios based on the new totalAbsWeight.
     *      </li>
     * </ol>
     * @param id The UUID, used as key in the childMap, for the ingredient to be modified
     * @param absWeight the new absWeight (grams) for the ingredient
     */
    public void modifyWeightOfIngredient(UUID id, double absWeight) {
        if (childMap.get(id) == null) {
            throw new IllegalArgumentException("No Ingredient with such an ID exist.");
        }
        if (absWeight == 0) {
            throw new IllegalArgumentException("The absWeight cannot be 0.");
        }

        childMap.get(id).setRecipeWeight(absWeight);

        // Calculate the current total weight
        double totalAbsWeight = 0.0;

        Set<UUID> keys = childMap.keySet();

        for (UUID key : keys) {
            totalAbsWeight += childMap.get(key).getRecipeWeight();
        }

        // update all ratios
        for (UUID key : keys) {
            double weightedValue = childMap.get(key).getRecipeWeight() / totalAbsWeight;
            modifyRatio(key, weightedValue);
        }
    }

    // Override updateAndPropagate
    @Override
    public void updateAndPropagate() {
        Log.infof("[Meal ID: %s] Entering updateAndPropagate.", getId());
        this.setNutrientsMapAndWeights(); // Meal-specific update
        super.updateAndPropagate(); // Propagate upwards
        Log.infof("[Meal ID: %s] Exiting updateAndPropagate.", getId());
    }

}
