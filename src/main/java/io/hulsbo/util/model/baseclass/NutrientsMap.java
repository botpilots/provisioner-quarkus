package io.hulsbo.util.model.baseclass;

import io.quarkus.logging.Log;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

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
            throw new IllegalArgumentException(String.format(Locale.US,
                "Cannot set %s to %.1f%%. Overshoot by  %.1f%%",
                key, value * 100, (potentialSum-1) * 100));
        } else {
            // Normal case: just update the value
            nutrientsMap.put(key, value);
            // Normalization, if needed (when sum < 1.0), is handled externally.
        }

        return value;
    }
    
    /**
     * Internal put method that bypasses the potentialSum check.
     * Used by batch update methods that have already validated the final sum.
     * Still performs basic key and negative value checks.
     */
    public Double internalPut(String key, Double value) {
        if (!nutrientsMap.containsKey(key)) {
            throw new IllegalArgumentException("Invalid nutrient key: " + key);
        }
        if (value < 0.0) {
             throw new IllegalArgumentException("Nutrient value cannot be negative: " + value);
        }
        // Clamp value at 1.0 maximum
        value = Math.min(value, 1.0);

        // Direct put without sum validation
        return nutrientsMap.put(key, value);
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

