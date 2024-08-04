package io.hulsbo.model;

import java.util.Set;
import io.hulsbo.util.model.SafeID;

public class Meal extends BaseClass {

    /**
     * Add a new ingredient to the meal
     * @param newIngredient the new ingredient to add
     * @return
     */
    public SafeID putChild(Ingredient newIngredient) {
    return super.putChild(newIngredient, 0.0, 0.0);
}

    // TODO: Implement in appropriate resource method for /objects/meal
    /**
     * <p>This function does three things:</p>
     * <ol>
     *      <li>
     *        Modifies the absWeight (grams) of an ingredient.
     *      </li>
     *      <li>
     *        Recalculates totalAbsWeight of all the ingredients.
     *      </li>
     *      <li>
     *        Reassesses the ratios based on the new totalAbsWeight.
     *      </li>
     * </ol>
     * @param id The SafeId, used as key in the childMap, for the ingredient to be modified
     * @param absWeight the new absWeight (grams) for the ingredient
     */
    public void modifyAbsWeightOfIngredient(SafeID id, double absWeight) {
        if (childMap.get(id) == null) {
            throw new IllegalArgumentException("No Ingredient with such name exist.");
        }
        if (absWeight == 0) {
            throw new IllegalArgumentException("The absWeight cannot be 0.");
        }

        childMap.get(id).setRecipeWeight(absWeight);

        // Calculate the current total weight
        double totalAbsWeight = 0.0;

        Set<SafeID> keys = childMap.keySet();

        for (SafeID key : keys) {
            totalAbsWeight += childMap.get(key).getRecipeWeight();
        }

        // update all ratios
        for (SafeID key : keys) {
            double weightedValue = childMap.get(key).getRecipeWeight() / totalAbsWeight;
            modifyRatio(key, weightedValue);
        }

    }
}
