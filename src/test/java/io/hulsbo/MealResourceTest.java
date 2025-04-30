package io.hulsbo;

import io.quarkus.test.junit.QuarkusTest;
import io.hulsbo.model.Meal;
import io.hulsbo.util.model.SafeID;
import io.hulsbo.util.model.MeasurementUnit;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.BeforeEach;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
public class MealResourceTest {

	private static String mealId;
	private static String ingredientId;
	private SafeID testMealId;
	private SafeID testIngredientId;
	private static final double TOLERANCE = 1e-6; // Tolerance for float comparisons in assertions

	@BeforeEach
	void setUp() {
		// Ensure a clean state for each test (if Manager is static and retains state)
		// Note: If Manager persists data across test runs in a real scenario,
		// more robust cleanup (e.g., database clearing) might be needed.
		// For this example, we assume Manager operates in-memory per test run or is reset.

		// Create a Meal
		Meal testMeal = new Meal();
		testMeal.setName("Test Meal");
		testMealId = testMeal.getId(); // Manager.register happens in BaseClass constructor

		// Create an Ingredient and add it to the Meal
		io.hulsbo.model.Ingredient testIngredient = new io.hulsbo.model.Ingredient();
		testIngredient.setName("Test Ingredient");
		// Initialize nutrients to a known state (e.g., 100% protein) for predictable tests
		testIngredient.setNutrientRatio("protein", 1.0);
		testIngredient.normalizeNutrientRatiosAndPropagate(); // Finalize initial state
		
		testIngredientId = testMeal.putChild(testIngredient); // This also calls Manager.register
	}

	@Test
	@Order(1)
	public void testCreateMeal() {
		Response response = given()
				.contentType(ContentType.JSON)
				.when()
				.queryParam("name", "Test Meal")
				.post("/meals")
				.then()
				.statusCode(200)
				.body("id", notNullValue())
				.body("name", is("Test Meal"))
				.extract().response();

		// Extract the meal ID for subsequent tests
		mealId = response.jsonPath().getString("id");
	}

	@Test
	@Order(2)
	public void testGetMeal() {
		given()
				.contentType(ContentType.JSON)
				.when()
				.get("/meals/{id}", mealId)
				.then()
				.statusCode(200)
				.body("name", is("Test Meal"));
	}

	@Test
	@Order(3)
	public void testGetMealNotFound() {
		// Use a non-existent ID
		String nonExistentId = SafeID.randomSafeID().toString();

		given()
				.contentType(ContentType.JSON)
				.when()
				.get("/meals/{id}", nonExistentId)
				.then()
				.statusCode(404);
	}

	@Test
	@Order(4)
	public void testAddIngredient() {
		Response response = given()
				.contentType(ContentType.JSON)
				.when()
				.queryParam("name", "Test Ingredient")
				.post("/meals/{id}/ingredients", mealId)
				.then()
				.statusCode(200)
				.extract().response();

		// Extract the ingredient ID for subsequent tests
		ingredientId = response.asString().replaceAll("\"", "");
	}

	@Test
	@Order(5)
	public void testModifyIngredientWeight() {
		given()
				.contentType(ContentType.JSON)
				.when()
				.queryParam("weight", 100.0)
				.put("/meals/{mealId}/ingredients/{ingredientId}", mealId, ingredientId)
				.then()
				.statusCode(200);
	}

	@Test
	@Order(6)
	public void testGetMealInfo() {
		given()
				.contentType(ContentType.JSON)
				.when()
				.get("/meals/{id}/info", mealId)
				.then()
				.statusCode(200)
				.body(containsString("Meal info printed to console"));
	}

	@Test
	public void testModifyIngredientWeightOnly() {
		double newWeight = 150.5;

		String responseBody = given()
			.queryParam("weight", newWeight)
		.when()
			.put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), testIngredientId.toString())
		.then()
			.statusCode(200)
			.contentType(ContentType.JSON)
            // .log().body() // Keep logging off for now
            .extract().body().asString();
            
        // Correct the assertion path
        given().body(responseBody)
            .then().body("id", equalTo(testIngredientId.toString()));

		// Verify backend state (remains the same)
		Meal meal = (Meal) io.hulsbo.model.Manager.getBaseClass(testMealId);
		Double actualWeight = meal.getChildMap().get(testIngredientId).getRecipeWeight();
		assertNotNull(actualWeight, "Recipe weight should not be null after update");
		assertEquals(newWeight, actualWeight, TOLERANCE);
	}

	@Test
	public void testModifyIngredientSingleNutrient() {
		double newProtein = 0.8;
        // Because only protein is set, normalization will scale it to 1.0
        double expectedNormalizedProtein = 1.0; 
        double expectedNormalizedFat = 0.0;

		Response response = given()
			.queryParam("protein", newProtein) // Send only protein update (0.8)
		.when()
			.put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), testIngredientId.toString())
		.then()
			.statusCode(200)
			.contentType(ContentType.JSON)
			.body("id", equalTo(testIngredientId.toString())) 
            .extract().response();
            
        // Extract and assert numeric values using jsonPath().getDouble()
        // Check against EXPECTED NORMALIZED values in the response
        double actualProtein = response.jsonPath().getDouble("nutrientsMap.protein");
        double actualFat = response.jsonPath().getDouble("nutrientsMap.fat");
        assertEquals(expectedNormalizedProtein, actualProtein, TOLERANCE); // Expect 1.0
        assertEquals(expectedNormalizedFat, actualFat, TOLERANCE);       // Expect 0.0

		// Verify backend state (check against expected normalized values)
		io.hulsbo.model.Ingredient ingredient = (io.hulsbo.model.Ingredient) io.hulsbo.model.Manager.getBaseClass(testIngredientId);
		assertEquals(expectedNormalizedProtein, ingredient.getNutrientsMap().get("protein"), TOLERANCE);
		assertEquals(expectedNormalizedFat, ingredient.getNutrientsMap().get("fat"), TOLERANCE);
		assertEquals(1.0, ingredient.getNutrientsMap().values().stream().mapToDouble(Double::doubleValue).sum(), TOLERANCE);
	}

	@Test
	public void testModifyIngredientMultipleNutrients() {
		double newProtein = 0.5;
		double newFat = 0.3;
		double newCarbs = 0.2;

		Response response = given()
			.queryParam("protein", newProtein)
			.queryParam("fat", newFat)
			.queryParam("carbs", newCarbs)
		.when()
			.put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), testIngredientId.toString())
		.then()
			.statusCode(200)
			.contentType(ContentType.JSON)
			.body("id", equalTo(testIngredientId.toString())) 
            .extract().response();
            
        // Extract and assert numeric values using jsonPath().getDouble()
        double actualProtein = response.jsonPath().getDouble("nutrientsMap.protein");
        double actualFat = response.jsonPath().getDouble("nutrientsMap.fat");
        double actualCarbs = response.jsonPath().getDouble("nutrientsMap.carbs");
        assertEquals(newProtein, actualProtein, TOLERANCE);
        assertEquals(newFat, actualFat, TOLERANCE);
        assertEquals(newCarbs, actualCarbs, TOLERANCE);

		// Verify backend state (remains same)
		io.hulsbo.model.Ingredient ingredient = (io.hulsbo.model.Ingredient) io.hulsbo.model.Manager.getBaseClass(testIngredientId);
		assertEquals(newProtein, ingredient.getNutrientsMap().get("protein"), TOLERANCE);
		assertEquals(newFat, ingredient.getNutrientsMap().get("fat"), TOLERANCE);
		assertEquals(newCarbs, ingredient.getNutrientsMap().get("carbs"), TOLERANCE);
		assertEquals(1.0, ingredient.getNutrientsMap().values().stream().mapToDouble(Double::doubleValue).sum(), TOLERANCE);
	}
	
	@Test
	public void testModifyIngredientNutrientsAndWeight() {
		double newWeight = 99.0;
		double newProtein = 0.25;
		double newFiber = 0.75;

		Response response = given()
			.queryParam("weight", newWeight)
			.queryParam("protein", newProtein)
			.queryParam("fiber", newFiber) 
		.when()
			.put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), testIngredientId.toString())
		.then()
			.statusCode(200)
			.contentType(ContentType.JSON)
			.body("id", equalTo(testIngredientId.toString())) 
            .extract().response();

        // Extract and assert numeric values using jsonPath().getDouble()
        double actualProtein = response.jsonPath().getDouble("nutrientsMap.protein");
        double actualFiber = response.jsonPath().getDouble("nutrientsMap.fiber");
        assertEquals(newProtein, actualProtein, TOLERANCE);
        assertEquals(newFiber, actualFiber, TOLERANCE);

		// Verify backend state (Nutrients - remains same)
		io.hulsbo.model.Ingredient ingredient = (io.hulsbo.model.Ingredient) io.hulsbo.model.Manager.getBaseClass(testIngredientId);
		assertEquals(newProtein, ingredient.getNutrientsMap().get("protein"), TOLERANCE);
		assertEquals(newFiber, ingredient.getNutrientsMap().get("fiber"), TOLERANCE);
		assertEquals(1.0, ingredient.getNutrientsMap().values().stream().mapToDouble(Double::doubleValue).sum(), TOLERANCE);
		
		// Verify backend state (Weight - remains same)
		Meal meal = (Meal) io.hulsbo.model.Manager.getBaseClass(testMealId);
		Double actualWeight = meal.getChildMap().get(testIngredientId).getRecipeWeight();
		assertNotNull(actualWeight, "Recipe weight should not be null after update");
		assertEquals(newWeight, actualWeight, TOLERANCE);
	}

	@Test
	public void testModifyIngredientNutrientSumExceeds100() {
        // This test now verifies the stealing logic works via the API
        // Initial state (from setUp) is protein = 1.0
        double newProtein = 0.7; // Requested value
        double newFat = 0.4;     // Requested value
        
        // Expected outcome after sequential updates and final normalization:
        // 1. Set protein=0.7 -> State: protein=0.7, fat=0.0 (sum=0.7)
        // 2. Set fat=0.4 -> Steal 0.4 from protein (0.7). Scale=(0.7-0.4)/0.7 = 0.3/0.7. 
        //                 -> State: protein=0.7*(0.3/0.7)=0.3, fat=0.4 (sum=0.7)
        // 3. Normalize -> Scale factor = 1.0 / 0.7
        //                 -> Final: protein=0.3*(1/0.7), fat=0.4*(1/0.7)
        double expectedProtein = 0.3 / 0.7; // Approx 0.4285714
        double expectedFat = 0.4 / 0.7;     // Approx 0.5714286
        
		given()
			.queryParam("protein", newProtein)
			.queryParam("fat", newFat) // Causes sum > 1 initially
		.when()
			.put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), testIngredientId.toString())
		.then()
			.statusCode(400) // Expect 400 Bad Request now
            .contentType(ContentType.JSON)
            .body("message", containsString("Updating nutrients would exceed 100%")); // Check the message from batch validation

        // Verify backend state hasn't changed (it should be whatever it was before the call)
        io.hulsbo.model.Ingredient ingredient = (io.hulsbo.model.Ingredient) io.hulsbo.model.Manager.getBaseClass(testIngredientId);
		assertEquals(1.0, ingredient.getNutrientsMap().get("protein"), TOLERANCE, "Original protein ratio should be retained after failed update");
	}

	@Test
	public void testModifyIngredientNegativeNutrient() {
		given()
			.queryParam("protein", -0.1)
		.when()
			.put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), testIngredientId.toString())
		.then()
			.statusCode(400)
            .contentType(ContentType.JSON) // Expect JSON error response
            .body("message", containsString("Invalid nutrient value for key 'protein'")); // Check message within JSON
	}

	@Test
	public void testModifyIngredientNegativeWeight() {
		given()
			.queryParam("weight", -50.0)
		.when()
			.put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), testIngredientId.toString())
		.then()
			.statusCode(400) // Bad Request
			 .body(containsString("Weight cannot be negative"));
	}

	@Test
	public void testModifyIngredientInvalidMealId() {
		SafeID invalidMealId = SafeID.randomSafeID();
		given()
			.queryParam("weight", 100.0)
		.when()
			.put("/meals/{mealId}/ingredients/{ingredientId}", invalidMealId.toString(), testIngredientId.toString())
		.then()
			.statusCode(404) // Not Found
			.body(containsString("Meal not found"));
	}
	
	@Test
	public void testModifyIngredientInvalidIngredientId() {
		SafeID invalidIngredientId = SafeID.randomSafeID();
		given()
			.queryParam("weight", 100.0)
		.when()
			.put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), invalidIngredientId.toString())
		.then()
			.statusCode(404) // Not Found
			.body(containsString("Ingredient not found"));
	}
	
	 @Test
	public void testModifyIngredientNotInMeal() {
		 // Create another meal and ingredient
		 Meal otherMeal = new Meal();
		 otherMeal.setName("Other Meal");
		 SafeID otherMealId = otherMeal.getId();

		 io.hulsbo.model.Ingredient otherIngredient = new io.hulsbo.model.Ingredient();
		 otherIngredient.setName("Other Ingredient");
		 SafeID otherIngredientId = otherMeal.putChild(otherIngredient);
		
		 // Try to modify otherIngredient using testMealId
		given()
			.queryParam("weight", 100.0)
		.when()
			.put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), otherIngredientId.toString())
		.then()
			.statusCode(400) // Bad Request
			.body(containsString("Ingredient does not belong to the specified meal"));
	}
	
	 @Test
	public void testModifyNutrientNormalization() {
		 // Start with protein = 1.0
		 // Send protein = 0.5, fat = 0.3 -> Sum = 0.8
		 // Expect normalization: protein = 0.5/0.8 = 0.625, fat = 0.3/0.8 = 0.375
		double newProtein = 0.5;
		double newFat = 0.3;
		double expectedProtein = 0.625;
		double expectedFat = 0.375;
		
		 Response response = given()
			.queryParam("protein", newProtein)
			.queryParam("fat", newFat)
		.when()
			.put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), testIngredientId.toString())
		.then()
			.statusCode(200)
			.contentType(ContentType.JSON)
            .extract().response();
            
        // Extract and assert numeric values using jsonPath().getDouble()
        double actualProtein = response.jsonPath().getDouble("nutrientsMap.protein");
        double actualFat = response.jsonPath().getDouble("nutrientsMap.fat");
        assertEquals(expectedProtein, actualProtein, TOLERANCE);
        assertEquals(expectedFat, actualFat, TOLERANCE);
			
		 // Verify backend state (remains same)
		io.hulsbo.model.Ingredient ingredient = (io.hulsbo.model.Ingredient) io.hulsbo.model.Manager.getBaseClass(testIngredientId);
		assertEquals(expectedProtein, ingredient.getNutrientsMap().get("protein"), TOLERANCE);
		assertEquals(expectedFat, ingredient.getNutrientsMap().get("fat"), TOLERANCE);
		assertEquals(1.0, ingredient.getNutrientsMap().values().stream().mapToDouble(Double::doubleValue).sum(), TOLERANCE);
	}

	// --- New Tests for MeasurementUnit and pcsWeight --- 

	@Test
	public void testModifyIngredientMeasurementUnitOnly() {
		String newUnit = MeasurementUnit.KILOGRAM.name();

		given()
			.queryParam("measurementUnit", newUnit)
		.when()
			.put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), testIngredientId.toString())
		.then()
			.statusCode(200)
			.contentType(ContentType.JSON)
			.body("id", equalTo(testIngredientId.toString()))
			.body("measurementUnit", equalTo(newUnit));

		// Verify backend state
		io.hulsbo.model.Ingredient ingredient = (io.hulsbo.model.Ingredient) io.hulsbo.model.Manager.getBaseClass(testIngredientId);
		assertEquals(MeasurementUnit.KILOGRAM, ingredient.getMeasurementUnit());
	}

	@Test
	public void testModifyIngredientPcsWeightOnly_Set() {
		Double newPcsWeight = 15.5;

		given()
			.queryParam("pcsWeight", newPcsWeight)
		.when()
			.put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), testIngredientId.toString())
		.then()
			.statusCode(200)
			.contentType(ContentType.JSON)
			.body("id", equalTo(testIngredientId.toString()))
			.body("pcsWeight", is((float) newPcsWeight.doubleValue())); // Cast primitive double to float

		// Verify backend state
		io.hulsbo.model.Ingredient ingredient = (io.hulsbo.model.Ingredient) io.hulsbo.model.Manager.getBaseClass(testIngredientId);
		assertEquals(newPcsWeight, ingredient.getPcsWeight(), TOLERANCE);
	}

	@Test
	public void testModifyIngredientPcsWeightOnly_UnsetWithNull() {
		// First, set a value
		double initialPcsWeight = 20.0;
		given()
			.queryParam("pcsWeight", initialPcsWeight)
			.put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), testIngredientId.toString())
			.then().statusCode(200);

		// Now, unset it by sending pcsWeight=null (or empty)
		given()
			.queryParam("pcsWeight", "null") // Test explicit string "null"
		.when()
			.put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), testIngredientId.toString())
		.then()
			.statusCode(200)
			.contentType(ContentType.JSON)
			.body("id", equalTo(testIngredientId.toString()))
			.body("pcsWeight", nullValue()); // Check that it's null in the response

		// Verify backend state
		io.hulsbo.model.Ingredient ingredient = (io.hulsbo.model.Ingredient) io.hulsbo.model.Manager.getBaseClass(testIngredientId);
		assertEquals(null, ingredient.getPcsWeight());
	}
	
	@Test
	public void testModifyIngredientPcsWeightOnly_UnsetWithEmpty() {
	    // First, set a value
	    double initialPcsWeight = 25.0;
	    given()
	        .queryParam("pcsWeight", initialPcsWeight)
	        .put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), testIngredientId.toString())
	        .then().statusCode(200);

	    // Now, unset it by sending an empty pcsWeight parameter
	    given()
	        .queryParam("pcsWeight", "") // Test empty string
	    .when()
	        .put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), testIngredientId.toString())
	    .then()
	        .statusCode(200)
	        .contentType(ContentType.JSON)
	        .body("id", equalTo(testIngredientId.toString()))
	        .body("pcsWeight", nullValue()); // Should be unset

	    // Verify backend state
	    io.hulsbo.model.Ingredient ingredient = (io.hulsbo.model.Ingredient) io.hulsbo.model.Manager.getBaseClass(testIngredientId);
	    assertEquals(null, ingredient.getPcsWeight());
	}

	@Test
	public void testModifyIngredientPcsWeightInvalid_Zero() {
		given()
			.queryParam("pcsWeight", 0.0)
		.when()
			.put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), testIngredientId.toString())
		.then()
			.statusCode(400)
			.body("message", equalTo("pcsWeight must be greater than 0."));
	}

	@Test
	public void testModifyIngredientPcsWeightInvalid_Negative() {
		given()
			.queryParam("pcsWeight", -10.0)
		.when()
			.put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), testIngredientId.toString())
		.then()
			.statusCode(400)
			.body("message", equalTo("pcsWeight must be greater than 0."));
	}
	
	@Test
	public void testModifyIngredientPcsWeightInvalid_NotANumber() {
	    given()
	        .queryParam("pcsWeight", "abc")
	    .when()
	        .put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), testIngredientId.toString())
	    .then()
	        .statusCode(400) // Expect Bad Request due to number format exception
	        .body("message", containsString("Invalid number format for pcsWeight"));
	}

	@Test
	public void testModifyIngredientMeasurementUnitInvalid() {
		given()
			.queryParam("measurementUnit", "INVALID_UNIT")
		.when()
			.put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), testIngredientId.toString())
		.then()
			.statusCode(400) // Expect Bad Request due to invalid enum value
			.body("message", containsString("Invalid measurementUnit value"));
	}

	@Test
	public void testModifyIngredientWeightAndPcsWeight() {
		double newWeight = 200.0;
		double newPcsWeight = 50.0;

		given()
			.queryParam("weight", newWeight)
			.queryParam("pcsWeight", newPcsWeight)
		.when()
			.put("/meals/{mealId}/ingredients/{ingredientId}", testMealId.toString(), testIngredientId.toString())
		.then()
			.statusCode(200)
			.contentType(ContentType.JSON)
			.body("pcsWeight", is((float) newPcsWeight)); // Cast primitive double to float

		// Verify backend state
		io.hulsbo.model.Ingredient ingredient = (io.hulsbo.model.Ingredient) io.hulsbo.model.Manager.getBaseClass(testIngredientId);
		Meal meal = (Meal) io.hulsbo.model.Manager.getBaseClass(testMealId);
		assertEquals(newPcsWeight, ingredient.getPcsWeight(), TOLERANCE);
		assertEquals(newWeight, meal.getChildMap().get(testIngredientId).getRecipeWeight(), TOLERANCE);
	}

	// --- End New Tests --- 

}