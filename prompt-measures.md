
### Specification: Ingredient Measurement Units and Piece Weight

**Overview:**

This specification outlines the necessary frontend and backend changes to introduce selectable measurement units (e.g., grams, pieces, cups) for ingredients within the application. It enables conversion based on a per-ingredient piece weight (`pcsWeight`) and defines the UI interactions for managing these units, focusing initially on "Grams" (default) and "Pieces" (PCS). The implementation utilizes a standard dropdown button for unit selection and dynamically adjusts the `step` attribute of the weight input field for unit-based increments, and a dedicated button for setting/unsetting the piece weight specifically for the PCS unit.

---

### Frontend Requirements

1.  **Modify Ingredient Modal Layout:**
    *   **Inline Weight Components:** Display the weight-related controls inline in the following order:
        *   **Weight Input (Grams):** A standard number input field. **This field always displays and accepts the ingredient's weight value in grams.** (Example value: `[ 150 ]`). The `step` attribute of this input will be dynamically updated (see point 4).
        *   **Converted Value Label:** A text label displayed immediately to the right of the weight input. Shows the gram value converted into the selected unit or specific state text (see below).
        *   **Unit Split Dropdown Button:** A **Split Dropdown Button component** located to the right of the label. This component consists of two visually connected parts:
            *   **Main Action Button:** Displays the selected unit text (e.g., "grams", "pcs", "kg"). Clicking this part triggers the gram increment action.
            *   **Dropdown Trigger:** Displays a dropdown arrow icon (`▾`). Clicking this part opens the unit selection dropdown menu.
        *   **Unit Dropdown Button:** A **Standard Dropdown Button** located to the right of the label. It displays the selected unit text (e.g., "grams", "pcs") and a dropdown arrow icon (`▾`). Clicking the button opens the unit selection dropdown menu.
        *   **PCS Set/Unset Button:** (Conditional) A *second*, separate button displayed immediately to the right of the Unit Split Dropdown Button. **This button is only visible when `PCS` is the selected unit.** Its text will be either "Set" or "Unset" based on the ingredient's state.
        *   **PCS Set/Unset Button:** (Conditional) A *second*, separate button displayed immediately to the right of the **Unit Dropdown Button**. **This button is only visible when `PCS` is the selected unit.** Its text will be either "Set" or "Unset" based on the ingredient's state.
        *   **Unit Selector (Dropdown Menu):** A dropdown menu (e.g., `<ul>` or `<div>`) associated with the **Dropdown Trigger** part of the split button. It lists all available `MeasurementUnit` enums fetched from the backend (`/configuration/measurement-units`).
        *   **Unit Selector (Dropdown Menu):** A dropdown menu (e.g., `<ul>` or `<div>`) associated with the **Unit Dropdown Button**. It lists all available `MeasurementUnit` enums fetched from the backend (`/configuration/measurement-units`).
    *   **Visual Examples:**
        *   GRAM selected: `Weight: [ 150 ] [ display: none ] [ grams | ▾ ]`
        *   PCS selected, `pcsWeight` unset: `Weight: [ 150 ] [ - ] [ pcs | ▾ ] [ Set ]`
        *   PCS selected, `pcsWeight` set to same value as weight input: `Weight: [ 150 ] [ 1 ] [ pcs | ▾ ] [ Unset ]`
        *   PCS selected, `pcsWeight` set to other value (100) than weight input: `Weight: [ 150 ] [ 1.5 ] [ pcs ▾ ] [ Set ]`

2.  **Unit Selection:**
    *   Clicking the **Dropdown Trigger** part (`▾`) of the split button opens the **Unit Selector (Dropdown Menu)**.
    *   Clicking the **Unit Dropdown Button** (`[ unit ▾ ]`) opens the **Unit Selector (Dropdown Menu)**.
    *   The options within the dropdown menu are populated by fetching the list of measurement units from the `GET /configuration/measurement-units` endpoint. This endpoint should return both the enum values and conversion logic for each unit.
    *   Selecting a unit from the menu updates the text on the **Main Action Button** part of the split button (e.g., to "pcs", "kg") and updates the **Converted Value Label** according to the new unit's conversion logic. This selection does *not* change the value in the **Weight Input (Grams)** field directly.
    *   Selecting a unit from the menu updates the text on the **Unit Dropdown Button** (e.g., to "pcs ▾", "kg ▾"), updates the **Converted Value Label** according to the new unit's conversion logic, and updates the `step` attribute of the **Weight Input (Grams)** field (see point 4). This selection does *not* change the *value* in the **Weight Input (Grams)** field directly.

3.  **Converted Value Label Behavior:**
    *   **Default Unit (GRAM):** When `GRAM` is selected, the label is **empty** and not displayed (none).
    *   **PCS Specific:**
        *   If `PCS` is selected and `pcsWeight` *is null*: Label displays "-" or can be empty (state is indicated by the "Set" button).
        *   If `PCS` is selected, `pcsWeight` *is set*: Label displays the calculated number of pieces based on the current gram input (e.g., "1.5", "0.0", "2.0"). No unit suffix (like "pcs") is displayed in the label itself.
    *   **Other Units:** Displays the current **Weight Input (Grams)** value converted into the selected unit (e.g., "0.2", "1.5").

4.  **Unit Split Dropdown Button Action:**
    *   **Main Action Button Text:**
        *   When `GRAM` is selected: "grams".
        *   For other units: The abbreviation (e.g., "pcs", "kg", "cup").
    *   **Main Action Button Click (Increment):** Clicking this part **increments** the value in the **Weight Input (Grams)** field. The increment amount corresponds to the gram equivalent of *one* of the currently selected units:
        *   `GRAM`: Adds `1` gram.
        *   `PCS`: If `pcsWeight` is set, adds the value of `pcsWeight` to the gram input. If `pcsWeight` is null, this click action and button should be disabled and have no effect.
        *   `KILOGRAM`: Adds `1000` grams.
        *   Other units (e.g., `CUP`, `TEASPOON`): Adds the predefined gram equivalent of that one unit.
    *   **Dropdown Trigger Click:** Clicking the **Dropdown Trigger** part (`▾`) opens/closes the **Unit Selector (Dropdown Menu)**.

5.  **Weight Input Stepping Behavior:**
    *   The `step` attribute of the **Weight Input (Grams)** field must be dynamically updated whenever the selected unit changes.
    *   The value of the `step` attribute corresponds to the gram equivalent of *one* of the currently selected units:
        *   `GRAM`: `step="1"`
        *   `PCS`: If `pcsWeight` is set and > 0, `step` should be set to the value of `pcsWeight`. If `pcsWeight` is null or <= 0, disable stepping.
        *   `KILOGRAM`: `step="1000"`
        *   Other units (e.g., `CUP`, `TEASPOON`): `step` should be set to the predefined gram equivalent of that one unit (fetched from the backend configuration).
    *   This allows users to use the browser's built-in number input controls (up/down arrows) to increment/decrement the gram weight by the equivalent of one selected unit.

6.  **PCS Set/Unset Button Behavior:**
    *   **Visibility:** This button is rendered and visible *only* when `PCS` is the currently selected unit in the dropdown.
    *   **State and Action:**
        *   If `pcsWeight` *is null*: The button displays the text **"Set"**. Clicking it triggers a minimal backend `PUT` request to the `modifyIngredient` endpoint, sending *only* the current value from the **Weight Input (Grams)** as the `pcsWeight` query parameter. This sets the piece weight for the ingredient.
        *   If `pcsWeight` *is set to some value* and the same value is present in the **Weight Input (Grams)** field: The button displays the text **"Unset"**. Clicking it triggers a minimal backend `PUT` request to `modifyIngredient` to **unset** `pcsWeight` (e.g., by sending `pcsWeight=null`).

7.  **PCS Unit Interaction Summary:**
    *   User selects `PCS` from the dropdown menu via the `▾` trigger.
    *   The Converted Value Label shows "-", the split button shows `[ pcs | ▾ ]`, and the separate `[ Set ]` button appears.
    *   The Converted Value Label shows "-", the dropdown button shows `[ pcs ▾ ]`, and the separate `[ Set ]` button appears. The **Weight Input (Grams)** `step` attribute is updated (e.g., to `step="1"` initially).
    *   User enters the desired gram weight for one piece (e.g., `150`) into the **Weight Input (Grams)** field.
    *   User clicks the `[ Set ]` button. A backend request (`?pcsWeight=150`) is sent.
    *   On success, the `[ Set ]` button changes to `[ Unset ]` and the Converted Value Label updates to show the piece count based on the input (e.g., "1.0" if input is still 150). The **Weight Input (Grams)** `step` attribute is updated to `150`.
    *   User changes the **Weight Input (Grams)** to `300`. The label updates to "2.0". The `[ Unset ]` button becomes `[ Set ]`. The `step` attribute remains `150`.
    *   User clicks the `[ pcs ]` part of the split button. The **Weight Input (Grams)** increments by `pcsWeight` (becomes `450` if `pcsWeight` was 150). The label updates accordingly ("3.0").
    *   User uses the up/down arrows on the **Weight Input (Grams)** field. The value increments/decrements by `pcsWeight` (the `step` value, e.g., 150). The Converted Value Label updates accordingly (e.g., "3.0" if incrementing from 300).
    *   User changes the **Weight Input (Grams)** to `0`. The label shows "0.0".

8.  **Minimal Backend Requests:**
    *   The frontend must be implemented to send partial `PUT` requests to the `modifyIngredient` endpoint containing *only* the `pcsWeight` parameter when the user clicks the "Set/Unset" button for the PCS unit.

---

### Backend Requirements

1.  **`MeasurementUnit` Enum:**
    *   **Definition:** Create a public enum named `MeasurementUnit`.
    *   **Package:** `io.hulsbo.util.model`
    *   **Values:** Include at least: `GRAM`, `PCS`, `KILOGRAM`, `TEASPOON`, `TABLESPOON`, `CUP`, `MILLILITER`, `LITER`. `GRAM` should be considered the default.
    *   **Location:** `src/main/java/io/hulsbo/util/model/MeasurementUnit.java`

2.  **`Ingredient` Class Modifications:**
    *   **Add `measurementUnit` Field:**
        *   Add `private MeasurementUnit measurementUnit = MeasurementUnit.GRAM;`
        *   Include standard getter (`getMeasurementUnit()`) and setter (`setMeasurementUnit(MeasurementUnit unit)`).
    *   **Add `pcsWeight` Field:**
        *   Add `private Double pcsWeight;` (nullable). Represents the weight in grams of a single "piece" of this ingredient.
        *   Include standard getter (`getPcsWeight()`) and setter (`setPcsWeight(Double weight)`).
    *   **Add `density` Field:**
        *   Add `private Double density = 1.0;` (defaults to 1.0 for water). Represents the density in g/ml for volume conversions.
        *   Include standard getter (`getDensity()`) and setter (`setDensity(Double density)`).
    *   **Location:** `src/main/java/io/hulsbo/model/Ingredient.java`

3.  **`MealResource.modifyIngredient` Endpoint Updates:**
    *   **Add Query Parameters:** Add the following optional `@QueryParam` annotations to the `modifyIngredient` method signature:
        ```java
        @QueryParam("measurementUnit") MeasurementUnit measurementUnit,
        @QueryParam("pcsWeight") Double pcsWeight
        ```
        *(Place these alongside existing query parameters like `weight`, `protein`, etc.)*
    *   **Partial Update Logic:**
        *   The endpoint must be able to process requests that contain *only* the `pcsWeight` parameter (and path params). In this case, it should update *only* the `pcsWeight` field of the target `Ingredient`.
        *   The endpoint must also handle requests containing *only* the `measurementUnit` parameter, updating only that field.
        *   If `pcsWeight` is provided in the request:
            *   Update `ingredient.pcsWeight`.
            *   Handle potential `null` values if the frontend sends `null` explicitly to unset the value. If `pcsWeight` is sent with a value of <= 0 treat as invalid with bad request response and message "pcsWeight must be greater than 0", `null` for unsetting).
        *   If `measurementUnit` is provided (always provided in both the partial request and the full request when clicking update button):
            *   Update `ingredient.measurementUnit`.
    *   **Error Handling:**
        *   Validate that provided `pcsWeight` values (if not null) are positive (`> 0`). Return a `BAD_REQUEST` status if not.
        *   The framework (Quarkus/JAX-RS) should handle invalid `measurementUnit` string values automatically, but ensure appropriate error responses are generated.
    *   **Location:** `src/main/java/io/hulsbo/MealResource.java`

4.  **Expose Enum Options:**
    *   **Create `ConfigurationResource`:** Create a new JAX-RS resource class `ConfigurationResource` if one doesn't exist.
    *   **Add Endpoint:** Implement a `GET` endpoint `/configuration/measurement-units` in this resource.
    *   **Response:** This endpoint should return a JSON array containing the string representations of all available `MeasurementUnit` enum values (e.g., `["GRAM", "PCS", "KILOGRAM", ...]`).
        ```java
        @Path("/configuration")
        @Produces(MediaType.APPLICATION_JSON)
        public class ConfigurationResource {

            @GET
            @Path("/measurement-units")
            public Response getMeasurementUnits() {
                return Response.ok(MeasurementUnit.values()).build();
                // Or: return Response.ok(Arrays.stream(MeasurementUnit.values())
                //                           .map(Enum::name)
                //                           .collect(Collectors.toList())).build();
            }
        }
        ```
    *   **Location:** `src/main/java/io/hulsbo/ConfigurationResource.java` (or similar appropriate resource class).

5.  **Conversion Logic (Scope Note):**
    *   This specification requires storing `pcsWeight` and `measurementUnit`.
    *   The backend will always treat the weight value in grams and the frontend will do the conversion using the unit configuration sent from backend and the fields pcsWeight and density of the ingredient subject to modification. 
    *   The measurementUnit field is only used to determine the default unit at open of the ingredient modal.

---
