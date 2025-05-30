package io.hulsbo;

import io.hulsbo.entities.IngredientEntity;
import io.hulsbo.model.Ingredient;
import io.hulsbo.util.model.baseclass.NutrientsMap;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseResourceTest {

	// Use a fixed ID for testing, so that on a retry an update, instead of create, is done.
	final UUID createdIngredientId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

	// Helper used for testing saving and updating an ingredient.
	private void putIngredientToDb(double proteinRatio, int responseCode) {

		// create ingredient
		Ingredient ingredient = new Ingredient();
		String ingredientName = "ingredientToSave";
		ingredient.setName(ingredientName);

		// set some protein value
		NutrientsMap nutrientMap = new NutrientsMap();
		nutrientMap.put("protein", proteinRatio);
		ingredient.setNutrientRatios(nutrientMap);

		// make put request and validate with responseCode
		Ingredient responseIngredient = given()
				.contentType(ContentType.JSON)
				.pathParam("id", createdIngredientId)
				.body(ingredient)
				.when()
				.put("/ingredients/{id}")
				.then()
				.statusCode(responseCode)
				.body("id", is(createdIngredientId.toString())) // Assert the correct ID
				.body("name", is(ingredientName)) // Assert the correct name
				.extract().as(Ingredient.class);

		ingredient.setNutrientRatio("protein", 0.5);

	}

	// Save new ingredient to db
	@Test
	@Order(1)
	public void saveIngredientToDb() {

		// return early as success if ingredient already exists in db
		IngredientEntity existingEntity = IngredientEntity.findById(createdIngredientId);
		if (existingEntity != null) {
			Log.info("Test saveIngredientToDb() cannot be performed as there's already an ingredient with id " + createdIngredientId + ". Skipping test.");
			return;
		}

		// Expect 201 Created
		putIngredientToDb(0.5, 201);
	}

	// Update new ingredient to db
	@Test
	@Order(2)
	public void updateIngredientToDb() {
		 // Expect 200 OK
		 putIngredientToDb(0.20, 200);
	}

	// Search for the saved ingredient
	@Test
	@Order(3)
	public void searchForSavedIngredient() {
		given()
			.pathParam("id", createdIngredientId) // Use the stored ID
			.when()
			.get("/ingredients/{id}")
			.then()
			.statusCode(200)
			.body("id", is(createdIngredientId.toString())) // Assert the correct ID
			.body("name", is("ingredientToSave")); // Assert the correct name
	}

	// Load the saved ingredient
	@Test
	@Order(4)
	public void loadSavedIngredient() {
		given()
			.pathParam("id", createdIngredientId) // Use the stored ID
			.when()
			.get("/ingredients/{id}")
			.then()
			.statusCode(200)
			.body("id", is(createdIngredientId.toString())) // Assert the correct ID
			.body("name", is("ingredientToSave")); // Assert the correct name
	}

	// Delete the saved ingredient
	@Test
	@Order(5)
	public void deleteSavedIngredient() throws Exception {

		IngredientEntity existingEntity = IngredientEntity.findById(createdIngredientId);

		if (existingEntity == null) {
			Log.error("Test deleteSavedIngredient() cannot be performed as there's no ingredient to delete. Run saveIngredientToDb() first or whole test module instead.");
			throw new Exception("Nothing to delete at beginning of test. Run saveIngredientToDb() first or whole test module instead.");
		}

		// Delete the ingredient
		given()
			.pathParam("id", createdIngredientId)
			.when()
			.delete("/ingredients/{id}")
			.then()
			.statusCode(204); // Expect 204 No Content for successful deletion
		
		// Then verify it's no longer available
		given()
			.pathParam("id", createdIngredientId)
			.when()
			.delete("/ingredients/{id}")
			.then()
			.statusCode(404); // Expect 404 Not Found since it was deleted
	}
	
}
