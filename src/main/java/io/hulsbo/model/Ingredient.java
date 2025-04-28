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

    // Override updateAndPropagate
    @Override
    protected void updateAndPropagate() {
        // 1. Perform Ingredient-specific recalculations *first* (if any)
        // Currently none needed.

        // 2. Then, call the base implementation to propagate upwards
        super.updateAndPropagate();
    }
}

