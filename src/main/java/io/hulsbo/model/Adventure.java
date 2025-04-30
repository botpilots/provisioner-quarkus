package io.hulsbo.model;

import io.hulsbo.util.model.CrewMember.Gender;
import io.hulsbo.util.model.CrewMember.KCalCalculationStrategies.KCalCalculationStrategy;
import io.hulsbo.util.model.CrewMember.PhysicalActivity;
import io.hulsbo.util.model.SafeID;
import io.hulsbo.util.model.baseclass.ChildWrapper;
import io.quarkus.logging.Log;

import java.util.*;
import java.util.stream.Collectors;

public class Adventure extends BaseClass  {
	private final Map<SafeID, CrewMember> crewMemberMap = new LinkedHashMap<>();
	private double crewDailyKcalNeed;
	private int days;
	private final Map<SafeID, Double> ingredientWeights = new LinkedHashMap<>();

	public Adventure() {

	}

	@Override
	public void setEnergyDensity() {
		super.setEnergyDensity();
		setWeight();
	}

	public void setWeight() {
		if (energyDensity != 0) {
			this.weight = (crewDailyKcalNeed * days) / energyDensity;
		}
	}

	public void setMealAndIngredientWeights() {

		Log.info("setMealAndIngredientWeights run");

		ingredientWeights.clear();

		setChildWeights(); // Ingredient weights depends on an updated childWeights field.

		Set<SafeID> mealKeys = childWeights.keySet();

		for (SafeID mealKey : mealKeys) { // For each meal, calculate its child ingredients weights and save in
											// ingredientWeights.

			Log.info("Meal");

			Map<SafeID, ChildWrapper> mealIngredients = childMap.get(mealKey).getChild().childMap;

			Set<SafeID> ingredientKeys = mealIngredients.keySet();

			for (SafeID ingredientKey : ingredientKeys) {
				Log.info("Ingredient");
				Log.info("key: " + ingredientKey);
				Log.info("mealweight: " + childWeights.get(mealKey));
				Log.info("ratio ingred: " + mealIngredients.get(ingredientKey).getRatio());
				// NOTE: Since there's no direct connection to this object and the ingredient,
				// we use the id of the ingredient itself.
				ingredientWeights.put(ingredientKey,
						childWeights.get(mealKey) * mealIngredients.get(ingredientKey).getRatio());

			if (!ingredientWeights.containsKey(ingredientKey)) {
				throw new IllegalArgumentException("Ingredient weight with key " + ingredientKey + " could not be added to ingredientWeights.");
			} else {
				Log.info("Ingredient weight with key " + ingredientKey + " was successfully added to ingredientWeights, with weight: " + ingredientWeights.get(ingredientKey));
			}
			}
		}
	}

	public void setNutrientsMapAndWeights() {
		super.setNutrientsMapAndWeights();
		setCrewDailyKcalNeed();
		setMealAndIngredientWeights();
	}

	/**
	 * Add a new meal to meals hashmap and the ratios hashmap using the same key.
	 *
	 * @return SafeID key of newChild
	 */
	public SafeID putChild(Meal newMeal) {
		double weightedValue = giveSpaceForAnotherEntry();
		return super.putChild(newMeal, weightedValue, 0.0);
	}

	public void putCrewMember(String name, int age, int height, int weight, String gender, String activity,
			String kCalCalculationStrategy) {
		CrewMember newCrewMember = new CrewMember(name, age, height, weight, gender, activity, kCalCalculationStrategy);
		crewMemberMap.put(newCrewMember.getId(), newCrewMember);
		// NOTE: Registration in Manager is done in constructor.
		setCrewDailyKcalNeed();

		// Trigger update propagation
		this.updateAndPropagate();
	}

	public void setCrewDailyKcalNeed() {
		int sum = 0;
		for (CrewMember crewMember : crewMemberMap.values()) {
			sum += crewMember.getDailyKCalNeed();
		}
		this.crewDailyKcalNeed = sum;
		setWeight();
	}

	public void setDays(int days) {
		if (days > 0) {
			this.days = days;
		} else {
			throw new IllegalArgumentException("Days must be one or more.");
		}

		// Trigger update propagation
		this.updateAndPropagate();
	}

	public int getDays() {
		return days;
	}

	public void getInfo() {
		System.out.println();
		System.out.println("Summary " + "of " + getClass().getSimpleName() + " \"" + getName() + "\":");
		System.out.println();
		System.out.println("Crew members: ".toUpperCase());
		int i = 1;
		for (CrewMember crewMember : crewMemberMap.values()) {
			System.out.println();
			System.out.printf("%25s %s %n", "Crew member " + i + ":", crewMember.getName());
			System.out.printf("%25s %s %n", "Gender:", crewMember.getGender().toString().toLowerCase());
			System.out.printf("%25s %d %n", "Age:", crewMember.getAge());
			System.out.printf("%25s %s %n", "Activity level:", crewMember.getActivity().toString().toLowerCase());
			System.out.printf("%25s %d KCal %n", "Daily KCal need:", crewMember.getDailyKCalNeed());

			i++;
		}
		System.out.println();
		System.out.printf("%25s %.0f KCal %n", "Daily KCal need crew:", crewDailyKcalNeed);

		System.out.println();

		System.out.println("MEALS FOR " + days + " DAYS:");
		System.out.println();
		System.out.println();
		childMap.forEach((key, value) -> {
			System.out.printf("%10s |", value.getChild().getName());
			System.out.printf(" ratio: " + "%5.1f %%", childMap.get(key).getRatio() * 100);
			Set<String> nutrients = childMap.get(key).getChild().getNutrientsMap().keySet();
			for (String nutrient : nutrients) {
				System.out.printf(" | %s: %4.1f %%", nutrient,
						childMap.get(key).getChild().getNutrientsMap().get(nutrient) * 100);
			}
			System.out.printf(" | calc. weight: " + "%4.2f kg", childWeights.get(childMap.get(key).getChild().getId()));
			System.out.println();
			System.out.println();
			// For adventures, also sum each ingredient for each meal
			Map<SafeID, ChildWrapper> childMapIngredient = value.getChild().childMap;
			childMapIngredient.forEach((childMapIngredientKey, childMapIngredientValue) -> {
				System.out.printf("%15s |", childMapIngredientValue.getChild().getName());
				System.out.printf(" ratio: " + "%5.1f %%",
						childMapIngredient.get(childMapIngredientKey).getRatio() * 100);
				Set<String> ingredientNutrients = childMapIngredient.get(childMapIngredientKey).getChild()
						.getNutrientsMap().keySet();
				for (String nutrient : ingredientNutrients) {
					System.out.printf(" | %s: %4.1f %%", nutrient,
							childMapIngredient.get(childMapIngredientKey).getChild().getNutrientsMap().get(nutrient)
									* 100);
				}
				System.out.printf(" | calc. weight: " + "%4.2f kg",
						ingredientWeights.get(childMapIngredient.get(childMapIngredientKey).getChild().getId()));
				System.out.println();
			});
			System.out.println();
		});

		// Summary
		System.out.printf("%10s |", getClass().getSimpleName());

		Set<SafeID> children = childMap.keySet();
		double sum = 0;

		for (SafeID id : children) {
			sum += childMap.get(id).getRatio();
		}

		System.out.printf(" ratio: " + "%5.1f %%", sum * 100);

		Set<String> nutrients = getNutrientsMap().keySet();
		for (String nutrient : nutrients) {
			System.out.printf(" | %s: %4.1f %%", nutrient, getNutrientsMap().get(nutrient) * 100);
		}
		System.out.printf(" | calc. weight: " + "%4.2f kg", getWeight());
		System.out.println();
		System.out.println();
		System.out.printf("Energy Density of " + getClass().getSimpleName() + ": %4.0f KCal/Kg %n", energyDensity);
		System.out.println();
		System.out.println("END OF SUMMARY");
	}

	// NOTE: Used in template.
	public int getCrewSize() {
		return crewMemberMap.size();
	}
	// NOTE: Used in template.

	/**
	 * Get all crew members of this adventure, sorted from oldest to newest in
	 * creation time.
	 * 
	 * @return List<CrewMember>
	 */
	public List<CrewMember> getAllCrewMembers() {
		return crewMemberMap.values().stream()
				.sorted(Comparator.comparing(CrewMember::getCreationTime))
				.collect(Collectors.toList());
	}

	// NOTE: Used in template.
	public int getCrewDailyKcalNeed() {
		return (int) crewDailyKcalNeed;
	}

	// NOTE: Used in template.
	public String getFormattedTotalRatio() {
		double ratio = childMap.values().stream()
				.mapToDouble(ChildWrapper::getRatio)
				.sum();
		return String.format("%.1f", ratio * 100);
	}

	// NOTE: Used in template.
	public Map<SafeID, Double> getIngredientWeights() {
		return ingredientWeights;
	}

	public void removeCrewMember(SafeID id) {
		crewMemberMap.remove(id);
		if (crewMemberMap.containsKey(id)) {
			throw new IllegalArgumentException("Crew member with id " + id + " could not be removed from adventure.");
		} else {
			Log.info("Crew member with id " + id + " was successfully removed from adventure.");
		}

		// Trigger update propagation
		this.updateAndPropagate();
	}

	// Override updateAndPropagate
	@Override
	public void updateAndPropagate() {
		Log.infof("[Adventure ID: %s] Entering updateAndPropagate.", getId());
		// Adventure-specific updates
		this.setNutrientsMapAndWeights();
		super.updateAndPropagate(); // Propagate upwards (though Adventure is usually the root)
		Log.infof("[Adventure ID: %s] Exiting updateAndPropagate.", getId());
	}

	public double getTotalWeight() {
		return weight;
	}
}
