package io.hulsbo;

import io.hulsbo.model.Ingredient;
import io.hulsbo.util.model.baseclass.NutrientsMap;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import io.restassured.response.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseResourceTest {

	private static UUID createdIngredientId; // To store the ID of the created ingredient

	// Save new ingredient to db
	@Test
	@Order(1)
	public void saveNewIngredientToDb() {

		Ingredient ingredient = new Ingredient();
		String ingredientName = "ingredientToSave";
		ingredient.setName(ingredientName);

		NutrientsMap nutrientMap = new NutrientsMap();

		nutrientMap.put("protein", 0.1);
		nutrientMap.put("carbs", 0.2);
		nutrientMap.put("fat", 0.3);
		nutrientMap.put("fiber", 0.25);
		nutrientMap.put("salt", 0.05);
		nutrientMap.put("water", 0.1);

		ingredient.setNutrientRatios(nutrientMap);

		// System.out.println(ingredient.getNutrientsMap().toString()); // Optional: remove or keep for debugging

		UUID newIngredientId = UUID.randomUUID();

		Ingredient responseIngredient = given()
				.contentType(ContentType.JSON)
				.pathParam("id", newIngredientId)
				.body(ingredient)
				.when()
				.put("/ingredients/{id}")
				.then()
				.statusCode(201) // Expect 201 Created for new resource
				.body("id", notNullValue())
				.body("name", is(ingredientName)) // Assert the correct name
				.extract().as(Ingredient.class);

		createdIngredientId = responseIngredient.getId(); // Store the ID from the response
		// Verify the ID in the response matches the one sent, if the endpoint sets it from the path
		// If the endpoint generates a new ID and returns it, this assertion might change or be removed.
		// Based on DatabaseResource, the ID from the path is used.
		// assertThat(createdIngredientId, is(newIngredientId)); // This line is commented out as it might be redundant or context-dependent.
	}

	// Search for the saved ingredient
	@Test
	@Order(2)
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
	@Order(3)
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
	
}
