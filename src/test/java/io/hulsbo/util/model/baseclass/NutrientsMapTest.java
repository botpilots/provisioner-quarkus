package io.hulsbo.util.model.baseclass;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class NutrientsMapTest {

    private NutrientsMap nutrientsMap;
    private static final double TOLERANCE = 1e-9;

    @BeforeEach
    void setUp() {
        nutrientsMap = new NutrientsMap();
    }

    @Test
    void testInitialState() {
        assertEquals(0.0, nutrientsMap.get("protein"), TOLERANCE);
        assertEquals(0.0, nutrientsMap.get("fat"), TOLERANCE);
        assertEquals(0.0, nutrientsMap.get("carbs"), TOLERANCE);
        assertEquals(0.0, nutrientsMap.get("water"), TOLERANCE);
        assertEquals(0.0, nutrientsMap.get("fiber"), TOLERANCE);
        assertEquals(0.0, nutrientsMap.get("salt"), TOLERANCE);
        assertEquals(6, nutrientsMap.size());
    }

    @Test
    void testPutValid() {
        nutrientsMap.put("protein", 0.2);
        assertEquals(0.2, nutrientsMap.get("protein"), TOLERANCE);
    }

    @Test
    void testPutInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            nutrientsMap.put("vitamin_c", 0.1);
        });
    }
    
    @Test
    void testPutNegativeValue() {
        assertThrows(IllegalArgumentException.class, () -> {
             nutrientsMap.put("protein", -0.1);
        });
    }

    @Test
    void testPutSumExceeds100Percent() {
        // This test is now invalid as put() should no longer throw this exception
        // We replace it with tests for the stealing logic
    }
    
    @Test
    void testPutCausesStealing() {
        nutrientsMap.put("protein", 0.5);
        nutrientsMap.put("fat", 0.5); // Sum = 1.0
        
        nutrientsMap.put("protein", 0.7); // Increase protein, should steal 0.2 from fat
        
        assertEquals(0.7, nutrientsMap.get("protein"), TOLERANCE);
        // Fat was 0.5. Other sum was 0.5. Increase was 0.2.
        // Scale factor = (0.5 - 0.2) / 0.5 = 0.3 / 0.5 = 0.6
        // New fat = 0.5 * 0.6 = 0.3
        assertEquals(0.3, nutrientsMap.get("fat"), TOLERANCE);
        assertEquals(0.0, nutrientsMap.get("carbs"), TOLERANCE);
        assertEquals(1.0, nutrientsMap.values().stream().mapToDouble(Double::doubleValue).sum(), TOLERANCE); // Verify sum is still 1.0
    }
    
    @Test
    void testPutCausesStealingMultipleOthers() {
        nutrientsMap.put("protein", 0.4);
        nutrientsMap.put("fat", 0.4);
        nutrientsMap.put("carbs", 0.2); // Sum = 1.0
        
        nutrientsMap.put("protein", 0.7); // Increase protein by 0.3, steal from fat and carbs
        
        assertEquals(0.7, nutrientsMap.get("protein"), TOLERANCE);
        // Other sum = 0.4 (fat) + 0.2 (carbs) = 0.6. Increase = 0.3.
        // Scale factor = (0.6 - 0.3) / 0.6 = 0.3 / 0.6 = 0.5
        // New fat = 0.4 * 0.5 = 0.2
        // New carbs = 0.2 * 0.5 = 0.1
        assertEquals(0.2, nutrientsMap.get("fat"), TOLERANCE);
        assertEquals(0.1, nutrientsMap.get("carbs"), TOLERANCE);
        assertEquals(1.0, nutrientsMap.values().stream().mapToDouble(Double::doubleValue).sum(), TOLERANCE);
    }

    @Test
    void testPutCausesClampTo100() {
        nutrientsMap.put("protein", 0.8);
        nutrientsMap.put("fat", 0.2); // Sum = 1.0
        
        // Try to increase protein by 0.3 (to 1.1), but can only steal 0.2 from fat.
        nutrientsMap.put("protein", 1.1); // Input gets clamped to 1.0 first
        
        // Because increase (1.0 - 0.8 = 0.2) >= otherSum (0.2), protein becomes 1.0, fat becomes 0.0
        assertEquals(1.0, nutrientsMap.get("protein"), TOLERANCE);
        assertEquals(0.0, nutrientsMap.get("fat"), TOLERANCE);
        assertEquals(0.0, nutrientsMap.get("carbs"), TOLERANCE);
        assertEquals(1.0, nutrientsMap.values().stream().mapToDouble(Double::doubleValue).sum(), TOLERANCE);
    }
    
    @Test
    void testPutValueUnchanged() {
        nutrientsMap.put("protein", 0.5);
        Map<String, Double> initialState = new HashMap<>(nutrientsMap);
        // Put the same value again (within tolerance)
        nutrientsMap.put("protein", 0.5 + TOLERANCE / 2);
        // Map should not have changed (stealing logic shouldn't run)
        assertEquals(initialState, nutrientsMap);
    }
    
     @Test
    void testPutValueClampedAt1() {
        nutrientsMap.put("protein", 2.0); // Try putting value > 1.0
        // Should be clamped to 1.0, others should become 0 because otherSum was 0 initially.
        assertEquals(1.0, nutrientsMap.get("protein"), TOLERANCE);
        assertEquals(0.0, nutrientsMap.get("fat"), TOLERANCE);
        assertEquals(1.0, nutrientsMap.values().stream().mapToDouble(Double::doubleValue).sum(), TOLERANCE);
    }

    @Test
    void testNormalizeRatiosSumLessThan1() {
        nutrientsMap.put("protein", 0.2);
        nutrientsMap.put("fat", 0.3); // Sum = 0.5
        nutrientsMap.normalizeRatios();
        // Expected: protein=0.4, fat=0.6
        assertEquals(0.4, nutrientsMap.get("protein"), TOLERANCE);
        assertEquals(0.6, nutrientsMap.get("fat"), TOLERANCE);
        assertEquals(0.0, nutrientsMap.get("carbs"), TOLERANCE); // Others remain 0
        // Verify sum is now 1.0
        assertEquals(1.0, nutrientsMap.values().stream().mapToDouble(Double::doubleValue).sum(), TOLERANCE);
    }

    @Test
    void testNormalizeRatiosSumEquals1() {
        nutrientsMap.put("protein", 0.4);
        nutrientsMap.put("fat", 0.6); // Sum = 1.0
        // Capture the current state
        Map<String, Double> initialState = new HashMap<>(nutrientsMap);
        
        nutrientsMap.normalizeRatios(); // Should do nothing
        
        // Verify state hasn't changed
        assertEquals(initialState, nutrientsMap);
        assertEquals(1.0, nutrientsMap.values().stream().mapToDouble(Double::doubleValue).sum(), TOLERANCE);
    }

    @Test
    void testNormalizeRatiosSumIs0() {
        // All values are 0 initially
         Map<String, Double> initialState = new HashMap<>(nutrientsMap);
        nutrientsMap.normalizeRatios(); // Should do nothing
        
        // Verify state hasn't changed
         assertEquals(initialState, nutrientsMap);
        assertEquals(0.0, nutrientsMap.values().stream().mapToDouble(Double::doubleValue).sum(), TOLERANCE);
    }

    @Test
    void testUpdateNutrientSameAsPut() {
        // Test that updateNutrient triggers stealing like put does
        nutrientsMap.updateNutrient("protein", 0.6);
        nutrientsMap.updateNutrient("fat", 0.4); // Sum = 1.0
        
        // Now try to update carbs, which should steal from protein and fat
        nutrientsMap.updateNutrient("carbs", 0.1); // Increase by 0.1, steal from protein (0.6) and fat (0.4)
        
        // Other sum = 0.6 + 0.4 = 1.0. Increase = 0.1
        // Scale factor = (1.0 - 0.1) / 1.0 = 0.9
        // New protein = 0.6 * 0.9 = 0.54
        // New fat = 0.4 * 0.9 = 0.36
        // New carbs = 0.1
        assertEquals(0.54, nutrientsMap.get("protein"), TOLERANCE);
        assertEquals(0.36, nutrientsMap.get("fat"), TOLERANCE);
        assertEquals(0.1, nutrientsMap.get("carbs"), TOLERANCE);
        assertEquals(1.0, nutrientsMap.values().stream().mapToDouble(Double::doubleValue).sum(), TOLERANCE);
        
        // Test invalid key and negative value (should still throw)
         assertThrows(IllegalArgumentException.class, () -> {
             nutrientsMap.updateNutrient("protein", -0.1);
         });
         
         assertThrows(IllegalArgumentException.class, () -> {
            nutrientsMap.updateNutrient("unknown", 0.1);
        });
    }

    @Test
    void testUnsupportedOperations() {
        assertThrows(UnsupportedOperationException.class, () -> {
            nutrientsMap.remove("protein");
        });
        assertThrows(UnsupportedOperationException.class, () -> {
            nutrientsMap.clear();
        });
    }
} 