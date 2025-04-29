package io.hulsbo;

import io.quarkus.test.junit.QuarkusTest;
import io.hulsbo.util.model.SafeID;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
public class AdventureMealIntegrationTest {

	private static String adventureId;
	private static String mealId;
	private static String ingredientId;

	@Test
	@Order(1)
	public void testCreateAdventure() {
		Response response = given()
				.contentType(ContentType.JSON)
				.when()
				.queryParam("name", "Integration Test Adventure")
				.post("/adventures")
				.then()
				.statusCode(200)
				.body("id", notNullValue())
				.extract().response();

		adventureId = response.jsonPath().getString("id");
	}

	@Test
	@Order(2)
	public void testAddMealToAdventure() {
		Response response = given()
				.contentType(ContentType.JSON)
				.when()
				.queryParam("name", "Integration Test Meal")
				.post("/adventures/{id}/meals", adventureId)
				.then()
				.statusCode(200)
				.extract().response();

		mealId = response.asString().replaceAll("\"", "");
	}

	@Test
	@Order(3)
	public void testRetrieveMeal() {
		given()
				.contentType(ContentType.JSON)
				.when()
				.get("/meals/{id}", mealId)
				.then()
				.statusCode(200)
				.body("name", is("Integration Test Meal"));
	}

	@Test
	@Order(4)
	public void testAddIngredientToMeal() {
		Response response = given()
				.contentType(ContentType.JSON)
				.when()
				.queryParam("name", "Integration Test Ingredient")
				.post("/meals/{id}/ingredients", mealId)
				.then()
				.statusCode(200)
				.extract().response();

		ingredientId = response.asString().replaceAll("\"", "");
	}

	@Test
	@Order(5)
	public void testModifyIngredientWeight() {
		given()
				.contentType(ContentType.JSON)
				.when()
				.queryParam("weight", 200.0)
				.put("/meals/{mealId}/ingredients/{ingredientId}", mealId, ingredientId)
				.then()
				.statusCode(200);
	}

	@Test
	@Order(6)
	public void testAddCrewMemberToAdventure() {
		given()
				.contentType(ContentType.JSON)
				.when()
				.queryParam("name", "Integration Test Crew")
				.queryParam("age", 25)
				.queryParam("height", 175)
				.queryParam("weight", 70)
				.queryParam("gender", "female")
				.queryParam("activity", "HEAVY")
				.queryParam("strategy", "harris_benedict_original")
				.post("/adventures/{id}/crew", adventureId)
				.then()
				.statusCode(200);
	}

	@Test
	@Order(7)
	public void testSetAdventureDays() {
		given()
				.contentType(ContentType.JSON)
				.when()
				.queryParam("days", 3)
				.put("/adventures/{id}/days", adventureId)
				.then()
				.statusCode(200);
	}

	@Test
	@Order(8)
	public void testEndToEndFlow() {
		// Verify that the adventure data is complete and contains everything we've
		// added
		given()
				.contentType(ContentType.JSON)
				.when()
				.get("/adventures/{id}", adventureId)
				.then()
				.statusCode(200)
				.body("name", is("Integration Test Adventure"));

		// Verify that the meal data is correct
		given()
				.contentType(ContentType.JSON)
				.when()
				.get("/meals/{id}", mealId)
				.then()
				.statusCode(200)
				.body("name", is("Integration Test Meal"));
	}
}