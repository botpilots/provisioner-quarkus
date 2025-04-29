package io.hulsbo;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.hulsbo.util.model.SafeID;
import io.hulsbo.model.BaseClass;
import io.hulsbo.model.Meal;
import io.hulsbo.model.Ingredient;
import io.hulsbo.model.Manager;

import java.util.Map;
import java.util.HashMap;

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
	public Response modifyIngredient(
			@PathParam("mealId") SafeID mealId,
			@PathParam("ingredientId") SafeID ingredientId,
			@QueryParam("weight") Double weight,
			@QueryParam("protein") Double protein,
			@QueryParam("fat") Double fat,
			@QueryParam("carbs") Double carbs,
			@QueryParam("water") Double water,
			@QueryParam("fiber") Double fiber,
			@QueryParam("salt") Double salt) {
		try {
			Meal meal = (Meal) Manager.getBaseClass(mealId);
			if (meal == null) {
				Map<String, String> errorMap = Map.of("message", "Meal not found.");
				return Response.status(Response.Status.NOT_FOUND).entity(errorMap).build();
			}

			BaseClass baseIngredient = Manager.getBaseClass(ingredientId);
			if (baseIngredient == null) {
				Map<String, String> errorMap = Map.of("message", "Ingredient not found.");
				return Response.status(Response.Status.NOT_FOUND).entity(errorMap).build();
			}

			if (!(baseIngredient instanceof Ingredient)) {
				Map<String, String> errorMap = Map.of("message", "Provided ID does not belong to an Ingredient.");
				return Response.status(Response.Status.BAD_REQUEST).entity(errorMap).build();
			}
			Ingredient ingredient = (Ingredient) baseIngredient;

			if (meal.getChildMap().values().stream().noneMatch(cw -> cw.getChild().getId().equals(ingredientId))) {
				Map<String, String> errorMap = Map.of("message", "Ingredient does not belong to the specified meal.");
				return Response.status(Response.Status.BAD_REQUEST).entity(errorMap).build();
			}

			// Create a map to hold nutrient updates
			Map<String, Double> nutrientUpdates = new HashMap<>();
			if (protein != null) { nutrientUpdates.put("protein", protein); }
			if (fat != null) { nutrientUpdates.put("fat", fat); }
			if (carbs != null) { nutrientUpdates.put("carbs", carbs); }
			if (water != null) { nutrientUpdates.put("water", water); }
			if (fiber != null) { nutrientUpdates.put("fiber", fiber); }
			if (salt != null) { nutrientUpdates.put("salt", salt); }

			boolean nutrientsModified = !nutrientUpdates.isEmpty();

			try {
				// Apply nutrient updates in batch if any were provided
				if (nutrientsModified) {
					ingredient.setNutrientRatios(nutrientUpdates);
				}
			} catch (IllegalArgumentException e) {
				// Include current nutrient state in the error response
				Map<String, Object> errorMap = new HashMap<>();
				errorMap.put("message", e.getMessage());
				errorMap.put("currentNutrients", ingredient.getNutrientsMap()); // Add current map
				return Response.status(Response.Status.BAD_REQUEST).entity(errorMap).build();
			}

			if (nutrientsModified) {
				ingredient.normalizeNutrientRatiosAndPropagate();
			}

			if (weight != null) {
				if (weight < 0) {
					Map<String, String> errorMap = Map.of("message", "Weight cannot be negative.");
					return Response.status(Response.Status.BAD_REQUEST).entity(errorMap).build();
				}
				try {
					meal.modifyWeightOfIngredient(ingredientId, weight);
				} catch (IllegalArgumentException e) {
					Map<String, String> errorMap = Map.of("message", e.getMessage());
					return Response.status(Response.Status.BAD_REQUEST).entity(errorMap).build();
				}
			}

			return Response.ok(ingredient).build();
		} catch (Exception e) {
			Map<String, String> errorMap = Map.of("message", "An unexpected server error occurred: " + e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorMap).build();
		}
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