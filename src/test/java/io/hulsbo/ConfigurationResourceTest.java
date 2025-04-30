package io.hulsbo;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.hulsbo.util.model.MeasurementUnit;
import java.util.Arrays;

@QuarkusTest
public class ConfigurationResourceTest {

    @Test
    public void testGetMeasurementUnits() {
        int expectedSize = MeasurementUnit.values().length;

        given()
          .when().get("/configuration/measurement-units")
          .then()
             .statusCode(200)
             .contentType(ContentType.JSON)
             // Verify the response is a list of the correct size
             .body("$", hasSize(expectedSize))
             // Verify the structure and values of the first element (GRAM)
             .body("[0].name", equalTo("GRAM"))
             .body("[0].standardGrams", equalTo(1.0f)) // Use 1.0f for float comparison
             .body("[0].isVolume", equalTo(false))
             // Verify the structure and values of the second element (PCS), checking null handling
             .body("[1].name", equalTo("PCS"))
             .body("[1].standardGrams", nullValue()) // Check that standardGrams is null
             .body("[1].isVolume", equalTo(false));

             // We don't need to check every single element in detail,
             // verifying the size and a couple of key elements (including null handling)
             // is usually sufficient for this type of endpoint.
    }
} 