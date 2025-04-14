# Curl Test Examples

This document demonstrates a typical use case of the backend API using curl commands. The example follows a scenario where we:
1. Create an adventure
2. Add crew members
3. Add meals to the adventure
4. Add ingredients to meals
5. Modify ingredient weights

## 1. Create a New Adventure

```bash
curl -X POST "http://localhost:8081/adventures?name=Summer%20Hiking%20Trip"
```

Expected response:
```json
{
  "id": "adventure-123",
  "name": "Summer Hiking Trip",
  "days": 0,
  "crew": {},
  "meals": {}
}
```

## 2. Add Crew Members to the Adventure

```bash
curl -X POST "http://localhost:8081/adventures/adventure-123/crew?name=John&age=30&height=180&weight=75&gender=male&activity=high&strategy=balanced"
```

Expected response:
```json
{
  "id": "adventure-123",
  "name": "Summer Hiking Trip",
  "days": 0,
  "crew": {
    "John": {
      "name": "John",
      "age": 30,
      "height": 180,
      "weight": 75,
      "gender": "male",
      "activity": "high",
      "strategy": "balanced"
    }
  },
  "meals": {}
}
```

## 3. Set Number of Days for the Adventure

```bash
curl -X PUT "http://localhost:8081/adventures/adventure-123/days?days=5"
```

Expected response:
```json
{
  "id": "adventure-123",
  "name": "Summer Hiking Trip",
  "days": 5,
  "crew": {
    "John": {
      "name": "John",
      "age": 30,
      "height": 180,
      "weight": 75,
      "gender": "male",
      "activity": "high",
      "strategy": "balanced"
    }
  },
  "meals": {}
}
```

## 4. Add Meals to the Adventure

```bash
curl -X POST "http://localhost:8081/adventures/adventure-123/meals?name=Breakfast%20Oatmeal"
```

Expected response:
```json
"meal-456"
```

## 5. Add Ingredients to the Meal

```bash
curl -X POST "http://localhost:8081/meals/meal-456/ingredients?name=Oats"
```

Expected response:
```json
"ingredient-789"
```

## 6. Modify Ingredient Weight

```bash
curl -X PUT "http://localhost:8081/meals/meal-456/ingredients/ingredient-789?weight=100"
```

Expected response:
```json
"Success"
```

## 7. Get Adventure Information

```bash
curl -X GET "http://localhost:8081/adventures/adventure-123/info"
```

Expected response:
```json
"Adventure info printed to console"
```

## 8. Get Meal Information

```bash
curl -X GET "http://localhost:8081/meals/meal-456/info"
```

Expected response:
```json
"Meal info printed to console"
```

## 9. Get All Adventures

```bash
curl -X GET "http://localhost:8081/adventures"
```

Expected response:
```json
[
  {
    "id": "adventure-123",
    "name": "Summer Hiking Trip",
    "days": 5,
    "crew": {
      "John": {
        "name": "John",
        "age": 30,
        "height": 180,
        "weight": 75,
        "gender": "male",
        "activity": "high",
        "strategy": "balanced"
      }
    },
    "meals": {
      "meal-456": {
        "name": "Breakfast Oatmeal",
        "ingredients": {
          "ingredient-789": {
            "name": "Oats",
            "weight": 100
          }
        }
      }
    }
  }
]
```

## Notes
- Replace `localhost:8081` with your actual server address if different
- The IDs (adventure-123, meal-456, ingredient-789) are examples and will be different in your actual responses
- All responses are in JSON format
- Make sure to URL encode special characters in the query parameters 