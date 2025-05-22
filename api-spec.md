# API Specification

## Base URL
All API endpoints are relative to the base URL of the application (default: http://localhost:8080).

## Content Types
- All endpoints accept and return JSON (`application/json`)

## Adventures API

### Create a new adventure
- **POST** `/adventures`
- Query Parameters:
  - `name` (optional): Name of the adventure
- Returns: Created adventure object with the following structure:
```json
{
  "nutrientsMap": {
    "protein": 0.0,
    "fat": 0.0,
    "carbs": 0.0,
    "water": 0.0,
    "fiber": 0.0,
    "salt": 0.0
  },
  "childMap": {},
  "mealWeights": {},
  "weight": 0.0,
  "id": "id_[uuid]",
  "energyDensity": 0.0,
  "name": "Adventure Name",
  "creationTime": "ISO-8601 timestamp",
  "crewDailyKcalNeed": 0,
  "days": 0,
  "crewSize": 0,
  "allCrewMembers": [],
  "formattedTotalRatio": "0,0",
  "formattedEnergyDensity": "0.0"
}
```

### Get all adventures
- **GET** `/adventures`
- Returns: Array of adventure objects (same structure as above)

### Get a specific adventure
- **GET** `/adventures/{id}`
- Path Parameters:
  - `id`: Adventure ID (format: "id_[uuid]")
- Returns: Adventure object
- Status Codes:
  - 200: Adventure found
  - 404: Adventure not found

### Add crew member to adventure
- **POST** `/adventures/{id}/crew`
- Path Parameters:
  - `id`: Adventure ID
- Query Parameters:
  - `name`: Crew member name
  - `age`: Crew member age (integer)
  - `height`: Crew member height in cm (integer)
  - `weight`: Crew member weight in kg (integer)
  - `gender`: Crew member gender (MALE/FEMALE)
  - `activity`: Activity level (SEDENTARY/MILD/MODERATE/HEAVY/VERY_HEAVY)
  - `strategy`: Strategy type (harris_benedict_original/harris_benedict_revised/mifflin_st_jeor)
- Returns: Updated adventure object
- Status Codes:
  - 200: Crew member added successfully
  - 404: Adventure not found
  - 400: Invalid parameters

### Set number of days for adventure
- **PUT** `/adventures/{id}/days`
- Path Parameters:
  - `id`: Adventure ID
- Query Parameters:
  - `days`: Number of days (integer)
- Returns: Updated adventure object
- Status Codes:
  - 200: Days updated successfully
  - 404: Adventure not found

### Add meal to adventure
- **POST** `/adventures/{id}/meals`
- Path Parameters:
  - `id`: Adventure ID
- Query Parameters:
  - `name`: Meal name
- Returns: Created meal ID (format: "id_[uuid]")
- Status Codes:
  - 200: Meal added successfully
  - 404: Adventure not found

### Get adventure information
- **GET** `/adventures/{id}/info`
- Path Parameters:
  - `id`: Adventure ID
- Returns: Success message (information printed to console)
- Status Codes:
  - 200: Information retrieved successfully
  - 404: Adventure not found

## Meals API

### Create a new meal
- **POST** `/meals`
- Query Parameters:
  - `name`: Meal name
- Returns: Created meal object

### Get a specific meal
- **GET** `/meals/{id}`
- Path Parameters:
  - `id`: Meal ID
- Returns: Meal object
- Status Codes:
  - 200: Meal found
  - 404: Meal not found

### Add ingredient to meal
- **POST** `/meals/{id}/ingredients`
- Path Parameters:
  - `id`: Meal ID
- Query Parameters:
  - `name`: Ingredient name
- Returns: Created ingredient ID (format: "id_[uuid]")
- Status Codes:
  - 200: Ingredient added successfully
  - 404: Meal not found

### Modify ingredient weight
- **PUT** `/meals/{mealId}/ingredients/{ingredientId}`
- Path Parameters:
  - `mealId`: Meal ID
  - `ingredientId`: Ingredient ID
- Query Parameters:
  - `weight`: New weight value (double)
- Returns: Empty response (204 No Content)
- Status Codes:
  - 204: Weight updated successfully
  - 404: Meal not found

### Get meal information
- **GET** `/meals/{id}/info`
- Path Parameters:
  - `id`: Meal ID
- Returns: Success message (information printed to console)
- Status Codes:
  - 200: Information retrieved successfully
  - 404: Meal not found 