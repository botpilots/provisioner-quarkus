package io.hulsbo;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.hulsbo.model.Adventure;
import io.hulsbo.model.Manager;
import io.hulsbo.model.Meal;
import io.hulsbo.util.model.SafeID;

import java.util.List;

@Path("/adventures")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdventureResource {

	@POST
	public Response createAdventure(@QueryParam("name") String name) {
		Adventure adventure = new Adventure();
		if (name != null && !name.isEmpty()) {
			adventure.setName(name);
		}
		return Response.ok(adventure).build();
	}

	@DELETE
	@Path("/{id}")
	public Response removeAdventure(@PathParam("id") String id) {
		Manager.removeBaseClassObject(SafeID.fromString(id));
		return Response.ok().build();
	}

	@GET
	public Response getAllAdventures() {
		List<Adventure> adventures = Manager.getAllAdventures();
		return Response.ok(adventures).build();
	}

	@GET
	@Path("/{id}")
	public Response getAdventure(@PathParam("id") SafeID id) {
		Adventure adventure = (Adventure) Manager.getBaseClass(id);
		if (adventure == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		return Response.ok(adventure).build();
	}

	// NOTE: Crewmembers are not added to the index currently.
	@POST
	@Path("/{id}/crew")
	public Response addCrewMember(
			@PathParam("id") SafeID adventureId,
			@QueryParam("name") String name,
			@QueryParam("age") int age,
			@QueryParam("height") int height,
			@QueryParam("weight") int weight,
			@QueryParam("gender") String gender,
			@QueryParam("activity") String activity,
			@QueryParam("strategy") String strategy) {

		Adventure adventure = (Adventure) Manager.getBaseClass(adventureId);
		if (adventure == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		adventure.putCrewMember(name, age, height, weight, gender, activity, strategy);
		return Response.ok(adventure).build();
	}

	@DELETE
	@Path("/{adventureId}/crew/{crewId}")
	public Response removeCrewMember(
		@PathParam("adventureId") String adventureId,
		@PathParam("crewId") String crewId) {
		Adventure adventure = (Adventure) Manager.getBaseClass(SafeID.fromString(adventureId));
		if (adventure == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		adventure.removeCrewMember(SafeID.fromString(crewId));
		return Response.ok().build();
	}

	@PUT
	@Path("/{id}/days")
	public Response setDays(@PathParam("id") SafeID id, @QueryParam("days") int days) {
		Adventure adventure = (Adventure) Manager.getBaseClass(id);
		if (adventure == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		adventure.setDays(days);
		return Response.ok(adventure).build();
	}

	@POST
	@Path("/{id}/meals")
	public Response addMeal(
			@PathParam("id") SafeID adventureId,
			@QueryParam("name") String name) {

		Adventure adventure = (Adventure) Manager.getBaseClass(adventureId);
		if (adventure == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		Meal meal = new Meal();
		if (name != null && !name.isEmpty()) {
			meal.setName(name);
		}

		SafeID mealId = adventure.putChild(meal);
		return Response.ok(mealId).build();
	}

	@DELETE
	@Path("/{id}/meals/{mealId}")
	public Response removeMeal(
			@PathParam("id") SafeID adventureId,
			@PathParam("mealId") SafeID mealId) {

		Adventure adventure = (Adventure) Manager.getBaseClass(adventureId);
		if (adventure == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		try {
			adventure.removeChild(mealId);
			return Response.ok().build();
		} catch (IllegalArgumentException e) {
			return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
		}
	}

	@GET
	@Path("/{id}/info")
	public Response getAdventureInfo(@PathParam("id") SafeID id) {
		Adventure adventure = (Adventure) Manager.getBaseClass(id);
		if (adventure == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		adventure.getInfo();
		return Response.ok("Adventure info printed to console").build();
	}
}