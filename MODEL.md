# Provisioner Data Model Explanation

This document explains the structure and interaction of the core data models used in the Provisioner application: `Adventure`, `Meal`, and `Ingredient`.

## Core Concepts

The model is designed around a hierarchical structure where an `Adventure` consists of `Meal`s, and each `Meal` consists of `Ingredient`s. This hierarchy allows for calculating aggregate nutritional information and weights.

### 1. Base Class (`BaseClass.java`)

This abstract class is the foundation for `Adventure`, `Meal`, and `Ingredient`. It provides common functionality:

*   **Identification:** `id` (SafeID), `name` (String), `creationTime` (OffsetDateTime).
*   **Hierarchy:**
    *   `childMap`: A `Map<SafeID, ChildWrapper>` storing direct children (Meals for an Adventure, Ingredients for a Meal). `ChildWrapper` holds the child object, its ratio relative to siblings, and its recipe weight (used primarily by Meal/Ingredient).
    *   `parents`: A `Set<SafeID>` storing the IDs of parent objects that contain this instance in their `childMap`.
    *   `nameIndex`: A `Map<String, SafeID>` for quick lookup of children by name.
*   **Nutritional Data:**
    *   `nutrientsMap`: A `NutrientsMap` (effectively `Map<String, Double>`) storing the ratio of core macronutrients (carbs, protein, fat).
    *   `energyDensity`: Calculated caloric density (kcal/kg) based on `nutrientsMap`.
*   **Core Methods:**
    *   `putChild()` / `removeChild()`: Manage the `childMap` and update parent/child links.
    *   `setNutrientsMapAndWeights()`: Calculates `nutrientsMap` and `energyDensity` based on children's data and their ratios/weights. (Overridden in subclasses for specific behavior).
    *   `updateAndPropagate()`: The core method for ensuring data consistency up the hierarchy (see Propagation section).
    *   `addParent()` / `removeParent()`: Used by `putChild`/`removeChild` to maintain the `parents` set.

### 2. Ingredient (`Ingredient.java`)

*   Represents a basic food item.
*   Inherits from `BaseClass`.
*   Typically has base nutritional values defined (although currently, this seems to be implicitly derived from its `childMap`, which might be an area for review/simplification as Ingredients shouldn't have children).
*   Its `nutrientsMap` defines its contribution to a `Meal`.

### 3. Meal (`Meal.java`)

*   Represents a collection of `Ingredient`s.
*   Inherits from `BaseClass`.
*   **Children:** Its `childMap` contains `Ingredient` objects.
*   **Ratio Meaning (Ingredient within Meal):** The `ratio` stored in the `ChildWrapper` for an Ingredient represents its **weight proportion relative to the total weight of all ingredients within that specific Meal recipe**. It is automatically recalculated based on the `recipeWeight` of each ingredient whenever an ingredient's weight is modified (`modifyWeightOfIngredient`). The sum of Ingredient ratios within a Meal always equals 1.0.
*   **Calculations:**
    *   `nutrientsMap` and `energyDensity` are calculated based on the weighted average of its child `Ingredient`s' nutrients and their *derived ratios* (which stem from their `recipeWeight`).
    *   `modifyWeightOfIngredient()`: Allows changing the absolute weight of an ingredient (`recipeWeight`), which triggers recalculation of ratios for all ingredients in the meal and updates the meal's overall nutrient profile via `updateAndPropagate()`.

### 4. Adventure (`Adventure.java`)

*   Represents a trip or expedition requiring provisions.
*   Inherits from `BaseClass`.
*   **Children:** Its `childMap` contains `Meal` objects.
*   **Ratio Meaning (Meal within Adventure):** The `ratio` stored in the `ChildWrapper` for a Meal represents what **fraction of the total adventure's nutritional need (or energy/weight) is intended to come from that specific Meal**. For example, a ratio of 0.3 means 30% of the daily/total energy comes from this Meal. The sum of Meal ratios within an Adventure should ideally equal 1.0. This ratio is set when adding the Meal or can be modified later.
*   **Crew:**
    *   `crewMemberMap`: A `Map<SafeID, CrewMember>` storing the individuals on the adventure.
    *   `crewDailyKcalNeed`: Total calculated daily caloric need for the entire crew.
*   **Duration:** `days` (int).
*   **Calculations:**
    *   `nutrientsMap` and `energyDensity` are calculated based on the weighted average of its child `Meal`s' nutrients and their defined *ratios* (`ChildWrapper.ratio`).
    *   `weight`: Total calculated weight of food needed for the adventure (`crewDailyKcalNeed * days / energyDensity`).
    *   `childWeights`: A `Map<SafeID, Double>` storing the calculated *total weight* needed for each specific `Meal` based on the overall Adventure `weight` and the `Meal`'s ratio.
    *   `ingredientWeights`: A `Map<SafeID, Double>` storing the calculated *total weight* needed for each specific `Ingredient` across all meals, derived from the `childWeights` of the meals they belong to and the ingredient's ratio within that meal.
    *   Crew Kcal needs are calculated based on individual `CrewMember` attributes (age, height, weight, gender, activity level) using selectable strategies (Harris-Benedict, Mifflin-St Jeor).

### 5. Crew Member (`CrewMember.java`)

*   Represents a person participating in an `Adventure`.
*   Contains personal attributes (age, height, weight, gender, activity level).
*   Calculates `dailyKCalNeed` based on its attributes and a selected `KCalCalculationStrategy`.

## Calculation Flow & Propagation

A key aspect is how changes in lower-level objects (like an `Ingredient`'s properties or a `Meal`'s composition) affect the aggregate values in parent objects (`Meal` and `Adventure`).

**Why Propagation is Needed:**

If an `Ingredient`'s nutritional data changes, or the weight of an `Ingredient` in a `Meal` is modified, the `Meal`'s overall `nutrientsMap`, `energyDensity`, and potentially its ratio within an `Adventure` need recalculation. Subsequently, the `Adventure`'s `nutrientsMap`, `energyDensity`, total `weight`, and the calculated weights (`childWeights`, `ingredientWeights`) must also be updated.

**How Propagation Works (`updateAndPropagate`):**

1.  **Trigger:** When a method modifies an object's state in a way that impacts calculations (e.g., `Meal.modifyWeightOfIngredient`, `Adventure.setDays`, `BaseClass.putChild`, `BaseClass.removeChild`, `BaseClass.setName`), it calls `this.updateAndPropagate()` at the end.
2.  **Local Recalculation:** The `updateAndPropagate` method is overridden in `Adventure`, `Meal`, and `Ingredient`. The *first* thing each override does is perform the necessary recalculations specific to that class instance (e.g., `setNutrientsMapAndWeights`, `updateNameIndex`, potentially `setCrewDailyKcalNeed` or `setWeight` if not handled implicitly).
3.  **Recursive Call:** After local recalculation, the overridden method calls `super.updateAndPropagate()`.
4.  **Base Propagation:** The `BaseClass.updateAndPropagate()` implementation iterates through the `parents` set (containing IDs of objects that hold this instance as a child). For each parent ID, it fetches the parent object from the `Manager` and calls `parent.updateAndPropagate()`.
5.  **Chain Reaction:** This recursive call continues up the hierarchy, ensuring that each parent object recalculates its state based on the updated information from its children, until the top-level `Adventure` (which has no parents in this context) is updated.

This mechanism ensures that the data displayed or used for calculations at the `Adventure` level remains consistent with the underlying composition of its `Meal`s and `Ingredient`s.

## Calculable Properties Summary

*   **Adventure:**
    *   Total Crew Daily kCal Need
    *   Total Food Weight (kg) for the duration
    *   Overall Energy Density (kCal/kg)
    *   Overall Macronutrient Profile (%)
    *   Calculated Weight per Meal (kg)
    *   Calculated Weight per Ingredient (kg, summed across the adventure)
*   **Meal:**
    *   Energy Density (kCal/kg)
    *   Macronutrient Profile (%)
    *   Total Recipe Weight (g, sum of ingredient recipe weights)
*   **Ingredient:**
    *   Energy Density (kCal/kg)
    *   Macronutrient Profile (%)
*   **Crew Member:**
    *   Individual Daily kCal Need

## Object Model Field Summary

### `Adventure extends BaseClass`

*   `crewMemberMap: Map<SafeID, CrewMember>`
*   `crewDailyKcalNeed: double`
*   `days: int`
*   `ingredientWeights: Map<SafeID, Double>` (Calculated total weight per ingredient for the adventure)
*   *(Inherited)* `childMap: Map<SafeID, ChildWrapper>` (Contains Meals; `ChildWrapper.ratio` defines Meal proportion)
*   *(Inherited)* `childWeights: Map<SafeID, Double>` (Calculated total weight per meal for the adventure)
*   *(Inherited)* `weight: double` (Total calculated food weight)
*   *(Inherited)* `energyDensity: double`
*   *(Inherited)* `nutrientsMap: NutrientsMap`
*   *(Inherited)* `parents: Set<SafeID>`
*   *(Inherited)* `id: SafeID`, `name: String`, `creationTime: OffsetDateTime`, `nameIndex: Map<String, SafeID>`

### `Meal extends BaseClass`

*   *(Inherited)* `childMap: Map<SafeID, ChildWrapper>` (Contains Ingredients; `ChildWrapper.recipeWeight` is significant, `ChildWrapper.ratio` is derived from recipeWeights)
*   *(Inherited)* `weight: double` (Represents total recipe weight, sum of ingredient `recipeWeight`)
*   *(Inherited)* `energyDensity: double`
*   *(Inherited)* `nutrientsMap: NutrientsMap`
*   *(Inherited)* `parents: Set<SafeID>`
*   *(Inherited)* `id: SafeID`, `name: String`, `creationTime: OffsetDateTime`, `nameIndex: Map<String, SafeID>`
*   *(Inherited)* `childWeights: Map<SafeID, Double>` (Likely unused/zero for Meal)

### `Ingredient extends BaseClass`

*   *(Inherited)* `energyDensity: double` (Should represent base values)
*   *(Inherited)* `nutrientsMap: NutrientsMap` (Should represent base values)
*   *(Inherited)* `parents: Set<SafeID>`
*   *(Inherited)* `id: SafeID`, `name: String`, `creationTime: OffsetDateTime`
*   *(Inherited)* `childMap: Map<SafeID, ChildWrapper>` (Should be empty for Ingredient)
*   *(Inherited)* `nameIndex: Map<String, SafeID>` (Should be empty for Ingredient)
*   *(Inherited)* `weight: double` (Likely unused/zero for Ingredient)
*   *(Inherited)* `childWeights: Map<SafeID, Double>` (Should be empty for Ingredient)

### `CrewMember` (Not a BaseClass)

*   `id: SafeID`
*   `name: String`
*   `age: int`
*   `height: int` (cm)
*   `weight: int` (kg)
*   `gender: Gender` (Enum)
*   `activity: PhysicalActivity` (Enum)
*   `kCalCalculationStrategy: KCalCalculationStrategy` (Enum)
*   `dailyKCalNeed: int` (Calculated)
*   `creationTime: OffsetDateTime` 