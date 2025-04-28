package io.hulsbo.model;

import io.hulsbo.util.model.SafeID;
import io.hulsbo.util.model.baseclass.ChildWrapper;
import io.hulsbo.util.model.baseclass.NutrientsMap;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

public abstract class BaseClass {
    protected final NutrientsMap nutrientsMap = new NutrientsMap();
    protected final Map<SafeID, ChildWrapper> childMap = new LinkedHashMap<>();
    protected final Map<SafeID, Double> childWeights = new LinkedHashMap<>(); // NOTE: This one uses not ChildMap keys.
    protected final Map<String, SafeID> nameIndex = new HashMap<>();
    protected double weight;
    private final SafeID id;
    protected double energyDensity;
    private String name;
    protected final OffsetDateTime creationTime;

    public BaseClass() {
        this.creationTime = OffsetDateTime.now(ZoneOffset.ofHours(2));
        SafeID id = SafeID.randomSafeID();
        this.id = id;
        this.name = "Unnamed " + getClass().getSimpleName();
        Manager.register(id, this);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;

        // Update name index for all parents
        Set<BaseClass> parents = Manager.findParents(this.getId());
        for (BaseClass parent : parents) {
            parent.updateNameIndex();
        }
    }

    public SafeID getId() {
        return this.id;
    }

    public void setEnergyDensity() {
        double carbsRatio = nutrientsMap.get("carbs");
        double proteinRatio = nutrientsMap.get("protein");
        double fatRatio = nutrientsMap.get("fat");

        this.energyDensity = (carbsRatio + proteinRatio) * 4000 + fatRatio * 9000;
    }

    // NOTE: used in template
    public double getEnergyDensity() {
        return this.energyDensity;
    }


    public NutrientsMap getNutrientsMap() {
        return nutrientsMap;
    }

    // NOTE: made public for qute template
    public String getFormattedNutrientsMap(String nutrient) {
        Double value = getNutrientsMap().get(nutrient);
        if (value == null) {
            return "N/A";
        }
        return String.format("%.1f%%", value * 100);
    }


    /**
     * Recalculates the nutrientsMap() based on childMap and ratiosMap
     * This method should be run if childMap has been updated.
     */
    protected void setNutrientsMapAndWeights() {
        Set<String> nutrients = nutrientsMap.keySet();

        // Reset this baseclass nutrientsMap
        for (String nutrient : nutrients) {
            nutrientsMap.put(nutrient, 0.0);
        }

        // Add all weighted nutrients of the baseclass to this baseclass' nutrientMap
        for (SafeID key : childMap.keySet()) {
            double ratio = childMap.get(key).getRatio();
            BaseClass baseClass = childMap.get(key).getChild();
            NutrientsMap baseClassNutrients = baseClass.getNutrientsMap();

            for (String nutrient : nutrients) {
                nutrientsMap.merge(nutrient, baseClassNutrients.get(nutrient),
                        (oldValue, newValue) -> (oldValue + newValue * ratio));
            }
        }
        setEnergyDensity();
    }


    /**
     * Resizes the percentage hashmap values assuming all take equal space, so their sum is one.
     * If empty returned weight is 1.
     *
     * @return the value of the new allocated space
     */
    protected double giveSpaceForAnotherEntry() {
        double size = childMap.size();
        double oldEntriesAllowedSpace = size / (size + 1);
        double sumNewValue = 0.0;

        if (!childMap.isEmpty()) {
            for (SafeID key : childMap.keySet()) {
                double oldValue = childMap.get(key).getRatio();
                double newValue = oldValue * oldEntriesAllowedSpace;
                sumNewValue += newValue;
                childMap.get(key).setRatio(newValue);
            }
        }
        return 1 - sumNewValue;
    }

    /**
     * For a given weightedValue space, enlarge the other weighted values in proportion for sum to remain 1.
     * <p>This method should be run if child is removed from childMap.</p>
     */
    protected void scaleEntriesOnRemoval(double weightedValue) {
        double scaleFactor = 1 / (1 - weightedValue);
        for (SafeID key : childMap.keySet()) {
            double value = childMap.get(key).getRatio();
            childMap.get(key).setRatio(value * scaleFactor);
        }
    }

    /**
     * Print info about this object.
     */
    public void getInfo() {
        System.out.println();
        System.out.println("Summary " + "of " + getClass().getSimpleName() + " \"" + getName() + "\":");
        System.out.println();
        childMap.forEach((key, value) -> {
            System.out.printf("%10s |", value.getChild().getName());
            System.out.printf(" ratio: " + "%5.1f %%", childMap.get(key).getRatio() * 100);
            if (getClass() != Adventure.class) {
                System.out.printf(" | weight: " + "%5.1f g", childMap.get(key).getRecipeWeight());
            }
            Set<String> nutrients = childMap.get(key).getChild().getNutrientsMap().keySet();
            for (String nutrient : nutrients) {
                System.out.printf(" | %s: %4.1f %%", nutrient, childMap.get(key).getChild().getNutrientsMap().get(nutrient) * 100);
            }
            System.out.println();
        });

        System.out.println();
        System.out.printf("%10s |", getClass().getSimpleName());
        Set<SafeID> children = childMap.keySet();
        double sum = 0;

        for (SafeID id : children) {
            sum += childMap.get(id).getRatio();
        }

        System.out.printf(" ratio: " + "%5.1f %%", sum * 100);

        if (getClass() != Adventure.class) {
            sum = 0;
            for (SafeID id : children) {
                sum += childMap.get(id).getRecipeWeight();
            }
            System.out.printf(" | weight: " + "%5.1f g", sum);
        }

        Set<String> nutrients = getNutrientsMap().keySet();
        for (String nutrient : nutrients) {
            System.out.printf(" | %s: %4.1f %%", nutrient, getNutrientsMap().get(nutrient) * 100);
        }
        System.out.println();
        System.out.println();

        System.out.printf("Energy Density of " + getClass().getSimpleName() + ": %4.0f KCal/Kg %n%n", energyDensity);
    }

    /**
     * Base method for putting new children and updating name index. See subclass for full method.
     *
     * @param newChild         The new child to add.
     * @param newWeightedValue The weighted value of the child.
     * @param absWeight        The absolute weight of the child.
     * @return SafeID key of newChild
     */
    protected SafeID putChild(BaseClass newChild, Double newWeightedValue, Double absWeight) {
        ChildWrapper newChildWrapper = new ChildWrapper(newChild, newWeightedValue, absWeight);
        childMap.put(newChild.getId(), newChildWrapper);
        // NOTE: Registration in Manager is done in constructor.
        updateNameIndex();
        setNutrientsMapAndWeights();
        return newChild.getId();
    }

    /**
     * Update the weighted value of an existing child.
     * The key must be present in childMap.
     *
     * @param key              Key of the child to update.
     * @param newWeightedValue The new weighted value.
     * @throws IllegalArgumentException if the key is not present in childMap.
     */
    protected void modifyRatio(SafeID key, Double newWeightedValue) {
        if (!childMap.containsKey(key)) {
            throw new IllegalArgumentException("Child with key not present in childMap.");
        }
        ChildWrapper childWrapper = childMap.get(key);
        childWrapper.setRatio(newWeightedValue);
        childMap.put(key, childWrapper);
        setNutrientsMapAndWeights();
    }

    /**
     * Update the recipe weight of an existing child.
     * The key must be present in childMap.
     *
     * @param key             Key of the child to update.
     * @param newRecipeWeight The new weighted value.
     * @throws IllegalArgumentException if the key is not present in childMap.
     */
    protected void modifyRecipeWeight(SafeID key, Double newRecipeWeight) {
        if (!childMap.containsKey(key)) {
            throw new IllegalArgumentException("Child with key not present in childMap.");
        }
        ChildWrapper childWrapper = childMap.get(key);
        childWrapper.setRecipeWeight(newRecipeWeight);
        childMap.put(key, childWrapper);
        setNutrientsMapAndWeights();
    }

    /**
     * Update the child of an existing ChildWrapper
     * The key must be present in childMap.
     *
     * @param key      Key of the child to update.
     * @param newChild The new child object.
     * @throws IllegalArgumentException if the key is not present in childrenMap.
     */
    protected void modifyChild(SafeID key, BaseClass newChild) {
        if (!childMap.containsKey(key)) {
            throw new IllegalArgumentException("Child with key not present in childMap.");
        }
        ChildWrapper childWrapper = childMap.get(key);
        childWrapper.setChild(newChild);
        childMap.put(key, childWrapper);
        updateNameIndex();
        setNutrientsMapAndWeights();
    }


    /**
     * Removes child using the name index, updates name index and scales the ratioMap so all children sum remains equal to 1.
     *
     * @param name Name of the child to remove.
     */
    public String removeChild(String name) {
        SafeID key = nameIndex.get(name);
        if (key == null) {
            throw new NullPointerException("No child with that name.");
        } else {
            return removeChild(key);
        }
    }

    /**
     * Removes child using key, updates name index and scales the ratioMap so all children sum remains equal to 1.
     *
     * @param key Key of the child to remove.
     * @return String as recipe for successful removal. Throws error if removal was unsuccessful.
     */
    public String removeChild(SafeID key) {
        ChildWrapper wasRemoved = childMap.remove(key);
        if (wasRemoved != null) {
            updateNameIndex();
            scaleEntriesOnRemoval(wasRemoved.getRatio());
            setNutrientsMapAndWeights();
            return "Child " + wasRemoved.getChild().getName() + " was successfully removed.";
        } else {
            throw new NullPointerException("The child of " +
                    this.getClass().getSimpleName() +
                    " could not be found in " +
                    childMap.getClass().getSimpleName() + ".");
        }
    }

    /**
     * Keeps a name index over current child objects of this BaseClass.
     * <p>Should be updated when children are added or removed from this object.</p>
     */
    protected void updateNameIndex() {
        nameIndex.clear();
        for (SafeID key : childMap.keySet()) {
            nameIndex.put(childMap.get(key).getChild().getName(), key);
        }
    }

    public double getWeight() {
        return weight;
    }

    /**
     * Get creation time in UTC+2 time.
     * @return OffsetDateTime
     */
    public OffsetDateTime getCreationTime() {
        return creationTime;
    }

    /**
     * Get all children, sorted from oldest to newest.
     * @return List<ChildWrapper>
     */
   public List<ChildWrapper> getAllChildren() {
        return childMap.values().stream()
                .sorted(Comparator.comparing(wrapper -> wrapper.getChild().getCreationTime()))
                .collect(Collectors.toList());
    }

    // NOTE: Used in template.
    public Map<SafeID, ChildWrapper> getChildMap() {
        return childMap;
    }

    // NOTE: Used in template.
    public String getFormattedEnergyDensity() {
        return String.format(Locale.US, "%.1f", energyDensity);
    }

    // NOTE: Used in template
    public Map<SafeID, Double> getChildWeights() {
        return childWeights;
    }

    protected void setChildWeights() { // refer to setIngredientWeights() in Adventure.java for complete update.

        childWeights.clear();

        Set<SafeID> keys = childMap.keySet();
        for (SafeID key : keys) {
            // NOTE: To be consistent with ingredientWeights, we here use the id of the Meal object itself.
            SafeID mealKey = childMap.get(key).getChild().getId();
            childWeights.put(mealKey, weight*childMap.get(key).getRatio());
        }
    }

}
