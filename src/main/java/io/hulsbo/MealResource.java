package io.hulsbo;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.hulsbo.util.model.SafeID;

import io.hulsbo.model.Meal;
import io.hulsbo.model.Ingredient;
import io.hulsbo.model.Manager;

@Path("/meals")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MealResource {

	@POST
	public Response createMeal(@QueryParam("name") String name) {
		Meal meal = new Meal();
		meal.setName(name);
		return Response.ok(meal).build();
	}

	@GET
	@Path("/{id}")
	public Response getMeal(@PathParam("id") SafeID id) {
		Meal meal = (Meal) Manager.getBaseClass(id);
		if (meal == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		return Response.ok(meal).build();
	}

	@POST
	@Path("/{id}/ingredients")
	public Response addIngredient(@PathParam("id") SafeID mealId, @QueryParam("name") String name) {
		Meal meal = (Meal) Manager.getBaseClass(mealId);
		if (meal == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		Ingredient ingredient = new Ingredient();
		ingredient.setName(name);
		SafeID ingredientId = meal.putChild(ingredient);
		return Response.ok(ingredientId).build();
	}

	@DELETE
	@Path("/{mealId}/ingredients/{ingredientId}")
	public Response removeIngredient(
			@PathParam("mealId") SafeID mealId,
			@PathParam("ingredientId") SafeID ingredientId) {
		Meal meal = (Meal) Manager.getBaseClass(mealId);
		if (meal == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		try {
			meal.removeChild(ingredientId);
			return Response.ok().build();
		} catch (IllegalArgumentException e) {
			return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
		}
	}

	@PUT
	@Path("/{mealId}/ingredients/{ingredientId}")
	public Response modifyIngredientWeight(
			@PathParam("mealId") SafeID mealId,
			@PathParam("ingredientId") SafeID ingredientId,
			@QueryParam("weight") double weight) {
		Meal meal = (Meal) Manager.getBaseClass(mealId);
		if (meal == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		meal.modifyWeightOfIngredient(ingredientId, weight);
		return Response.ok().build();
	}

	@GET
	@Path("/{id}/info")
	public Response getMealInfo(@PathParam("id") SafeID id) {
		Meal meal = (Meal) Manager.getBaseClass(id);
		if (meal == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		meal.getInfo();
		return Response.ok("Meal info printed to console").build();
	}
}