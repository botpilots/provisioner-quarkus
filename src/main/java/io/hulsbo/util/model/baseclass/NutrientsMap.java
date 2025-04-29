package io.hulsbo.util.model.baseclass;

import io.quarkus.logging.Log;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class NutrientsMap extends AbstractMap<String, Double> {
    private final Map<String, Double> nutrientsMap = new LinkedHashMap<>();
    private static final double TOLERANCE = 1e-9;

    public NutrientsMap() {
        nutrientsMap.put("protein", 0.0);
        nutrientsMap.put("fat", 0.0);
        nutrientsMap.put("carbs", 0.0);
        nutrientsMap.put("water", 0.0);
        nutrientsMap.put("fiber", 0.0);
        nutrientsMap.put("salt", 0.0);
    }

    @Override
    public Set<Entry<String, Double>> entrySet() {
        return nutrientsMap.entrySet();
    }

    @Override
    public Double put(String key, Double value) {
        if (!nutrientsMap.containsKey(key)) {
            throw new IllegalArgumentException("Invalid nutrient key: " + key);
        }
        if (value < 0.0) {
             throw new IllegalArgumentException("Nutrient value cannot be negative: " + value);
        }
        // Clamp value at 1.0 maximum
        value = Math.min(value, 1.0);

        double oldValue = nutrientsMap.get(key);
        // If the value isn't changing significantly, just return
        if (Math.abs(value - oldValue) < TOLERANCE) {
             return oldValue;
        }

        double currentSum = sumRatios();
        double potentialSum = currentSum - oldValue + value;

        if (potentialSum > 1.0 + TOLERANCE) {
            // Stealing logic: Reduce other nutrients proportionally
            double increase = value - oldValue;
            double otherSum = currentSum - oldValue;

            if (otherSum < TOLERANCE || increase >= otherSum) {
                // Cannot steal enough, or other nutrients are zero.
                // Set current key to 1.0 and others to 0.0.
                Log.infof("Nutrient '%s' set to 100%%, others cleared (increase=%.4f, otherSum=%.4f)", key, increase, otherSum);
                value = 1.0; // Ensure the target value is 1.0
                nutrientsMap.put(key, value);
                for (String k : nutrientsMap.keySet()) {
                    if (!k.equals(key)) {
                        nutrientsMap.put(k, 0.0);
                    }
                }
            } else {
                // Steal proportionally from others
                double scaleFactor = (otherSum - increase) / otherSum;
                Log.infof("Nutrient '%s' increase causes stealing. Scale factor for others: %.4f", key, scaleFactor);
                nutrientsMap.put(key, value); // Set the target value first
                for (Map.Entry<String, Double> entry : nutrientsMap.entrySet()) {
                    if (!entry.getKey().equals(key)) {
                        nutrientsMap.put(entry.getKey(), entry.getValue() * scaleFactor);
                    }
                }
                // Optional: Verify sum after stealing (for debugging)
                // double finalSum = sumRatios();
                // if (Math.abs(finalSum - 1.0) > TOLERANCE) {
                //     Log.warnf("NutrientsMap sum after stealing is %.4f, expected 1.0.", finalSum);
                // }
            }
        } else {
            // Normal case: just update the value
            nutrientsMap.put(key, value);
            // We might still need normalization if potentialSum < 1.0
            // This is handled externally by Ingredient.normalizeNutrientRatiosAndPropagate()
        }
        
        return value;
    }
    
    public Double updateNutrient(String key, Double value) {
        return this.put(key, value);
    }
    
    private double sumRatios() {
        return nutrientsMap.values().stream().mapToDouble(Double::doubleValue).sum();
    }
    
    public void normalizeRatios() {
        double currentSum = sumRatios();

        if (Math.abs(currentSum - 1.0) < TOLERANCE || currentSum > 1.0 + TOLERANCE) {
            return;
        }

        if (currentSum < TOLERANCE) {
            return;
        }

        double scaleFactor = 1.0 / currentSum;
        Log.infof("Normalizing NutrientsMap. Current sum: %.4f, Scale factor: %.4f", currentSum, scaleFactor);

        for (Map.Entry<String, Double> entry : nutrientsMap.entrySet()) {
            double oldValue = entry.getValue();
            double newValue = oldValue * scaleFactor;
            nutrientsMap.put(entry.getKey(), newValue);
        }
    }

    @Override
    public Double remove(Object key) {
        throw new UnsupportedOperationException("Keys cannot be removed in a nutrientsMap.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("A nutrientsMaps cannot be cleared.");
    }

}

