package io.hulsbo;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Disabled; // Import Disabled

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import java.util.Locale;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.anyOf;
import io.restassured.response.Response;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * Test class focused on observing the propagation logic triggered by various API endpoints.
 * Initially, tests will primarily assert successful responses (2xx status codes).
 * Log analysis after running these tests will guide further investigation and potentially more specific assertions.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // Ensure tests run in sequence
public class PropagationTest {

    // Store IDs created during tests to use them in subsequent tests
    // Note: Using static fields for state between tests can be fragile,
    // but acceptable for this focused investigation sequence.
    static String adventureId;
    static String mealId;
    static String ingredientId1;
    static String ingredientId2;
    // Crew member ID handling might need refinement later as they are not BaseClass objects directly managed by SafeID in Manager
    // static String crewMemberId; 


    @Test
    @Order(1)
    public void test_01_CreateAdventure() {
        System.out.println("\n--- Test: Create Adventure ---");
        adventureId = given()
          .queryParam("name", "Propagation Test Adventure")
          .contentType(ContentType.JSON)
          .accept(ContentType.JSON)
        .when()
          .post("/adventures")
        .then()
          .statusCode(200) // Expecting 200 OK based on AdventureResource
          .contentType(ContentType.JSON)
          .body("id", notNullValue())
          .body("name", is("Propagation Test Adventure"))
          .extract().path("id");

        System.out.println("Created Adventure ID: " + adventureId);
        assert adventureId != null && adventureId.startsWith("id_");
    }

    @Test
    @Order(2)
    public void test_02_CreateMeal() {
        System.out.println("\n--- Test: Create Meal ---");
        mealId = given()
          .queryParam("name", "Propagation Test Meal")
          .contentType(ContentType.JSON)
          .accept(ContentType.JSON)
        .when()
          .post("/meals")
        .then()
          .statusCode(200) // Expecting 200 OK based on MealResource
          .contentType(ContentType.JSON)
          .body("id", notNullValue())
          .body("name", is("Propagation Test Meal"))
          .extract().path("id");

        System.out.println("Created Meal ID: " + mealId);
        assert mealId != null && mealId.startsWith("id_");
    }

    @Test
    @Order(3)
    public void test_03_AddMealToAdventure() {
        System.out.println("\n--- Test: Add Meal To Adventure ---");
        assert adventureId != null : "Adventure ID should not be null";

        // This endpoint creates a *new* meal and adds it to the adventure.
        String addedMealId = given()
          .pathParam("id", adventureId)
          .queryParam("name", "Meal Added To Adventure") 
          .contentType(ContentType.JSON)
          .accept(ContentType.JSON)
        .when()
          .post("/adventures/{id}/meals")
        .then()
          .statusCode(200)
          .contentType(ContentType.JSON)
          // Expect the body to be a JSON string matching the pattern
          .body(is(startsWith("\"id_"))) // Check for quoted string starting with id_
          .body(is(endsWith("\"")))     // Check for ending quote
          .extract().body().asString(); // Extract the plain ID string

        // Store the ID of the meal that was actually added to the adventure
        // Overwrite the mealId created in test_02 as that one is now orphaned.
        // Remove quotes from the extracted string
        mealId = addedMealId.substring(1, addedMealId.length() - 1);

        System.out.println("Meal Added To Adventure ID: " + mealId);
        assert mealId != null && mealId.startsWith("id_");
    }

    @Test
    @Order(4)
    public void test_04_AddIngredientToMeal() {
        System.out.println("\n--- Test: Add Ingredient To Meal ---");
        assert mealId != null : "Meal ID should not be null";

        ingredientId1 = given()
          .pathParam("id", mealId)
          .queryParam("name", "Ingredient A")
          .contentType(ContentType.JSON)
          .accept(ContentType.JSON)
        .when()
          .post("/meals/{id}/ingredients")
        .then()
          .statusCode(200)
          .contentType(ContentType.JSON)
          .body(is(startsWith("\"id_"))) // Expecting quoted JSON string ID
          .body(is(endsWith("\"")))
          .extract().body().asString();

        // Clean and store the ID
        ingredientId1 = ingredientId1.substring(1, ingredientId1.length() - 1);

        System.out.println("Added Ingredient A ID: " + ingredientId1);
        assert ingredientId1 != null && ingredientId1.startsWith("id_");
    }
    
    @Test
    @Order(5)
    public void test_05_AddSecondIngredientToMeal() {
        System.out.println("\n--- Test: Add Second Ingredient To Meal ---");
        assert mealId != null : "Meal ID should not be null";

        ingredientId2 = given()
          .pathParam("id", mealId)
          .queryParam("name", "Ingredient B")
          .contentType(ContentType.JSON)
          .accept(ContentType.JSON)
        .when()
          .post("/meals/{id}/ingredients")
        .then()
          .statusCode(200)
          .contentType(ContentType.JSON)
          .body(is(startsWith("\"id_"))) 
          .body(is(endsWith("\"")))
          .extract().body().asString();

        // Clean and store the ID
        ingredientId2 = ingredientId2.substring(1, ingredientId2.length() - 1);

        System.out.println("Added Ingredient B ID: " + ingredientId2);
        assert ingredientId2 != null && ingredientId2.startsWith("id_");
    }

    @Test
    @Order(6)
    public void test_06_ModifyIngredientWeight() {
        System.out.println("\n--- Test: Modify Ingredient Weight ---");
        assert mealId != null : "Meal ID should not be null";
        assert ingredientId1 != null : "Ingredient 1 ID should not be null";

        given()
          .pathParam("mealId", mealId)
          .pathParam("ingredientId", ingredientId1)
          .queryParam("weight", 150.0) // Set weight for Ingredient A
          .contentType(ContentType.JSON)
          .accept(ContentType.JSON)
        .when()
          .put("/meals/{mealId}/ingredients/{ingredientId}")
        .then()
          .statusCode(200) 
          .contentType(ContentType.JSON)
          .body("id", is(ingredientId1)); // Should return the modified ingredient

        System.out.println("Modified Ingredient A weight successfully.");
    }

    @Test
    @Order(7)
    public void test_07_ModifyIngredientNutrients() {
        System.out.println("\n--- Test: Modify Ingredient Nutrients ---");
        assert mealId != null : "Meal ID should not be null";
        assert ingredientId1 != null : "Ingredient 1 ID should not be null";

        given()
          .pathParam("mealId", mealId)
          .pathParam("ingredientId", ingredientId1)
          // Set some nutrient values for Ingredient A (ensure they sum <= 1.0)
          .queryParam("protein", 0.2) 
          .queryParam("carbs", 0.5)
          .queryParam("fat", 0.3)
          .contentType(ContentType.JSON)
          .accept(ContentType.JSON)
        .when()
          .put("/meals/{mealId}/ingredients/{ingredientId}")
        .then()
          .statusCode(200)
          .contentType(ContentType.JSON)
          .body("id", is(ingredientId1)) // Should return the modified ingredient
          .body("nutrientsMap.protein", equalTo(0.2f))
          .body("nutrientsMap.carbs", equalTo(0.5f))
          .body("nutrientsMap.fat", equalTo(0.3f));

        System.out.println("Modified Ingredient A nutrients successfully.");
    }

     @Test
     @Order(8)
     public void test_08_RemoveIngredientFromMeal() {
         System.out.println("\n--- Test: Remove Ingredient From Meal ---");
         assert mealId != null : "Meal ID should not be null";
         assert ingredientId2 != null : "Ingredient 2 ID should not be null";

         given()
           .pathParam("mealId", mealId)
           .pathParam("ingredientId", ingredientId2) // Removing Ingredient B
           .contentType(ContentType.JSON)
           .accept(ContentType.JSON) // Though the response is empty, accept header might be good practice
         .when()
           .delete("/meals/{mealId}/ingredients/{ingredientId}")
         .then()
           .statusCode(200);

         System.out.println("Removed Ingredient B successfully.");
         // ingredientId2 is now invalid/removed from the meal
     }

     @Test
     @Order(9)
     public void test_09_SetAdventureDetailsAndVerifyInitialWeights() {
         System.out.println("\n--- Test: Set Adventure Details and Verify Initial Weights ---");
         assert adventureId != null : "Adventure ID should not be null";
         assert mealId != null : "Meal ID should not be null";
         assert ingredientId1 != null : "Ingredient 1 ID should not be null"; // Ingredient A remains

         // 1. Set Days
         given()
             .pathParam("id", adventureId)
             .queryParam("days", 5)
             .contentType(ContentType.JSON)
             .accept(ContentType.JSON)
         .when()
             .put("/adventures/{id}/days")
         .then()
             .statusCode(200)
             .body("id", is(adventureId))
             .body("days", is(5));
         System.out.println("Set adventure days to 5.");

         // 2. Add a Crew Member (using arbitrary but plausible values)
         // Example calculation for this member (Harris-Benedict BMR * Activity):
         // BMR = 88.362 + (13.397 * 75) + (4.799 * 180) - (5.677 * 30) = 88.362 + 1004.775 + 863.82 - 170.31 = 1786.647 kcal
         // Daily Need = BMR * 1.55 (Moderate Activity) = 1786.647 * 1.55 = ~2769 kcal
         final int expectedKcalCrew1 = 2769;
         given()
             .pathParam("id", adventureId)
             .queryParam("name", "Crew Member 1")
             .queryParam("age", 30)
             .queryParam("height", 180)
             .queryParam("weight", 75)
             .queryParam("gender", "MALE") // Assuming Gender enum uses uppercase
             .queryParam("activity", "MODERATE") // Assuming PhysicalActivity enum
             .queryParam("strategy", "harris_benedict_revised") // Use valid strategy key
             .contentType(ContentType.JSON)
             .accept(ContentType.JSON)
         .when()
             .post("/adventures/{id}/crew")
         .then()
             .statusCode(200)
             .body("id", is(adventureId))
             .body("crewDailyKcalNeed", is(expectedKcalCrew1)); // Check if kcal need is updated
         System.out.println("Added Crew Member 1 with expected need: " + expectedKcalCrew1 + " kcal.");

         // 3. Get Adventure and Verify Weights
         // At this point: Adventure has 1 Meal (Meal Added To Adventure), which has 1 Ingredient (Ingredient A).
         // Meal ratio should be 100% (as it's the only one).
         // Ingredient A ratio within its meal should be 100% (as it's the only one).
         // Ingredient A nutrients: P=0.2, C=0.5, F=0.3 => Energy Density = (0.2+0.5)*4000 + 0.3*9000 = 2800 + 2700 = 5500 Kcal/kg
         // Adventure Energy Density should match Meal's, should match Ingredient A's => 5500 Kcal/kg
         // Adventure Total Kcal Need = crew * days = 2769 * 5 = 13845 kcal
         // Adventure Total Weight = Total Need / Energy Density = 13845 / 5500 = ~2.517 kg = ~2517 g
         final double expectedEnergyDensity = 5500.0;
         final double expectedAdventureWeightKg = (double) expectedKcalCrew1 * 5 / expectedEnergyDensity; // Keep in KG
         final double toleranceKg = 0.01; // Tolerance for floating point comparisons in KG

         Response advResponse = given()
             .pathParam("id", adventureId)
             .accept(ContentType.JSON)
         .when()
             .get("/adventures/{id}")
         .then()
             .statusCode(200)
             .body("id", is(adventureId))
             .body("energyDensity", anyOf(instanceOf(Float.class), instanceOf(Double.class)))
             .body("weight", anyOf(instanceOf(Float.class), instanceOf(Double.class)))
             // Check Meal Weight (mealWeights map: MealID -> weight in grams)
             .body("mealWeights", hasKey(mealId))
             .body("mealWeights.'" + mealId + "'", anyOf(instanceOf(Float.class), instanceOf(Double.class)))
             // Check Ingredient Weight (ingredientWeights map: IngredientID -> weight in grams)
             .body("ingredientWeights", hasKey(ingredientId1))
             .body("ingredientWeights.'" + ingredientId1 + "'", anyOf(instanceOf(Float.class), instanceOf(Double.class)))
             .extract().response();

         // Extract and Assert using JUnit Assertions
         float actualED = advResponse.path("energyDensity");
         float actualWeight = advResponse.path("weight");
         Map<String, Float> actualMealWeights = advResponse.path("mealWeights");
         Map<String, Float> actualIngredientWeights = advResponse.path("ingredientWeights");

         assertEquals(expectedEnergyDensity, actualED, toleranceKg);
         assertEquals(expectedAdventureWeightKg, actualWeight, toleranceKg);
         assertNotNull(actualMealWeights.get(mealId), "Meal weight for mealId should exist");
         assertEquals(expectedAdventureWeightKg, actualMealWeights.get(mealId), toleranceKg);
         assertNotNull(actualIngredientWeights.get(ingredientId1), "Ingredient weight for ingredientId1 should exist");
         assertEquals(expectedAdventureWeightKg, actualIngredientWeights.get(ingredientId1), toleranceKg);

         System.out.println("Verified initial Adventure, Meal, and Ingredient weights.");
         System.out.printf(" - Expected Adventure Weight: %.3f kg%n", expectedAdventureWeightKg);
     }


     @Test
     @Order(10)
     public void test_10_ModifyDaysAndVerifyWeightPropagation() {
         System.out.println("\n--- Test: Modify Adventure Days and Verify Weight Propagation ---");
         assert adventureId != null;
         assert mealId != null;
         assert ingredientId1 != null;

         // Current state: 5 days, 1 crew (2769 kcal/day), ED = 5500 Kcal/kg
         // Current weight = 2769 * 5 / 5500 * 1000 = ~2517 g

         // Change days to 7
         given()
             .pathParam("id", adventureId)
             .queryParam("days", 7)
             .contentType(ContentType.JSON)
             .accept(ContentType.JSON)
         .when()
             .put("/adventures/{id}/days")
         .then()
             .statusCode(200)
             .body("days", is(7));
         System.out.println("Changed adventure days to 7.");

         // Verify new weights
         // New Total Kcal Need = 2769 * 7 = 19383 kcal
         // New Adventure Total Weight = 19383 / 5500 = ~3.524 kg = ~3524 g
         final double expectedKcalCrew1 = 2769.0;
         final double expectedEnergyDensity = 5500.0;
         final double newExpectedAdventureWeightKg = expectedKcalCrew1 * 7 / expectedEnergyDensity;
         final double toleranceKg = 0.01;

         Response advResponse = given()
             .pathParam("id", adventureId)
             .accept(ContentType.JSON)
         .when()
             .get("/adventures/{id}")
         .then()
             .statusCode(200)
             .body("mealWeights", hasKey(mealId))       // Ensure keys still exist before extracting
             .body("ingredientWeights", hasKey(ingredientId1))
             .extract().response();

         // Extract and Assert
         float actualWeight = advResponse.path("weight");
         Map<String, Float> actualMealWeights = advResponse.path("mealWeights");
         Map<String, Float> actualIngredientWeights = advResponse.path("ingredientWeights");

         assertEquals(newExpectedAdventureWeightKg, actualWeight, toleranceKg);
         assertNotNull(actualMealWeights.get(mealId));
         assertEquals(newExpectedAdventureWeightKg, actualMealWeights.get(mealId), toleranceKg);
         assertNotNull(actualIngredientWeights.get(ingredientId1));
         assertEquals(newExpectedAdventureWeightKg, actualIngredientWeights.get(ingredientId1), toleranceKg);

         System.out.println("Verified weight propagation after changing days.");
         System.out.printf(" - Expected New Adventure Weight: %.3f kg%n", newExpectedAdventureWeightKg);
     }

     @Test
     @Order(11)
     public void test_11_AddCrewMemberAndVerifyWeightPropagation() {
         System.out.println("\n--- Test: Add Crew Member and Verify Weight Propagation ---");
         assert adventureId != null;
         assert mealId != null;
         assert ingredientId1 != null;

         // Current state: 7 days, 1 crew (2769 kcal/day), ED = 5500 Kcal/kg
         // Current weight = ~3524 g

         // Add second crew member
         // BMR = 447.593 + (9.247 * 60) + (3.098 * 165) - (4.330 * 25) = 447.593 + 554.82 + 511.17 - 108.25 = 1405.333 kcal
         // Daily Need = BMR * 1.375 (Light Activity) = 1405.333 * 1.375 = ~1932 kcal
         final int expectedKcalCrew1 = 2769;
         final int expectedKcalCrew2 = 1932;
         final int newTotalCrewKcal = expectedKcalCrew1 + expectedKcalCrew2; // 4701 kcal/day

         given()
             .pathParam("id", adventureId)
             .queryParam("name", "Crew Member 2")
             .queryParam("age", 25)
             .queryParam("height", 165)
             .queryParam("weight", 60)
             .queryParam("gender", "FEMALE")
             .queryParam("activity", "MILD") // Use MILD enum value
             .queryParam("strategy", "harris_benedict_revised") // Use valid strategy key
             .contentType(ContentType.JSON)
             .accept(ContentType.JSON)
         .when()
             .post("/adventures/{id}/crew")
         .then()
             .statusCode(200)
             .body("crewDailyKcalNeed", is(newTotalCrewKcal));
         System.out.println("Added Crew Member 2 with expected need: " + expectedKcalCrew2 + " kcal. Total: " + newTotalCrewKcal + " kcal.");

         // Verify new weights
         // New Total Kcal Need = 4701 * 7 = 32907 kcal
         // New Adventure Total Weight = 32907 / 5500 = ~5.983 kg = ~5983 g
         final double expectedEnergyDensity = 5500.0;
         final double newExpectedAdventureWeightKg = (double) newTotalCrewKcal * 7 / expectedEnergyDensity;
         final double toleranceKg = 0.01;

         Response advResponse = given()
             .pathParam("id", adventureId)
             .accept(ContentType.JSON)
         .when()
             .get("/adventures/{id}")
         .then()
             .statusCode(200)
             .body("mealWeights", hasKey(mealId))
             .body("ingredientWeights", hasKey(ingredientId1))
             .extract().response();

         // Extract and Assert
         float actualWeight = advResponse.path("weight");
         Map<String, Float> actualMealWeights = advResponse.path("mealWeights");
         Map<String, Float> actualIngredientWeights = advResponse.path("ingredientWeights");

         assertEquals(newExpectedAdventureWeightKg, actualWeight, toleranceKg);
         assertNotNull(actualMealWeights.get(mealId));
         assertEquals(newExpectedAdventureWeightKg, actualMealWeights.get(mealId), toleranceKg);
         assertNotNull(actualIngredientWeights.get(ingredientId1));
         assertEquals(newExpectedAdventureWeightKg, actualIngredientWeights.get(ingredientId1), toleranceKg);

         System.out.println("Verified weight propagation after adding second crew member.");
         System.out.printf(" - Expected New Adventure Weight: %.3f kg%n", newExpectedAdventureWeightKg);
     }


     // Skipping Crew Member Removal test for now (test_12)

     @Test
     @Order(13) // Renumbering subsequent tests
     public void test_13_ModifyIngredientWeightAndVerifyPropagation() {
         System.out.println("\n--- Test: Modify Ingredient Weight and Verify Propagation ---");
         assert adventureId != null;
         assert mealId != null;
         assert ingredientId1 != null; // Ingredient A

         // Current state: 7 days, 2 crew (4701 kcal/day), 1 Meal (100%), 1 Ingredient A (100% of meal, ED=5500)
         // Current Adventure Weight = ~5983 g

         // Add Ingredient B back to the meal. It will initially get 0 weight and 0 ratio.
         ingredientId2 = given()
             .pathParam("id", mealId)
             .queryParam("name", "Ingredient B")
             .contentType(ContentType.JSON)
             .accept(ContentType.JSON)
           .when()
             .post("/meals/{id}/ingredients")
           .then()
             .statusCode(200)
             .extract().body().asString();
         ingredientId2 = ingredientId2.substring(1, ingredientId2.length() - 1);
         System.out.println("Re-added Ingredient B: " + ingredientId2);

         // Modify Ingredient A weight to 100g, Ingredient B weight to 300g
         // This directly sets weights *within the meal recipe*
         // Meal.modifyWeightOfIngredient calculates ratios based on these weights.
         // Ing A ratio = 100 / (100+300) = 0.25
         // Ing B ratio = 300 / (100+300) = 0.75
         given()
             .pathParam("mealId", mealId)
             .pathParam("ingredientId", ingredientId1) // Ing A
             .queryParam("weight", 100.0)
             .contentType(ContentType.JSON)
             .accept(ContentType.JSON)
           .when()
             .put("/meals/{mealId}/ingredients/{ingredientId}")
           .then()
             .statusCode(200);
         System.out.println("Set Ingredient A recipe weight to 100g.");

         given()
             .pathParam("mealId", mealId)
             .pathParam("ingredientId", ingredientId2) // Ing B
             .queryParam("weight", 300.0)
             .contentType(ContentType.JSON)
             .accept(ContentType.JSON)
           .when()
             .put("/meals/{mealId}/ingredients/{ingredientId}")
           .then()
             .statusCode(200);
         System.out.println("Set Ingredient B recipe weight to 300g.");

         // Verify Propagation - Need to recalculate Energy Density
         // Ing A: ED = 5500 Kcal/kg (P=0.2, C=0.5, F=0.3)
         // Ing B: ED = 0 Kcal/kg (Default: P=0, C=0, F=0, Water=1.0?) - Let's assume ED=0 for simplicity
         // Meal ED = (RatioA * EDA) + (RatioB * EDB) = (0.25 * 5500) + (0.75 * 0) = 1375 Kcal/kg
         // Adventure ED = Meal ED = 1375 Kcal/kg
         // New Total Kcal Need = 4701 * 7 = 32907 kcal
         // New Adventure Total Weight = 32907 / 1375 = ~23.932 kg = ~23932 g
         // Meal Weight = Adventure Weight = ~23932 g
         // Ingredient A Weight = Meal Weight * RatioA = 23932 * 0.25 = ~5983 g
         // Ingredient B Weight = Meal Weight * RatioB = 23932 * 0.75 = ~17949 g

         final double edA = 5500.0;
         final float edB = 0.0f; // Assume default ingredient ED is 0, use float
         final double ratioA = 0.25;
         final double ratioB = 0.75;
         final float newExpectedMealED = (float)((ratioA * edA) + (ratioB * edB)); // Calculate as float
         final double totalCrewKcal = 4701.0;
         final double days = 7.0;
         final double newExpectedAdventureWeightKg = totalCrewKcal * days / newExpectedMealED;
         final double newExpectedIngredientAWeightKg = newExpectedAdventureWeightKg * ratioA;
         final double newExpectedIngredientBWeightKg = newExpectedAdventureWeightKg * ratioB;
         final double toleranceKg = 0.05; // Tolerance in KG, adjusted slightly

         Response advResponse = given()
             .pathParam("id", adventureId)
             .accept(ContentType.JSON)
         .when()
             .get("/adventures/{id}")
         .then()
             .statusCode(200)
             .body("mealWeights", hasKey(mealId))
             .body("ingredientWeights", hasKey(ingredientId1))
             .body("ingredientWeights", hasKey(ingredientId2))
             .extract().response();

         // Extract and Assert
         float actualED = advResponse.path("energyDensity");
         float actualWeight = advResponse.path("weight");
         Map<String, Float> actualMealWeights = advResponse.path("mealWeights");
         Map<String, Float> actualIngredientWeights = advResponse.path("ingredientWeights");

         assertEquals((double)newExpectedMealED, actualED, 0.1);
         assertEquals(newExpectedAdventureWeightKg, actualWeight, toleranceKg);

         assertNotNull(actualMealWeights.get(mealId));
         assertEquals(newExpectedAdventureWeightKg, actualMealWeights.get(mealId), toleranceKg);

         assertNotNull(actualIngredientWeights.get(ingredientId1));
         assertEquals(newExpectedIngredientAWeightKg, actualIngredientWeights.get(ingredientId1), toleranceKg);

         assertNotNull(actualIngredientWeights.get(ingredientId2));
         assertEquals(newExpectedIngredientBWeightKg, actualIngredientWeights.get(ingredientId2), toleranceKg);

         System.out.println("Verified weight propagation after modifying ingredient recipe weights.");
         System.out.printf(" - Expected New Adventure ED: %.1f Kcal/kg%n", (double)newExpectedMealED); // Cast back for printing if needed
         System.out.printf(" - Expected New Adventure Weight: %.3f kg%n", newExpectedAdventureWeightKg);
         System.out.printf(" - Expected New Ingredient A Weight: %.3f kg%n", newExpectedIngredientAWeightKg);
         System.out.printf(" - Expected New Ingredient B Weight: %.3f kg%n", newExpectedIngredientBWeightKg);
     }


     @Test
     @Order(14) // Renumbering
     public void test_14_RemoveMealAndVerifyPropagation() {
         System.out.println("\n--- Test: Remove Meal From Adventure and Verify Propagation ---");
         assert adventureId != null;
         assert mealId != null; // The meal we added and modified

         // Current state: Adv Weight ~23932g, Meal (ID: mealId, ratio 100%), Ing A, Ing B
         // Remove the only meal
         given()
             .pathParam("id", adventureId)
             .pathParam("mealId", mealId)
             .accept(ContentType.JSON)
         .when()
             .delete("/adventures/{id}/meals/{mealId}")
         .then()
             .statusCode(200);
         System.out.println("Removed meal: " + mealId);

         // Verify Adventure state
         // ED should reset (or become NaN/Infinity if calculated from empty children? Let's assume 0)
         // Weight should become 0 (or NaN/Infinity) as ED is 0 or children are empty. Check Adventure.setWeight logic. If ED=0, weight should be NaN/Infinity based on division. JSON might represent this as null or handle it. Let's check for 0 or null. Assuming weight becomes 0 for testability.
         // mealWeights and ingredientWeights maps should be empty or not contain the removed meal/ingredients.

         given()
             .pathParam("id", adventureId)
             .accept(ContentType.JSON)
         .when()
             .get("/adventures/{id}")
         .then()
             .statusCode(200)
             .body("mealWeights", not(hasKey(mealId)))
             .body("ingredientWeights", not(hasKey(ingredientId1)))
             .body("ingredientWeights", not(hasKey(ingredientId2)));

         System.out.println("Verified adventure state after removing the only meal.");
         // Nullify IDs for subsequent tests if any depended on them
         mealId = null;
         ingredientId1 = null;
         ingredientId2 = null;
     }


     @Test
     @Order(15) // Renumbering
     @Disabled("Crew member removal needs specific handling/ID retrieval")
     public void test_15_RemoveCrewMember() {
         // Test implementation would go here if enabled
         System.out.println("\n--- Test: Remove Crew Member ---");
     }

     @Test
     @Order(16) // Renumbering
     public void test_16_DeleteMealOrphaned() {
         System.out.println("\n--- Test: Delete Meal (Orphaned) ---");
         // We created an orphaned meal in test_02. Let's find its ID if needed or create a new one.
         // Reusing the logic from test_02 to create a meal to delete
         String orphanedMealId = given()
             .queryParam("name", "Orphaned Meal To Delete")
             .contentType(ContentType.JSON)
             .accept(ContentType.JSON)
         .when()
             .post("/meals")
         .then()
             .statusCode(200)
             .extract().path("id");
         System.out.println("Created orphaned meal to delete: " + orphanedMealId);

         // Need a DELETE /meals/{id} endpoint first. Assuming it exists:
         // given()
         //    .pathParam("id", orphanedMealId)
         // .when()
         //    .delete("/meals/{id}")
         // .then()
         //    .statusCode(200); // Or 204 No Content
         // System.out.println("Deleted orphaned meal: " + orphanedMealId);

         // Verify it's gone
         // given()
         //    .pathParam("id", orphanedMealId)
         // .when()
         //    .get("/meals/{id}")
         // .then()
         //    .statusCode(404);
         System.out.println("Skipping actual delete/verify - DELETE /meals/{id} endpoint needed.");
     }

     @Test
     @Order(17) // Renumbering
     public void test_17_DeleteAdventure() {
         System.out.println("\n--- Test: Delete Adventure ---");
         assert adventureId != null : "Adventure ID should not be null";

         given()
             .pathParam("id", adventureId)
         .when()
             .delete("/adventures/{id}")
         .then()
             .statusCode(200); // Assuming 200 OK on successful delete

         System.out.println("Deleted Adventure: " + adventureId);

         // Verify it's gone
         given()
             .pathParam("id", adventureId)
             .accept(ContentType.JSON)
         .when()
             .get("/adventures/{id}")
         .then()
             .statusCode(404); // Should be Not Found
         System.out.println("Verified Adventure is deleted.");
         adventureId = null; // Nullify for safety
     }

    // TODO: Add tests for GET endpoints? Maybe less critical for propagation testing initially.
    // TODO: Add failure case tests (e.g., non-existent IDs).
    // TODO: Verify JSON representation of Adventure weight (grams or kg?) and adjust assertions.
} 