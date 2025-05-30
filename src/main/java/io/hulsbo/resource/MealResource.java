package io.hulsbo.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;
import io.hulsbo.util.model.MeasurementUnit;
import io.hulsbo.model.BaseClass;
import io.hulsbo.model.Meal;
import io.hulsbo.model.Ingredient;
import io.hulsbo.model.Manager;
import io.quarkus.logging.Log;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

@Path("/meals")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MealResource {

	@POST
	public Response createMeal(@QueryParam("name") String name) {
		Log.infof("POST /meals?name=%s - Entering createMeal", name);
		Meal meal = new Meal();
		meal.setName(name);
		Log.infof("POST /meals?name=%s - Success creating Meal ID: %s", name, meal.getId());
		return Response.ok(meal).build();
	}

	@GET
	@Path("/{id}")
	public Response getMeal(@PathParam("id") UUID id) {
		Log.infof("GET /meals/%s - Entering getMeal", id);
		Meal meal = (Meal) Manager.getBaseClass(id);
		if (meal == null) {
			Log.warnf("GET /meals/%s - Failed: Meal not found.", id);
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		Log.infof("GET /meals/%s - Success", id);
		return Response.ok(meal).build();
	}

	@POST
	@Path("/{id}/ingredients")
	public Response addIngredient(@PathParam("id") UUID mealId, @QueryParam("name") String name, @QueryParam("ingredientId") UUID ingredientId) {
		Log.infof("POST /meals/%s/ingredient - Entering addIngredient with name=%s and id=%s", mealId, name, ingredientId);
		if (ingredientId != null && name != null) {
			Map<String, String> errorMap = Map.of("message", "Only one of 'id' or 'name' query parameter must be provided.");
			Log.errorf("POST /meals/%s/ingredient - Failed: %s", mealId, errorMap.get("message"));
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMap).build();
		}
		if (ingredientId == null && name == null) {
			Map<String, String> errorMap = Map.of("message", "Either 'id' (for an existing ingredient) or 'name' (for a new ingredient) query parameter must be provided and non-empty.");
			Log.errorf("POST /meals/%s/ingredient - Failed: %s", mealId, errorMap.get("message"));
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMap).build();
		}
		Meal meal = (Meal) Manager.getBaseClass(mealId);
		if (meal == null) {
			Map<String, String> errorMap = Map.of("message", "Meal with ID: \"" + mealId + "\" not found.");
			Log.errorf("POST /meals/%s/ingredient - Failed: %s", mealId, errorMap.get("message"));
			return Response.status(Response.Status.NOT_FOUND).entity(errorMap).build();
		}

		if (ingredientId != null) {
			Ingredient ingredient = (Ingredient) Manager.getBaseClass(ingredientId);
			ingredientId = meal.putChild(ingredient);
			Log.infof("POST /meals/%s/ingredients - Success adding existing Ingredient with ID: %s", mealId, ingredientId);
			return Response.ok(ingredientId).build();
		} else {
			Ingredient ingredient = new Ingredient();
			ingredient.setName(name);
			ingredientId = meal.putChild(ingredient);
			Log.infof("POST /meals/%s/ingredients - Success adding Ingredient ID: %s", mealId, ingredientId);
			return Response.ok(ingredientId).build();
		}
	}

	@DELETE
	@Path("/{mealId}/ingredients/{ingredientId}")
	public Response removeIngredient(
			@PathParam("mealId") UUID mealId,
			@PathParam("ingredientId") UUID ingredientId) {
		Log.infof("DELETE /meals/%s/ingredients/%s - Entering removeIngredient", mealId, ingredientId);
		Meal meal = (Meal) Manager.getBaseClass(mealId);
		if (meal == null) {
			Log.warnf("DELETE /meals/%s/ingredients/%s - Failed: Meal not found.", mealId, ingredientId);
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		try {
			meal.removeChild(ingredientId);
			Log.infof("DELETE /meals/%s/ingredients/%s - Success", mealId, ingredientId);
			return Response.ok().build();
		} catch (IllegalArgumentException e) {
			Log.warnf("DELETE /meals/%s/ingredients/%s - Failed: %s", mealId, ingredientId, e.getMessage());
			return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
		}
	}

	@PUT
	@Path("/{mealId}/ingredients/{ingredientId}")
	public Response modifyIngredient(
			@PathParam("mealId") UUID mealId,
			@PathParam("ingredientId") UUID ingredientId,
			@QueryParam("weight") Double weight,
			@QueryParam("protein") Double protein,
			@QueryParam("fat") Double fat,
			@QueryParam("carbs") Double carbs,
			@QueryParam("water") Double water,
			@QueryParam("fiber") Double fiber,
			@QueryParam("salt") Double salt,
			@Context UriInfo uriInfo) {

		String measurementUnitStr = uriInfo.getQueryParameters().getFirst("measurementUnit");
		List<String> pcsWeightValues = uriInfo.getQueryParameters().get("pcsWeight");
		boolean pcsWeightParamPresent = pcsWeightValues != null && !pcsWeightValues.isEmpty();
		String pcsWeightStr = pcsWeightParamPresent ? pcsWeightValues.get(0) : null;
		String densityStr = uriInfo.getQueryParameters().getFirst("density");

		Log.infof("PUT /meals/%s/ingredients/%s - Entering modifyIngredient (weight=%s, protein=%s, fat=%s, carbs=%s, water=%s, fiber=%s, salt=%s, measurementUnit=%s, pcsWeight=%s, density=%s)",
				mealId, ingredientId, weight, protein, fat, carbs, water, fiber, salt, measurementUnitStr, pcsWeightStr, densityStr);
		try {
			Meal meal = (Meal) Manager.getBaseClass(mealId);
			if (meal == null) {
				Map<String, String> errorMap = Map.of("message", "Meal not found.");
				Log.warnf("PUT /meals/%s/ingredients/%s - Failed: Meal not found.", mealId, ingredientId);
				return Response.status(Response.Status.NOT_FOUND).entity(errorMap).build();
			}

			BaseClass baseIngredient = Manager.getBaseClass(ingredientId);
			if (baseIngredient == null) {
				Map<String, String> errorMap = Map.of("message", "Ingredient not found.");
				Log.warnf("PUT /meals/%s/ingredients/%s - Failed: Ingredient not found.", mealId, ingredientId);
				return Response.status(Response.Status.NOT_FOUND).entity(errorMap).build();
			}

			if (!(baseIngredient instanceof Ingredient)) {
				Map<String, String> errorMap = Map.of("message", "Provided ID does not belong to an Ingredient.");
				Log.warnf("PUT /meals/%s/ingredients/%s - Failed: Provided ID is not an Ingredient.", mealId, ingredientId);
				return Response.status(Response.Status.BAD_REQUEST).entity(errorMap).build();
			}
			Ingredient ingredient = (Ingredient) baseIngredient;

			if (meal.getChildMap().values().stream().noneMatch(cw -> cw.getChild().getId().equals(ingredientId))) {
				Map<String, String> errorMap = Map.of("message", "Ingredient does not belong to the specified meal.");
				Log.warnf("PUT /meals/%s/ingredients/%s - Failed: Ingredient does not belong to meal.", mealId, ingredientId);
				return Response.status(Response.Status.BAD_REQUEST).entity(errorMap).build();
			}

			Map<String, Double> nutrientUpdates = new HashMap<>();
			if (protein != null) { nutrientUpdates.put("protein", protein); }
			if (fat != null) { nutrientUpdates.put("fat", fat); }
			if (carbs != null) { nutrientUpdates.put("carbs", carbs); }
			if (water != null) { nutrientUpdates.put("water", water); }
			if (fiber != null) { nutrientUpdates.put("fiber", fiber); }
			if (salt != null) { nutrientUpdates.put("salt", salt); }

			boolean nutrientsModified = !nutrientUpdates.isEmpty();
			boolean weightModified = weight != null;

			boolean measurementUnitModified = false;
			boolean pcsWeightModified = false;
			boolean densityModified = false;

			if (measurementUnitStr != null && !measurementUnitStr.trim().isEmpty()) {
				try {
					MeasurementUnit unit = MeasurementUnit.valueOf(measurementUnitStr.toUpperCase());
					if (ingredient.getMeasurementUnit() != unit) {
						ingredient.setMeasurementUnit(unit);
						measurementUnitModified = true;
					}
				} catch (IllegalArgumentException e) {
					Map<String, String> errorMap = Map.of("message", "Invalid measurementUnit value: " + measurementUnitStr);
					Log.warnf("PUT /meals/%s/ingredients/%s - Failed: Invalid measurementUnit (%s)", mealId, ingredientId, measurementUnitStr);
					return Response.status(Response.Status.BAD_REQUEST).entity(errorMap).build();
				}
			}

			if (pcsWeightParamPresent) {
				if (pcsWeightStr == null || pcsWeightStr.trim().isEmpty() || pcsWeightStr.equalsIgnoreCase("null")) {
					if (ingredient.getPcsWeight() != null) {
						ingredient.setPcsWeight(null);
						pcsWeightModified = true;
					}
				} else {
					try {
						Double pcsWeightValue = Double.parseDouble(pcsWeightStr);
						if (pcsWeightValue <= 0) {
							Map<String, String> errorMap = Map.of("message", "pcsWeight must be greater than 0.");
							Log.warnf("PUT /meals/%s/ingredients/%s - Failed: pcsWeight <= 0 (%s)", mealId, ingredientId, pcsWeightValue);
							return Response.status(Response.Status.BAD_REQUEST).entity(errorMap).build();
						}
						if (ingredient.getPcsWeight() == null || !ingredient.getPcsWeight().equals(pcsWeightValue)) {
							ingredient.setPcsWeight(pcsWeightValue);
							pcsWeightModified = true;
						}
					} catch (NumberFormatException e) {
						Map<String, String> errorMap = Map.of("message", "Invalid number format for pcsWeight: " + pcsWeightStr);
						Log.warnf("PUT /meals/%s/ingredients/%s - Failed: Invalid pcsWeight format (%s)", mealId, ingredientId, pcsWeightStr);
						return Response.status(Response.Status.BAD_REQUEST).entity(errorMap).build();
					}
				}
			}

			if (densityStr != null && !densityStr.trim().isEmpty()) {
				try {
					Double densityValue = Double.parseDouble(densityStr);
					if (densityValue <= 0) {
						Map<String, String> errorMap = Map.of("message", "Density must be greater than 0.");
						Log.warnf("PUT /meals/%s/ingredients/%s - Failed: Density <= 0 (%s)", mealId, ingredientId, densityValue);
						return Response.status(Response.Status.BAD_REQUEST).entity(errorMap).build();
					}
					if (ingredient.getDensity() == null || !ingredient.getDensity().equals(densityValue)) {
						ingredient.setDensity(densityValue);
						densityModified = true;
					}
				} catch (NumberFormatException e) {
					Map<String, String> errorMap = Map.of("message", "Invalid number format for density: " + densityStr);
					Log.warnf("PUT /meals/%s/ingredients/%s - Failed: Invalid density format (%s)", mealId, ingredientId, densityStr);
					return Response.status(Response.Status.BAD_REQUEST).entity(errorMap).build();
				}
			}

			try {
				if (nutrientsModified) {
					ingredient.setNutrientRatios(nutrientUpdates);
				}
			} catch (IllegalArgumentException e) {
				Map<String, String> errorMap = Map.of("message", e.getMessage());
				Log.warnf("PUT /meals/%s/ingredients/%s - Failed setting nutrients: %s", mealId, ingredientId, e.getMessage());
				return Response.status(Response.Status.BAD_REQUEST).entity(errorMap).build();
			}

			if (weightModified) {
				if (weight < 0) {
					Map<String, String> errorMap = Map.of("message", "Weight cannot be negative.");
					Log.warnf("PUT /meals/%s/ingredients/%s - Failed: Negative weight provided (%s).", mealId, ingredientId, weight);
					return Response.status(Response.Status.BAD_REQUEST).entity(errorMap).build();
				}
				try {
					meal.modifyWeightOfIngredient(ingredientId, weight);
				} catch (IllegalArgumentException e) {
					Map<String, String> errorMap = Map.of("message", e.getMessage());
					Log.warnf("PUT /meals/%s/ingredients/%s - Failed modifying weight: %s", mealId, ingredientId, e.getMessage());
					return Response.status(Response.Status.BAD_REQUEST).entity(errorMap).build();
				}
			}

			if (nutrientsModified) {
				ingredient.normalizeNutrientRatiosAndPropagate();
			} else if (weightModified) {
				meal.updateAndPropagate();
			}

			if (densityModified && !nutrientsModified && !weightModified) {
				ingredient.updateAndPropagate();
			}

			Log.infof("PUT /meals/%s/ingredients/%s - Success (unit: %s, pcsW: %s, density: %s)", mealId, ingredientId, ingredient.getMeasurementUnit(), ingredient.getPcsWeight(), ingredient.getDensity());
			return Response.ok(ingredient).build();
		} catch (Exception e) {
			Log.errorf(e, "PUT /meals/%s/ingredients/%s - Unexpected server error: %s", mealId, ingredientId, e.getMessage());
			Map<String, String> errorMap = Map.of("message", "An unexpected server error occurred: " + e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorMap).build();
		}
	}

	@GET
	@Path("/{id}/info")
	public Response getMealInfo(@PathParam("id") UUID id) {
		Log.infof("GET /meals/%s/info - Entering getMealInfo", id);
		Meal meal = (Meal) Manager.getBaseClass(id);
		if (meal == null) {
			Log.warnf("GET /meals/%s/info - Failed: Meal not found.", id);
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		meal.getInfo();
		Log.infof("GET /meals/%s/info - Success (info printed to console)", id);
		return Response.ok("Meal info printed to console").build();
	}
}