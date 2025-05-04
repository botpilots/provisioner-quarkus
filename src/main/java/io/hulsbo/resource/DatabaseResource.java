package io.hulsbo.resource;

import io.hulsbo.entities.IngredientEntity;
import io.hulsbo.mapper.IngredientMapper;
import io.hulsbo.model.Ingredient; // Assuming this is the POJO
import io.hulsbo.dto.IngredientSearchResultDTO;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.annotations.jaxrs.QueryParam; // If using RESTEasy specific, else use jakarta.ws.rs.QueryParam
import org.jboss.logging.Logger; // Import Logger

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Path("/ingredients")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DatabaseResource {

    private static final Logger LOG = Logger.getLogger(DatabaseResource.class); // Add Logger instance

    @Inject
    IngredientMapper ingredientMapper;

    // --- Search Endpoint --- (Previously created)
    @GET
    @Path("/search")
    public Response searchIngredients(@QueryParam("query") String query) {
        if (query == null || query.trim().isEmpty()) {
            return Response.ok(List.of()).build();
        }

        String searchPattern = "%" + query.toLowerCase() + "%";
        String jpqlQuery = "lower(name) like ?1"; // The query string for Panache

        // 1. Find matching entities
        List<IngredientEntity> matchingEntities = IngredientEntity
                .find(jpqlQuery, searchPattern)
                .list();

        // 2. Map entities to DTOs
        List<IngredientSearchResultDTO> results = matchingEntities.stream()
                .map(entity -> new IngredientSearchResultDTO(entity.id, entity.name))
                .toList();

        return Response.ok(results).build();
    }

    // --- Load Endpoint (GET /ingredients/{id}) ---
    @GET
    @Path("/{id}")
    public Response getIngredientById(@PathParam("id") UUID id) {
        Optional<IngredientEntity> optionalEntity = IngredientEntity.findByIdOptional(id);
		LOG.info("---- ENTERING getIngredientById with id: " + id + " ----");
        if (optionalEntity.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        IngredientEntity entity = optionalEntity.get();
		LOG.info("---- entity: " + entity.id + " ----");
        Ingredient domainObject = ingredientMapper.toDomain(entity);

		LOG.info("---- EXITING getIngredientById with id: " + id + " and domainObject: " + domainObject.getId() + " ----");

        // It might be better to return a DTO instead of the full Ingredient domain object
        // depending on what the frontend needs.
        return Response.ok(domainObject).build();
    }

    // --- Save/Update Endpoint (PUT /ingredients/{id}) ---
    @PUT
    @Path("/{id}")
    @Transactional
    public Response createOrUpdateIngredient(@PathParam("id") UUID id, Ingredient ingredientFromBody, @Context UriInfo uriInfo) {

        IngredientEntity existingEntity = IngredientEntity.findById(id);

        if (existingEntity != null) {
            // --- Update Existing Ingredient ---
            ingredientMapper.updateEntity(ingredientFromBody, existingEntity);
            // Panache handles the update commit automatically due to @Transactional
            Ingredient updatedDomainObject = ingredientMapper.toDomain(existingEntity); // Map back for response
            return Response.ok(updatedDomainObject).build();
        } else {
            // --- Create New Ingredient ---
            IngredientEntity newEntity = ingredientMapper.toEntity(ingredientFromBody);
            newEntity.id = id; // Set the ID provided in the path
            newEntity.persist();

            Ingredient createdDomainObject = ingredientMapper.toDomain(newEntity); // Map back for response
            URI createdUri = uriInfo.getAbsolutePathBuilder().path(id.toString()).build();
            return Response.created(createdUri).entity(createdDomainObject).build();
        }
    }
}
