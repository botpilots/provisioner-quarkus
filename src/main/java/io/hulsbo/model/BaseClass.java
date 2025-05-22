package io.hulsbo.model;

import java.util.UUID;
import io.hulsbo.util.model.baseclass.ChildWrapper;
import io.hulsbo.util.model.baseclass.NutrientsMap;
import io.quarkus.logging.Log;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseClass {
    protected final NutrientsMap nutrientsMap = new NutrientsMap();
    protected final Map<UUID, ChildWrapper> childMap = new LinkedHashMap<>();
    protected final Map<String, UUID> nameIndex = new HashMap<>();
    protected final Set<UUID> parents = new HashSet<>(); // Parent tracking
    protected double weight;
    private final UUID id;
    protected double energyDensity;
    private String name;
    protected final OffsetDateTime creationTime;

	// Constructor creating new id
    public BaseClass() {
        this.creationTime = OffsetDateTime.now(ZoneOffset.ofHours(2));
        UUID id = UUID.randomUUID();
        this.id = id;
        this.name = "Unnamed " + getClass().getSimpleName();
        Manager.register(id, this);
    }

	// Constructor accepting id as parameter
	public BaseClass(UUID id) {
		this.id = id;
		this.creationTime = OffsetDateTime.now(ZoneOffset.ofHours(2));
		this.name = "Unnamed " + getClass().getSimpleName();
		Manager.register(id, this);
	}

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        
        // Trigger update propagation
        this.updateAndPropagate();
    }

    public UUID getId() {
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
        for (UUID key : childMap.keySet()) {
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
            for (UUID key : childMap.keySet()) {
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
        for (UUID key : childMap.keySet()) {
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
        Set<UUID> children = childMap.keySet();
        double sum = 0;

        for (UUID id : children) {
            sum += childMap.get(id).getRatio();
        }

        System.out.printf(" ratio: " + "%5.1f %%", sum * 100);

        if (getClass() != Adventure.class) {
            sum = 0;
            for (UUID id : children) {
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
     * @return UUID key of newChild
     */
    protected UUID putChild(BaseClass newChild, Double newWeightedValue, Double absWeight) {
		if (newChild == null) {
			throw new IllegalArgumentException("newChild cannot be null - must be a subclass of BaseClass.");
		}
        ChildWrapper newChildWrapper = new ChildWrapper(newChild, newWeightedValue, absWeight);
        childMap.put(newChild.getId(), newChildWrapper);
        newChild.addParent(this.getId());
        // NOTE: Registration in Manager is done in constructor.
        updateNameIndex();
        this.updateAndPropagate(); // Trigger update
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
    protected void modifyRatio(UUID key, Double newWeightedValue) {
        if (!childMap.containsKey(key)) {
            throw new IllegalArgumentException("Child with key not present in childMap.");
        }
        ChildWrapper childWrapper = childMap.get(key);
        childWrapper.setRatio(newWeightedValue);
    }

    /**
     * Update the recipe weight of an existing child.
     * The key must be present in childMap.
     *
     * @param key             Key of the child to update.
     * @param newRecipeWeight The new weighted value.
     * @throws IllegalArgumentException if the key is not present in childMap.
     */
    protected void modifyRecipeWeight(UUID key, Double newRecipeWeight) {
        if (!childMap.containsKey(key)) {
            throw new IllegalArgumentException("Child with key not present in childMap.");
        }
        ChildWrapper childWrapper = childMap.get(key);
        childWrapper.setRecipeWeight(newRecipeWeight);
        setNutrientsMapAndWeights();
        this.updateAndPropagate(); // Trigger update
    }

    /**
     * Update the child of an existing ChildWrapper
     * The key must be present in childMap.
     *
     * @param key      Key of the child to update.
     * @param newChild The new child object.
     * @throws IllegalArgumentException if the key is not present in childrenMap.
     */
    protected void modifyChild(UUID key, BaseClass newChild) {
        if (!childMap.containsKey(key)) {
            throw new IllegalArgumentException("Child with key not present in childMap.");
        }
        ChildWrapper childWrapper = childMap.get(key);
        // Unregister old child's parent if necessary
        BaseClass oldChild = childWrapper.getChild();
        if (oldChild != null) {
            oldChild.removeParent(this.getId());
        }
        // Set new child and register parent
        childWrapper.setChild(newChild);
        newChild.addParent(this.getId());
        updateNameIndex();
        setNutrientsMapAndWeights();
        this.updateAndPropagate();
    }


    /**
     * Removes child using the name index, updates name index and scales the ratioMap so all children sum remains equal to 1.
     *
     * @param name Name of the child to remove.
     */
    public String removeChild(String name) {
        UUID key = nameIndex.get(name);
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
    public String removeChild(UUID key) {
        // Unregister parent before removal
        ChildWrapper removedWrapper = childMap.get(key);
        if (removedWrapper != null) {
             BaseClass childToRemove = removedWrapper.getChild();
             if (childToRemove != null) {
                 childToRemove.removeParent(this.getId());
             }
        }

        ChildWrapper wasRemoved = childMap.remove(key);
        if (wasRemoved != null) {
            updateNameIndex();
            scaleEntriesOnRemoval(wasRemoved.getRatio());
            setNutrientsMapAndWeights();
            this.updateAndPropagate();
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
        for (UUID key : childMap.keySet()) {
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

    // NOTE: Used in template.
    public Map<UUID, ChildWrapper> getChildMap() {
        return childMap;
    }

    // NOTE: Used in template
    public String getFormattedEnergyDensity() {
        return String.format(Locale.US, "%.1f", energyDensity);
    }

    // Method to add parent ID
    public void addParent(UUID parentId) {
        this.parents.add(parentId);
    }

    // Method to remove parent ID
    public void removeParent(UUID parentId) {
        this.parents.remove(parentId);
    }

    // Base update and propagation method
    // Made public so Resource classes can trigger it after batch updates
    public void updateAndPropagate() {
        Log.infof("[%s ID: %s] Entering updateAndPropagate.", getClass().getSimpleName(), getId());

        // Specific update logic should be implemented in subclasses BEFORE this call
        Log.infof("[%s ID: %s] Propagating update upwards to %d parent(s).", getClass().getSimpleName(), getId(), parents.size());
        for (UUID parentId : parents) {
            BaseClass parent = Manager.getBaseClass(parentId);
            if (parent != null) {
                Log.infof("[%s ID: %s] --> Calling updateAndPropagate on parent [%s ID: %s].",
                          getClass().getSimpleName(), getId(), parent.getClass().getSimpleName(), parentId);
                parent.updateAndPropagate(); // Recursive call to the parent's version
            } else {
                Log.warnf("[%s ID: %s] Found null parent reference during propagation for parent ID: %s",
                          getClass().getSimpleName(), getId(), parentId);
                // Optionally remove the null parent reference here if desired
                // this.parents.remove(parentId); // Be careful with concurrent modification if iterating directly
            }
        }
        Log.infof("[%s ID: %s] Exiting updateAndPropagate.", getClass().getSimpleName(), getId());
    }
}
