package io.hulsbo.model;

public class ManualTest {
    public static void main(String[] args) {
        // Create an adventure
        Adventure adventure = new Adventure();
        adventure.setName("Summer Expedition");
        adventure.setDays(5);

        // Add crew members
        adventure.putCrewMember("John", 30, 180, 75, "MALE", "MODERATE", "mifflin_st_jeor");
        adventure.putCrewMember("Sarah", 28, 165, 60, "FEMALE", "HEAVY", "harris_benedict_revised");

        // Create and add meals
        Meal breakfast = new Meal();
        breakfast.setName("Morning Oats");
        
        Meal lunch = new Meal();
        lunch.setName("Trail Mix");
        
        Meal dinner = new Meal();
        dinner.setName("Camp Stew");

        // Add meals to adventure
        adventure.putChild(breakfast);
        adventure.putChild(lunch);
        adventure.putChild(dinner);

        // Create ingredients
        Ingredient oats = new Ingredient();
        oats.setName("Oats");
        oats.getNutrientsMap().put("carbs", 0.65);
        oats.getNutrientsMap().put("protein", 0.12);
        oats.getNutrientsMap().put("fat", 0.23);

        Ingredient nuts = new Ingredient();
        nuts.setName("Mixed Nuts");
        nuts.getNutrientsMap().put("carbs", 0.20);
        nuts.getNutrientsMap().put("protein", 0.15);
        nuts.getNutrientsMap().put("fat", 0.65);

        Ingredient rice = new Ingredient();
        rice.setName("Rice");
        rice.getNutrientsMap().put("carbs", 0.80);
        rice.getNutrientsMap().put("protein", 0.08);
        rice.getNutrientsMap().put("fat", 0.12);

        Ingredient beans = new Ingredient();
        beans.setName("Beans");
        beans.getNutrientsMap().put("carbs", 0.60);
        beans.getNutrientsMap().put("protein", 0.25);
        beans.getNutrientsMap().put("fat", 0.15);

        // Add ingredients to meals
        breakfast.putChild(oats);
        breakfast.modifyWeightOfIngredient(oats.getId(), 100.0);

        lunch.putChild(nuts);
        lunch.modifyWeightOfIngredient(nuts.getId(), 150.0);

        dinner.putChild(rice);
        dinner.putChild(beans);
        dinner.modifyWeightOfIngredient(rice.getId(), 200.0);
        dinner.modifyWeightOfIngredient(beans.getId(), 150.0);

        // Print adventure info
        adventure.getInfo();
    }
} 