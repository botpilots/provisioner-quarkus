package io.hulsbo.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.hulsbo.model.Adventure;
import io.hulsbo.model.Manager;
import io.hulsbo.model.Meal;
import java.util.UUID;
import io.quarkus.logging.Log;

import java.util.List;

@Path("/adventures")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdventureResource {

	@POST
	public Response createAdventure(@QueryParam("name") String name) {
		Log.infof("POST /adventures?name=%s - Entering createAdventure", name);
		Adventure adventure = new Adventure();
		if (name != null && !name.isEmpty()) {
			adventure.setName(name);
		}
		Log.infof("POST /adventures - Success creating Adventure ID: %s", adventure.getId());
		return Response.ok(adventure).build();
	}

	@DELETE
	@Path("/{id}")
	public Response removeAdventure(@PathParam("id") String id) {
		UUID uuid = UUID.fromString(id);
		Log.infof("DELETE /adventures/%s - Entering removeAdventure", uuid);
		// TODO: Add check if object exists before removal for better logging?
		Manager.removeBaseClassObject(uuid);
		Log.infof("DELETE /adventures/%s - Success", uuid);
		return Response.ok().build();
	}

	@GET
	public Response getAllAdventures() {
		Log.infof("GET /adventures - Entering getAllAdventures");
		List<Adventure> adventures = Manager.getAllAdventures();
		Log.infof("GET /adventures - Success returning %d adventures", adventures.size());
		return Response.ok(adventures).build();
	}

	@GET
	@Path("/{id}")
	public Response getAdventure(@PathParam("id") UUID id) {
		Log.infof("GET /adventures/%s - Entering getAdventure", id);
		Adventure adventure = (Adventure) Manager.getBaseClass(id);
		if (adventure == null) {
			Log.warnf("GET /adventures/%s - Failed: Adventure not found.", id);
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		Log.infof("GET /adventures/%s - Success", id);
		return Response.ok(adventure).build();
	}

	// NOTE: Crewmembers are not added to the index currently.
	@POST
	@Path("/{id}/crew")
	public Response addCrewMember(
			@PathParam("id") UUID adventureId,
			@QueryParam("name") String name,
			@QueryParam("age") int age,
			@QueryParam("height") int height,
			@QueryParam("weight") int weight,
			@QueryParam("gender") String gender,
			@QueryParam("activity") String activity,
			@QueryParam("strategy") String strategy) {
		Log.infof("POST /adventures/%s/crew?name=%s... - Entering addCrewMember", adventureId, name);

		Adventure adventure = (Adventure) Manager.getBaseClass(adventureId);
		if (adventure == null) {
			Log.warnf("POST /adventures/%s/crew - Failed: Adventure not found.", adventureId);
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		try {
		    // Assuming putCrewMember returns the ID or some identifier of the new member, or throws - Correction: it's void
		    adventure.putCrewMember(name, age, height, weight, gender, activity, strategy);
		    Log.infof("POST /adventures/%s/crew - Success adding crew member '%s'", adventureId, name); // Log name instead
		    return Response.ok(adventure).build(); // Returning whole adventure might be excessive?
		} catch (Exception e) {
		    Log.errorf(e, "POST /adventures/%s/crew - Failed adding crew member: %s", adventureId, e.getMessage());
            // Consider a more specific error response if possible
            return Response.status(Response.Status.BAD_REQUEST).entity("Failed to add crew member: " + e.getMessage()).build();
		}
	}

	@DELETE
	@Path("/{adventureId}/crew/{crewId}")
	public Response removeCrewMember(
		@PathParam("adventureId") String adventureIdStr,
		@PathParam("crewId") String crewIdStr) {
		UUID adventureId = UUID.fromString(adventureIdStr);
		UUID crewId = UUID.fromString(crewIdStr);
		Log.infof("DELETE /adventures/%s/crew/%s - Entering removeCrewMember", adventureId, crewId);
		Adventure adventure = (Adventure) Manager.getBaseClass(adventureId);
		if (adventure == null) {
			Log.warnf("DELETE /adventures/%s/crew/%s - Failed: Adventure not found.", adventureId, crewId);
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		try {
		    adventure.removeCrewMember(crewId);
		    Log.infof("DELETE /adventures/%s/crew/%s - Success", adventureId, crewId);
		    return Response.ok().build();
		} catch (IllegalArgumentException e) {
            Log.warnf("DELETE /adventures/%s/crew/%s - Failed: %s", adventureId, crewId, e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (Exception e) {
            Log.errorf(e, "DELETE /adventures/%s/crew/%s - Unexpected error: %s", adventureId, crewId, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to remove crew member: " + e.getMessage()).build();
        }
	}

	@PUT
	@Path("/{id}/days")
	public Response setDays(@PathParam("id") UUID id, @QueryParam("days") int days) {
		Log.infof("PUT /adventures/%s/days?days=%d - Entering setDays", id, days);
		Adventure adventure = (Adventure) Manager.getBaseClass(id);
		if (adventure == null) {
			Log.warnf("PUT /adventures/%s/days - Failed: Adventure not found.", id);
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		try {
		    adventure.setDays(days);
		    Log.infof("PUT /adventures/%s/days?days=%d - Success", id, days);
		    return Response.ok(adventure).build();
		} catch (IllegalArgumentException e) {
            Log.warnf("PUT /adventures/%s/days - Failed: %s", id, e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            Log.errorf(e, "PUT /adventures/%s/days - Unexpected error: %s", id, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to set days: " + e.getMessage()).build();
        }
	}

	@POST
	@Path("/{id}/meals")
	public Response addMeal(
			@PathParam("id") UUID adventureId,
			@QueryParam("name") String name) {
		Log.infof("POST /adventures/%s/meals?name=%s - Entering addMeal", adventureId, name);

		Adventure adventure = (Adventure) Manager.getBaseClass(adventureId);
		if (adventure == null) {
			Log.warnf("POST /adventures/%s/meals - Failed: Adventure not found.", adventureId);
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		Meal meal = new Meal();
		if (name != null && !name.isEmpty()) {
			meal.setName(name);
		}

		UUID mealId = adventure.putChild(meal);
		Log.infof("POST /adventures/%s/meals - Success adding Meal ID: %s", adventureId, mealId);
		return Response.ok(mealId).build();
	}

	@DELETE
	@Path("/{id}/meals/{mealId}")
	public Response removeMeal(
			@PathParam("id") UUID adventureId,
			@PathParam("mealId") UUID mealId) {
		Log.infof("DELETE /adventures/%s/meals/%s - Entering removeMeal", adventureId, mealId);

		Adventure adventure = (Adventure) Manager.getBaseClass(adventureId);
		if (adventure == null) {
			Log.warnf("DELETE /adventures/%s/meals/%s - Failed: Adventure not found.", adventureId, mealId);
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		try {
			adventure.removeChild(mealId);
			Log.infof("DELETE /adventures/%s/meals/%s - Success", adventureId, mealId);
			return Response.ok().build();
		} catch (IllegalArgumentException e) {
			Log.warnf("DELETE /adventures/%s/meals/%s - Failed: %s", adventureId, mealId, e.getMessage());
			return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
		}
	}

	@GET
	@Path("/{id}/info")
	public Response getAdventureInfo(@PathParam("id") UUID id) {
		Log.infof("GET /adventures/%s/info - Entering getAdventureInfo", id);
		Adventure adventure = (Adventure) Manager.getBaseClass(id);
		if (adventure == null) {
			Log.warnf("GET /adventures/%s/info - Failed: Adventure not found.", id);
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		adventure.getInfo(); // Assuming this logs to console itself
		Log.infof("GET /adventures/%s/info - Success (info printed to console)", id);
		return Response.ok("Adventure info printed to console").build();
	}

}