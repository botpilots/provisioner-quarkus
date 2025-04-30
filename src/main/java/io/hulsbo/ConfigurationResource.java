package io.hulsbo;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.hulsbo.util.model.MeasurementUnit;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Path("/configuration")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigurationResource {

    // Define a record to represent the structure for JSON serialization
    // Records automatically provide getters, constructor, equals, hashCode, toString
    public record MeasurementUnitInfo(String name, Double standardGrams, boolean isVolume, String description) {}

    @GET
    @Path("/measurement-units")
    public Response getMeasurementUnits() {
        // Map MeasurementUnit enum values to MeasurementUnitInfo records
        List<MeasurementUnitInfo> unitInfos = Arrays.stream(MeasurementUnit.values())
            .map(unit -> new MeasurementUnitInfo(
                unit.name(),
                unit.getStandardGrams(), // This is now allowed as the DTO field is Double (nullable)
                unit.isVolume(),
                unit.getDescription() // Add description field
            ))
            .collect(Collectors.toList());

        return Response.ok(unitInfos).build();
    }
} 