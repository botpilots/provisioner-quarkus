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

}

