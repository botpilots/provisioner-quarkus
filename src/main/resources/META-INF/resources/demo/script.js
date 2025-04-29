// API endpoints
const API_BASE_URL = 'http://localhost:8080';

// State variables
let adventures = [];  // Store all adventures
let currentAdventure = null;
let selectedMeal = null;
let selectedIngredient = null; // Add state for selected ingredient
let ingredientToModify = null; // Temp store for ingredient being modified
let lastAdventureData = null;  // Store last fetched adventure data for comparison
let previousIngredientModalState = {}; // Store previous values for comparison
let lastValidNutrientsOnError = null; // Added reset button

// DOM elements
const adventuresList = document.getElementById('adventures-list');
const crewMembersList = document.getElementById('crew-members-list');
const baseClassesList = document.getElementById('base-classes-list');
const addCrewModal = document.getElementById('addCrewModal'); // Modal element
const addMealModal = document.getElementById('addMealModal');
const addIngredientModal = document.getElementById('addIngredientModal');
const setDaysModal = document.getElementById('setDaysModal'); 
const modifyIngredientModal = document.getElementById('modifyIngredientModal'); // Modify modal
const modalMealNameInput = document.getElementById('modalMealName');
const modalIngredientNameInput = document.getElementById('modalIngredientName');
const modalDaysInput = document.getElementById('modalDays'); 
const modalIngredientModifyName = document.getElementById('modalIngredientModifyName'); // Modify modal name display
const modalIngredientModifyId = document.getElementById('modalIngredientModifyId'); // Modify modal hidden ID
const modalIngredientWeightInput = document.getElementById('modalIngredientWeight'); // Modify modal weight input
// Nutrient input elements
const modalIngredientProteinInput = document.getElementById('modalIngredientProtein');
const modalIngredientFatInput = document.getElementById('modalIngredientFat');
const modalIngredientCarbsInput = document.getElementById('modalIngredientCarbs');
const modalIngredientWaterInput = document.getElementById('modalIngredientWater');
const modalIngredientFiberInput = document.getElementById('modalIngredientFiber');
const modalIngredientSaltInput = document.getElementById('modalIngredientSalt');
const adventureTitle = document.getElementById('adventureTitle');
const selectedMealTitle = document.getElementById('selectedMealTitle');
const selectedIngredientTitle = document.getElementById('selectedIngredientTitle');
const addIngredientButton = document.getElementById('addIngredientButton');
const setDaysButton = document.getElementById('setDaysButton'); 
const modalIngredientFeedback = document.getElementById('modalIngredientFeedback'); // Added feedback element
const modalResetButton = document.getElementById('modalResetButton'); // Added reset button

// Track mousedown target to prevent modal closing on drag
let mouseDownTargetOnWindow = null;

// Helper function to make API calls
async function makeApiCall(url, method = 'GET', body = null) {
    try {
        const options = {
            method,
            headers: {
                'Content-Type': 'application/json'
            }
        };
        
        if (body) {
            options.body = JSON.stringify(body);
        }
        
        const response = await fetch(url, options);
        const data = await response.json();
        return { success: true, data };
    } catch (error) {
        return { success: false, error: error.message };
    }
}

// Helper function to render nutrient map
function renderNutrients(nutrientMap, containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.innerHTML = ''; // Clear previous content

    if (!nutrientMap || Object.keys(nutrientMap).length === 0) {
        container.innerHTML = '<p>No nutrient data available.</p>';
        return;
    }

    // Calculate total for percentage calculation
    let total = 0;
    Object.values(nutrientMap).forEach(value => total += value);

    // Create nutrient items
    for (const [key, value] of Object.entries(nutrientMap)) {
        const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : 0;
        const item = document.createElement('div');
        item.className = 'nutrient-item';
        item.innerHTML = `
            <span class="nutrient-name">${key}:</span>
            <span class="nutrient-value">${percentage}%</span>
        `;
        container.appendChild(item);
    }
}

// Helper function to check if data has changed
function hasDataChanged(newData, oldData) {
    if (!oldData) return true;
    return JSON.stringify(newData) !== JSON.stringify(oldData);
}

// Helper function to refresh current adventure data
async function refreshCurrentAdventure() {
    if (!currentAdventure) return;
    
    const adventureId = currentAdventure.id; // Store id in case currentAdventure is replaced
    
    try {
        const response = await fetch(`${API_BASE_URL}/adventures/${adventureId}`);
        if (!response.ok) {
            // Handle case where adventure might have been deleted
            if (response.status === 404) {
                 console.warn(`Adventure with ID ${adventureId} not found.`);
                 // Remove from local state and update UI
                 adventures = adventures.filter(a => a.id !== adventureId);
                 currentAdventure = null;
                 selectedMeal = null;
                 selectedIngredient = null;
                 lastAdventureData = null;
                 updateAdventureDropdown();
                 updateAdventureDisplay(); 
                 return;
            } else {
                 throw new Error(`Failed to fetch adventure details (status: ${response.status})`);
            }
        }
        
		// Only update if response has changed.
        const newData = await response.json();
        if (hasDataChanged(newData, lastAdventureData)) {
            lastAdventureData = newData;
            currentAdventure = newData;

             // Re-select meal/ingredient if they still exist in the new data
            if (selectedMeal && selectedMeal.child) {
                 const updatedMealData = currentAdventure.allChildren.find(m => m.child && m.child.id === selectedMeal.child.id);
                 selectedMeal = updatedMealData || null;
                 if (selectedMeal && selectedIngredient && selectedIngredient.child) {
                     const updatedIngredientData = selectedMeal.child.allChildren.find(i => i.child && i.child.id === selectedIngredient.child.id);
                     selectedIngredient = updatedIngredientData || null;
                 }
             }

            updateAdventureDisplay();
			// Print for debug purposes.
			console.log(currentAdventure);
        }
    } catch (error) {
        console.error('Error refreshing adventure:', error);
        // Optionally reset state if refresh fails critically
        // currentAdventure = null; 
        // updateAdventureDisplay(); 
    }
}

// Adventure Creation
async function createAdventure() {
    const name = document.getElementById('adventureName').value;
    if (!name) {
        alert('Please enter an adventure name');
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/adventures?name=${encodeURIComponent(name)}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        if (!response.ok) {
            throw new Error('Failed to create adventure');
        }

        const data = await response.json();
        adventures.push(data);
        updateAdventureDropdown();
        
        // Select the new adventure
        document.getElementById('adventureSelect').value = data.id;
        await selectAdventure(data.id); // Call selectAdventure to load and display

    } catch (error) {
        alert('Error creating adventure: ' + error.message);
    }
}

// Adventure Selection
async function selectAdventure(adventureId) {
    if (!adventureId) {
        currentAdventure = null;
        selectedMeal = null;
        selectedIngredient = null; // Reset selected ingredient
        lastAdventureData = null;
        updateAdventureDisplay();
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/adventures/${adventureId}`);
        if (!response.ok) {
            throw new Error('Failed to fetch adventure details');
        }
        
        const adventureData = await response.json();
        currentAdventure = adventureData;
        lastAdventureData = adventureData;
        selectedMeal = null;
        selectedIngredient = null; // Reset selected ingredient
        updateAdventureDisplay();
    } catch (error) {
        console.error('Error fetching adventure details:', error);
        alert('Error loading adventure details: ' + error.message);
        currentAdventure = null;
        selectedMeal = null;
        selectedIngredient = null; // Reset selected ingredient
        lastAdventureData = null;
        updateAdventureDropdown(); // Reset dropdown selection if fetch fails
        updateAdventureDisplay();
    }
}

// Adventure Removal
async function removeSelectedAdventure() {
    if (!currentAdventure) {
        alert('Please select an adventure first');
        return;
    }

    const confirmRemove = confirm(`Are you sure you want to remove adventure "${currentAdventure.name}"?`);
    if (!confirmRemove) return;

    try {
        const response = await fetch(`${API_BASE_URL}/adventures/${currentAdventure.id}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error('Failed to remove adventure');
        }

        const removedId = currentAdventure.id;
        adventures = adventures.filter(a => a.id !== removedId);
        currentAdventure = null;
        selectedMeal = null;
        selectedIngredient = null; // Reset selected ingredient
        lastAdventureData = null;
        updateAdventureDropdown();
        updateAdventureDisplay();
    } catch (error) {
        alert('Error removing adventure: ' + error.message);
    }
}

// Update Adventure Dropdown
function updateAdventureDropdown() {
    const select = document.getElementById('adventureSelect');
    const currentSelection = select.value;
    select.innerHTML = '<option value="">Select an adventure...</option>';
    
    adventures.forEach(adventure => {
        const option = document.createElement('option');
        option.value = adventure.id;
        option.textContent = adventure.name;
        select.appendChild(option);
    });

    // Try to preserve selection if possible
    if (adventures.some(a => a.id.toString() === currentSelection)) {
        select.value = currentSelection;
    } else if (currentAdventure) {
         select.value = currentAdventure.id;
    } else {
        select.value = '';
    }
}

// --- Modal Management ---

// NEW Helper function to calculate and update water percentage in the modal
function calculateAndUpdateWaterPercentage() {
    if (!modifyIngredientModal || modifyIngredientModal.style.display !== 'block') {
        return; // Only calculate if the modal is visible
    }
    const protein = parseFloat(modalIngredientProteinInput.value) || 0;
    const fat = parseFloat(modalIngredientFatInput.value) || 0;
    const carbs = parseFloat(modalIngredientCarbsInput.value) || 0;
    const fiber = parseFloat(modalIngredientFiberInput.value) || 0;
    const salt = parseFloat(modalIngredientSaltInput.value) || 0;

    const sum = protein + fat + carbs + fiber + salt;
    // Calculate remaining water, ensuring it's not negative and clamp at 100
    const water = Math.max(0, Math.min(100, 100 - sum)); 
    
    if (modalIngredientWaterInput) {
        modalIngredientWaterInput.value = water.toFixed(1); // Update the disabled input field
    }
}

function openAddCrewModal() {
    if (!currentAdventure) {
        alert('Please select an adventure first.');
        return;
    }
    // Clear previous inputs (optional)
    document.getElementById('modalCrewName').value = '';
    document.getElementById('modalCrewAge').value = '30';
    document.getElementById('modalCrewHeight').value = '175';
    document.getElementById('modalCrewWeight').value = '70';
    document.getElementById('modalCrewGender').value = 'MALE';
    document.getElementById('modalCrewActivity').value = 'MODERATE';
    document.getElementById('modalCrewStrategy').value = 'mifflin_st_jeor'; 

    if(addCrewModal) addCrewModal.style.display = 'block';
    if (document.getElementById('modalCrewName')) document.getElementById('modalCrewName').focus(); // Focus Name field
}

function closeAddCrewModal() {
    if(addCrewModal) addCrewModal.style.display = 'none';
}

function openAddMealModal() {
     if (!currentAdventure) {
        alert('Please select an adventure first.');
        return;
    }
    if (modalMealNameInput) modalMealNameInput.value = ''; // Clear input
    if (addMealModal) addMealModal.style.display = 'block';
    if (modalMealNameInput) modalMealNameInput.focus(); // Focus input
}

function closeAddMealModal() {
    if (addMealModal) addMealModal.style.display = 'none';
}

function openAddIngredientModal() {
    if (!selectedMeal) {
        alert('Please select a meal first.');
        return;
    }
    if (modalIngredientNameInput) modalIngredientNameInput.value = ''; // Clear input
    if (addIngredientModal) addIngredientModal.style.display = 'block';
    if (modalIngredientNameInput) modalIngredientNameInput.focus(); // Focus input
}

function closeAddIngredientModal() {
    if (addIngredientModal) addIngredientModal.style.display = 'none';
}

function openSetDaysModal() {
    if (!currentAdventure) {
        alert('Please select an adventure first.');
        return;
    }
    // Set current value in modal
    if (modalDaysInput) modalDaysInput.value = currentAdventure.days || 1;
    if (setDaysModal) setDaysModal.style.display = 'block';
    if (modalDaysInput) modalDaysInput.focus();
}

function closeSetDaysModal() {
    if (setDaysModal) setDaysModal.style.display = 'none';
}

function openModifyIngredientModal(ingredientData) {
    if (!ingredientData || !ingredientData.child) {
        console.error('Invalid ingredient data for modification modal');
        return;
    }
    ingredientToModify = ingredientData; // Store current data
    const ingredient = ingredientData.child;
    const nutrients = ingredient.nutrientsMap || {};

    // Clear previous feedback and styling
    clearModalFeedbackAndStyles();

    // Populate inputs and store these as the 'previous' state for comparison on update
    previousIngredientModalState = {}; // Reset previous state

    if (modalIngredientModifyName) modalIngredientModifyName.textContent = ingredient.name || 'Unknown';
    if (modalIngredientModifyId) modalIngredientModifyId.value = ingredient.id;
    
    const currentWeight = ingredientData.recipeWeight || 0;
    if (modalIngredientWeightInput) modalIngredientWeightInput.value = currentWeight;
    previousIngredientModalState.weight = currentWeight;

    // Store as percentages (except water initially)
    const nutrientValues = {
        protein: ((nutrients.protein || 0) * 100).toFixed(1),
        fat:     ((nutrients.fat || 0) * 100).toFixed(1),
        carbs:   ((nutrients.carbs || 0) * 100).toFixed(1),
        // water:   ((nutrients.water || 0) * 100).toFixed(1), // Don't set water directly
        fiber:   ((nutrients.fiber || 0) * 100).toFixed(1),
        salt:    ((nutrients.salt || 0) * 100).toFixed(1)
    };

    if (modalIngredientProteinInput) modalIngredientProteinInput.value = nutrientValues.protein;
    if (modalIngredientFatInput)     modalIngredientFatInput.value = nutrientValues.fat;
    if (modalIngredientCarbsInput)   modalIngredientCarbsInput.value = nutrientValues.carbs;
    // if (modalIngredientWaterInput) modalIngredientWaterInput.value = nutrientValues.water; // Water is calculated
    if (modalIngredientFiberInput)   modalIngredientFiberInput.value = nutrientValues.fiber;
    if (modalIngredientSaltInput)    modalIngredientSaltInput.value = nutrientValues.salt;
    
    // Calculate and set initial water value
    calculateAndUpdateWaterPercentage(); 
    // Store the calculated water percentage in the previous state
    nutrientValues.water = modalIngredientWaterInput.value; 

    previousIngredientModalState.nutrients = nutrientValues;

    lastValidNutrientsOnError = null; // Clear stored state
    
    if (modifyIngredientModal) modifyIngredientModal.style.display = 'block';
    if (modalIngredientWeightInput) modalIngredientWeightInput.focus();
}

function closeModifyIngredientModal() {
    if (modifyIngredientModal) modifyIngredientModal.style.display = 'none';
    ingredientToModify = null; 
    previousIngredientModalState = {}; // Clear state on close
    clearModalFeedbackAndStyles(); // Clear feedback when closing
    
    lastValidNutrientsOnError = null; // Clear stored state
}

function clearModalFeedbackAndStyles() {
     if (modalIngredientFeedback) {
        modalIngredientFeedback.textContent = '';
        modalIngredientFeedback.className = 'modal-feedback'; // Reset classes
    }
    // Still clear the error state if feedback is cleared
    lastValidNutrientsOnError = null; 
    
    clearInputHighlighting();
}

// Close modal if user clicks outside of it
window.onclick = function(event) {
    // Only close if mousedown and mouseup/click are both on the modal background
    if (event.target == addCrewModal && mouseDownTargetOnWindow == addCrewModal) {
        closeAddCrewModal();
    } else if (event.target == addMealModal && mouseDownTargetOnWindow == addMealModal) {
        closeAddMealModal();
    } else if (event.target == addIngredientModal && mouseDownTargetOnWindow == addIngredientModal) {
        closeAddIngredientModal();
    } else if (event.target == setDaysModal && mouseDownTargetOnWindow == setDaysModal) {
        closeSetDaysModal();
    } else if (event.target == modifyIngredientModal && mouseDownTargetOnWindow == modifyIngredientModal) {
        closeModifyIngredientModal();
    }
}

// Listeners to track mousedown target and reset on mouseup
window.addEventListener('mousedown', (event) => {
    mouseDownTargetOnWindow = event.target;
});

window.addEventListener('mouseup', () => {
    // Reset the target after the click/mouseup cycle is potentially processed by window.onclick
    // Using a timeout allows the onclick handler to run first
    setTimeout(() => {
        mouseDownTargetOnWindow = null;
    }, 0);
});

// --- End Modal Management ---

// Crew Member Management
async function addCrewMember() {
    if (!currentAdventure) {
        alert('Please select an adventure first');
        return;
    }

    // Read values from modal inputs
    const params = new URLSearchParams({
        name: document.getElementById('modalCrewName').value,
        age: document.getElementById('modalCrewAge').value,
        height: document.getElementById('modalCrewHeight').value,
        weight: document.getElementById('modalCrewWeight').value,
        gender: document.getElementById('modalCrewGender').value,
        activity: document.getElementById('modalCrewActivity').value,
        strategy: document.getElementById('modalCrewStrategy').value
    });

    try {
        const response = await fetch(`${API_BASE_URL}/adventures/${currentAdventure.id}/crew?${params}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        if (!response.ok) {
            throw new Error('Failed to add crew member');
        }

        closeAddCrewModal(); // Close modal on success
        await refreshCurrentAdventure();
    } catch (error) {
        alert('Error adding crew member: ' + error.message);
        // Optionally keep modal open on error, or close it:
        // closeAddCrewModal(); 
    }
}

// Days Management
async function setDays() {
    if (!currentAdventure) {
        // Modal shouldn't open if no adventure, but double-check
        alert('Please select an adventure first');
        return;
    }

    const days = modalDaysInput.value; // Read from modal input
    if (!days || days < 1) {
        alert('Please enter a valid number of days (minimum 1).');
        modalDaysInput.focus();
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/adventures/${currentAdventure.id}/days?days=${days}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        if (!response.ok) {
            throw new Error('Failed to set days');
        }
        
        closeSetDaysModal(); // Close modal on success
        await refreshCurrentAdventure();
    } catch (error) {
        alert('Error setting days: ' + error.message);
        // Optionally keep modal open
    }
}

// Meal Management
async function addMeal() {
    if (!currentAdventure) {
        // Modal shouldn't open if no adventure, but double-check
        alert('Please select an adventure first'); 
        return;
    }

    const name = modalMealNameInput.value.trim(); // Read from modal input
    if (!name) {
        alert('Please enter a meal name');
        modalMealNameInput.focus(); // Focus the modal input field
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/adventures/${currentAdventure.id}/meals?name=${encodeURIComponent(name)}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        if (!response.ok) {
            throw new Error('Failed to add meal');
        }

        closeAddMealModal(); // Close modal on success
        await refreshCurrentAdventure();
    } catch (error) {
        alert('Error adding meal: ' + error.message);
        // Optionally keep modal open
    }
}

// Ingredient Management
async function addIngredient() {
    if (!currentAdventure || !selectedMeal) {
         // Modal shouldn't open if no meal, but double-check
        alert('Please select a meal first');
        return;
    }

    const name = modalIngredientNameInput.value.trim(); // Read from modal input
    if (!name) {
        alert('Please enter an ingredient name');
        modalIngredientNameInput.focus(); // Focus modal input
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/meals/${selectedMeal.child.id}/ingredients?name=${encodeURIComponent(name)}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        if (!response.ok) {
            throw new Error('Failed to add ingredient');
        }

        closeAddIngredientModal(); // Close modal on success
        await refreshCurrentAdventure();
    } catch (error) {
        alert('Error adding ingredient: ' + error.message);
         // Optionally keep modal open
    }
}

// NEW function to update ingredient via modal
async function updateIngredient() {
    const ingredientId = modalIngredientModifyId.value;
    const newWeightValue = modalIngredientWeightInput.value;

    // Read nutrient values (as percentages)
    const proteinPercent = parseFloat(modalIngredientProteinInput.value) || 0;
    const fatPercent = parseFloat(modalIngredientFatInput.value) || 0;
    const carbsPercent = parseFloat(modalIngredientCarbsInput.value) || 0;
    // const waterPercent = parseFloat(modalIngredientWaterInput.value) || 0; // Don't read the disabled input directly
    const fiberPercent = parseFloat(modalIngredientFiberInput.value) || 0;
    const saltPercent = parseFloat(modalIngredientSaltInput.value) || 0;
    
    // Ensure water is calculated *before* validation and sending
    calculateAndUpdateWaterPercentage();
    const waterPercent = parseFloat(modalIngredientWaterInput.value) || 0; // Read the calculated value

    // Clear previous feedback before attempting update
    clearModalFeedbackAndStyles();

    if (!ingredientId) {
        setModalFeedback('Error: Cannot identify ingredient to modify.', true);
        return;
    }
    if (!selectedMeal || !selectedMeal.child) {
        setModalFeedback('Error: No meal selected.', true);
        return;
    }
    
    if (newWeightValue === '' || newWeightValue === null || parseFloat(newWeightValue) < 0) {
        setModalFeedback('Please enter a valid weight (minimum 0).', true);
        modalIngredientWeightInput.focus();
        return;
    }
    const newWeight = parseFloat(newWeightValue);
    
    const nutrientsPercent = [proteinPercent, fatPercent, carbsPercent, waterPercent, fiberPercent, saltPercent];
    if (nutrientsPercent.some(n => n < 0 || n > 100)) {
         setModalFeedback('Nutrient percentages must be between 0 and 100.', true);
         return;
    }
    // --- Simplified validation as backend handles sum check ---
     const nutrientsToCheck = [proteinPercent, fatPercent, carbsPercent, /*waterPercent,*/ fiberPercent, saltPercent]; // Don't check calculated water here
     if (nutrientsToCheck.some(n => n < 0 || n > 100)) {
          setModalFeedback('Editable nutrient percentages must be between 0 and 100.', true);
          return;
     }

    // Convert percentages to ratios for API call (including calculated water)
    const proteinRatio = proteinPercent / 100;
    const fatRatio = fatPercent / 100;
    const carbsRatio = carbsPercent / 100;
    const waterRatio = waterPercent / 100; // Use the calculated value
    const fiberRatio = fiberPercent / 100;
    const saltRatio = saltPercent / 100;

    const mealId = selectedMeal.child.id;

    const params = new URLSearchParams();
    params.append('weight', newWeight);
    params.append('protein', proteinRatio);
    params.append('fat', fatRatio);
    params.append('carbs', carbsRatio);
    params.append('water', waterRatio);
    params.append('fiber', fiberRatio);
    params.append('salt', saltRatio);

    try {
        const response = await fetch(`${API_BASE_URL}/meals/${mealId}/ingredients/${ingredientId}?${params.toString()}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' }
        });

        const responseData = await response.json(); // Assume backend sends JSON on success or error

        if (!response.ok) {
            // Create a custom error object containing the response data
            const error = new Error(responseData.message || `HTTP error! status: ${response.status}`);
            error.data = responseData; // Attach the full data
            throw error; // Throw the custom error object
        }

        // --- Success --- 
        setModalFeedback('Ingredient updated successfully!', false);
        // No longer need to hide reset button here
        // if(modalResetButton) modalResetButton.style.display = 'none'; 
        lastValidNutrientsOnError = null; // Clear stored state on success

        // Update the modal fields with the *returned* normalized/stolen values
        const updatedIngredient = responseData; // The updated ingredient object from backend
        const updatedNutrients = updatedIngredient.nutrientsMap || {};
        const updatedWeight = updatedIngredient.weight; // Backend endpoint returns the Ingredient, not ChildWrapper. Need weight if available.
        // NOTE: The backend currently returns the Ingredient object which DOES NOT have recipeWeight.
        // We'll update with the weight SENT for now, assuming it was accepted.
        // A better approach might be for the backend to return the updated ChildWrapper or the specific weight.
        const currentReturnedWeight = newWeight; 

        if (modalIngredientWeightInput) modalIngredientWeightInput.value = currentReturnedWeight;

        const returnedNutrientsPercent = {
            protein: ((updatedNutrients.protein || 0) * 100).toFixed(1),
            fat:     ((updatedNutrients.fat || 0) * 100).toFixed(1),
            carbs:   ((updatedNutrients.carbs || 0) * 100).toFixed(1),
            water:   ((updatedNutrients.water || 100) * 100).toFixed(1), // Use backend value for water display
            fiber:   ((updatedNutrients.fiber || 0) * 100).toFixed(1),
            salt:    ((updatedNutrients.salt || 0) * 100).toFixed(1)
        };
        
        if (modalIngredientProteinInput) modalIngredientProteinInput.value = returnedNutrientsPercent.protein;
        if (modalIngredientFatInput)     modalIngredientFatInput.value = returnedNutrientsPercent.fat;
        if (modalIngredientCarbsInput)   modalIngredientCarbsInput.value = returnedNutrientsPercent.carbs;
        if (modalIngredientWaterInput)   modalIngredientWaterInput.value = returnedNutrientsPercent.water; // Update disabled field too
        if (modalIngredientFiberInput)   modalIngredientFiberInput.value = returnedNutrientsPercent.fiber;
        if (modalIngredientSaltInput)    modalIngredientSaltInput.value = returnedNutrientsPercent.salt;

        // Highlight changes based on comparison with 'previousIngredientModalState'
        highlightInputChanges(currentReturnedWeight, returnedNutrientsPercent);

        // Update the 'previous' state for the *next* comparison
        previousIngredientModalState.weight = currentReturnedWeight;
        previousIngredientModalState.nutrients = returnedNutrientsPercent; // Store the full returned state
        
        // Update ingredientToModify state if needed for other logic (though maybe not strictly necessary here)
        // ingredientToModify = ??? // Backend needs to return ChildWrapper for this to be accurate

        // Refresh the main adventure view in the background AFTER modal update
        await refreshCurrentAdventure(); 
        
        // DO NOT close the modal automatically
        // closeModifyIngredientModal(); 

    } catch (error) {
        // --- Error --- 
        let errorMessage = "An unexpected error occurred.";
        let currentNutrients = null;
        
        // Check if it's our custom error with data attached
        if (error.data && error.data.message) {
            errorMessage = error.data.message;
            currentNutrients = error.data.currentNutrients;
        } else if (error.message) { // Fallback for other errors (network, etc.)
            errorMessage = error.message;
        }
        
        setModalFeedback('Error: ' + errorMessage, true);
		console.error('Update Ingredient Error:', errorMessage, error.data || error); // Log details

        // Clear previous error state before potentially setting a new one
        lastValidNutrientsOnError = null; 

        // If backend provided current nutrient state, reset the modal inputs automatically 
        // AND store this state for the reset button
        if (currentNutrients) {
            console.log("Resetting modal inputs based on backend state:", currentNutrients);
            updateModalInputsFromNutrients(currentNutrients); // Use helper function
            calculateAndUpdateWaterPercentage();
            updatePreviousStateFromNutrients(currentNutrients); // Update previous state too
            
            // Store the error state for the reset button
            lastValidNutrientsOnError = { ...currentNutrients }; 
        }
    }
}

// Helper to set modal feedback message and style
function setModalFeedback(message, isError) {
    if (modalIngredientFeedback) {
        modalIngredientFeedback.textContent = message;
        modalIngredientFeedback.className = 'modal-feedback'; // Reset
        if (isError) {
            modalIngredientFeedback.classList.add('error');
        } else {
            modalIngredientFeedback.classList.add('success');
        }
    }
}

// Helper function to compare and highlight inputs
function highlightInputChanges(newWeight, newNutrientsPercent) {
    const tolerance = 0.01; // Tolerance for float comparison
    
    // Weight
    if (modalIngredientWeightInput) {
        const oldWeight = previousIngredientModalState.weight || 0;
        if (Math.abs(newWeight - oldWeight) > tolerance) {
            modalIngredientWeightInput.classList.add(newWeight > oldWeight ? 'input-increased' : 'input-decreased');
        } else {
             modalIngredientWeightInput.classList.remove('input-increased', 'input-decreased');
        }
    }
    
    // Nutrients (including water)
    const nutrientInputsMap = {
        protein: modalIngredientProteinInput,
        fat:     modalIngredientFatInput,
        carbs:   modalIngredientCarbsInput,
        water:   modalIngredientWaterInput, // Include water input for highlighting
        fiber:   modalIngredientFiberInput,
        salt:    modalIngredientSaltInput
    };

    for (const key in newNutrientsPercent) {
        const inputElement = nutrientInputsMap[key];
        // Check if previous state and the specific nutrient value exist before comparing
        if (inputElement && previousIngredientModalState.nutrients && previousIngredientModalState.nutrients[key] !== undefined) {
            const oldValue = parseFloat(previousIngredientModalState.nutrients[key]) || 0;
            const newValue = parseFloat(newNutrientsPercent[key]) || 0;
            inputElement.classList.remove('input-increased', 'input-decreased'); // Reset first
            // Use a tolerance for floating point comparisons
            if (Math.abs(newValue - oldValue) > tolerance) { 
                 inputElement.classList.add(newValue > oldValue ? 'input-increased' : 'input-decreased');
            }
        } else if (inputElement) {
             // If previous state didn't have the key, or it was undefined, just remove classes
             inputElement.classList.remove('input-increased', 'input-decreased');
        }
    }
}

// UI Updates
function updateAdventureDisplay() {
    const adventureInfoDiv = document.getElementById('adventureInfo');
    const selectedMealSection = document.getElementById('selectedMealDetails').closest('.info-group'); // Get parent info-group
    const selectedIngredientSection = document.getElementById('selectedIngredientDetails').closest('.info-group'); // Get parent info-group
    const selectedMealDetailsDiv = document.getElementById('selectedMealDetails');
    const selectedIngredientDetailsDiv = document.getElementById('selectedIngredientDetails');

    if (!currentAdventure) {
        if (adventureTitle) adventureTitle.textContent = 'Adventure Information';
        if (setDaysButton) setDaysButton.style.display = 'none'; // Hide Set Days button
        if (adventureInfoDiv) adventureInfoDiv.style.display = 'none';
        if (selectedMealSection) selectedMealSection.style.display = 'none'; // Hide whole section
        if (selectedIngredientSection) selectedIngredientSection.style.display = 'none'; // Hide whole section
        if (addIngredientButton) addIngredientButton.style.display = 'none'; // Hide add ingredient button
        // Clear basic info displays if no adventure is selected
        document.getElementById('adventureNameDisplay').textContent = '-';
        document.getElementById('adventureDuration').textContent = '-';
        document.getElementById('adventureCrewSize').textContent = '-';
        document.getElementById('adventureCrewDailyKcalNeed').textContent = '-';
        document.getElementById('adventureWeight').textContent = '-';
        document.getElementById('adventureEnergyDensity').textContent = '-';
        return;
    }

    // --- Update Adventure Section --- 
    if (adventureTitle) adventureTitle.textContent = `Adventure: ${currentAdventure.name || 'Unnamed'}`;
    if (setDaysButton) setDaysButton.style.display = 'inline-block'; // Show Set Days button
    if (adventureInfoDiv) adventureInfoDiv.style.display = 'block'; // Show the main info block
    
    // Update Adventure Basic Info
    document.getElementById('adventureNameDisplay').textContent = currentAdventure.name || '-';
    document.getElementById('adventureDuration').textContent = `${currentAdventure.days || 0} days`;
    document.getElementById('adventureCrewSize').textContent = `${currentAdventure.crewSize || 0} persons`;
    document.getElementById('adventureCrewDailyKcalNeed').textContent = `${currentAdventure.crewDailyKcalNeed || 0} kCal / day`;
    document.getElementById('adventureWeight').textContent = `${(currentAdventure.weight || 0).toFixed(3)} kg`;
    document.getElementById('adventureEnergyDensity').textContent = `${currentAdventure.formattedEnergyDensity || '0.0'} kCal / kg`;

    // Render Adventure Nutrients
    renderNutrients(currentAdventure.nutrientsMap, 'adventureNutrients');

    // Update Crew Members
    const crewList = document.getElementById('crewMembersList');
    crewList.innerHTML = '';
    if (currentAdventure.allCrewMembers && Array.isArray(currentAdventure.allCrewMembers) && currentAdventure.allCrewMembers.length > 0) {
        currentAdventure.allCrewMembers.forEach(member => {
            if (member) {
                const card = document.createElement('div');
                card.className = 'crew-card';
                card.innerHTML = `
                    <div class="card-header">
                        <h4>${member.name || 'Unknown'}</h4>
                        <button class="remove-button" onclick="removeCrewMember('${member.id}')">
                            <img src="/graphics/icons/trash.svg" alt="Remove" class="icon-button">
                        </button>
                    </div>
                    <p>Age: ${member.age || 0}</p>
                    <p>Height: ${member.height || 0} cm</p>
                    <p>Weight: ${member.weight || 0} kg</p>
                    <p>Gender: ${member.gender || 'Unknown'}</p>
                    <p>Activity: ${member.activity || 'Unknown'}</p>
                    <p>Strategy: ${member.strategy || 'Unknown'}</p>
                    <p>Daily KCal Need: ${member.dailyKCalNeed || 0}</p>
                `;
                crewList.appendChild(card);
            }
        });
    } else {
         crewList.innerHTML = '<p>No crew members added yet.</p>';
    }

    // Update Meals
    const mealsList = document.getElementById('mealsList');
    mealsList.innerHTML = '';
    if (currentAdventure.allChildren && Array.isArray(currentAdventure.allChildren) && currentAdventure.allChildren.length > 0) {
        currentAdventure.allChildren.forEach(mealData => {
            if (mealData && mealData.child) {
                const meal = mealData.child;
                const card = document.createElement('div');
                card.dataset.mealId = meal.id;
                card.className = 'meal-card' + (selectedMeal && selectedMeal.child && selectedMeal.child.id === meal.id ? ' selected' : '');
                card.innerHTML = `
                    <div class="card-header">
                        <h4>${meal.name || 'Unknown'}</h4>
                        <button class="remove-button" onclick="event.stopPropagation(); removeMeal('${meal.id}')">
                            <img src="/graphics/icons/trash.svg" alt="Remove" class="icon-button">
                        </button>
                    </div>
                    <p>Calc. Weight: ${(currentAdventure.childWeights && currentAdventure.childWeights[meal.id] !== undefined ? currentAdventure.childWeights[meal.id].toFixed(3) : '0.000')} kg</p>
                    <p>Energy Density: ${meal.formattedEnergyDensity || '0.0'} kCal / kg</p>
                    <p>Ratio: ${Math.round(mealData.ratio*100) || '0.0'} %</p>
                    <p>${(meal.allChildren && Array.isArray(meal.allChildren) ? meal.allChildren.length : 0)} ingredients</p>
                `;
                // Pass the full meal object from currentAdventure to preserve nutrientMap
                const fullMealData = currentAdventure.allChildren.find(m => m.child && m.child.id === meal.id);
                card.onclick = () => selectMeal(fullMealData);
                mealsList.appendChild(card);
            }
        });
    } else {
        mealsList.innerHTML = '<p>No meals added yet.</p>';
    }

    // --- Update Selected Meal Section --- 
    if (selectedMeal && selectedMeal.child && selectedMeal.child.id) {
        // Find the *latest* version of the selected meal from the refreshed adventure data
        const currentMealData = currentAdventure.allChildren.find(m => m.child && m.child.id === selectedMeal.child.id);

        if (!currentMealData) { // Meal might have been deleted in the meantime
            if (selectedMealSection) selectedMealSection.style.display = 'none'; 
            if (selectedIngredientSection) selectedIngredientSection.style.display = 'none';
            if (addIngredientButton) addIngredientButton.style.display = 'none';
            selectedMeal = null;
            selectedIngredient = null;
            console.warn('Selected meal not found in current adventure data.');
            return; // Stop further processing for this meal
        }

        selectedMeal = currentMealData; // Update state with fresh data
        const meal = selectedMeal.child; // Use the up-to-date child object
        const mealRatio = selectedMeal.ratio; // Use the up-to-date ratio

        if (selectedMealSection) selectedMealSection.style.display = 'block'; // Show the section
        if (selectedMealDetailsDiv) selectedMealDetailsDiv.style.display = 'block'; // Show the details box
        if (selectedMealTitle) selectedMealTitle.textContent = `Meal: ${meal.name || 'Unnamed'}`; 
        if (addIngredientButton) addIngredientButton.style.display = 'inline-block'; // Show Add Ingredient button

        document.getElementById('selectedMealName').textContent = meal.name || 'Unknown';
        // Use the weight from the adventure's childWeights map
        const calculatedMealWeight = (currentAdventure.childWeights && currentAdventure.childWeights[meal.id] !== undefined)
            ? currentAdventure.childWeights[meal.id]
            : 0;
        document.getElementById('selectedMealWeight').textContent = `${calculatedMealWeight.toFixed(3)} kg`;
        document.getElementById('selectedMealEnergyDensity').textContent = `${meal.formattedEnergyDensity || '0.0'} kCal / kg`;
        document.getElementById('selectedMealRatio').textContent = `${(mealRatio * 100).toFixed(1)} %`;

        // Render Meal Nutrients
        renderNutrients(meal.nutrientsMap, 'selectedMealNutrients');

        // Update Ingredients List (Now uses cards)
        const ingredientsListDiv = document.getElementById('selectedMealIngredients');
        ingredientsListDiv.innerHTML = ''; // Clear previous list/cards
        if (meal.allChildren && Array.isArray(meal.allChildren) && meal.allChildren.length > 0) {
            meal.allChildren.forEach(ingredientData => {
                if (ingredientData && ingredientData.child) {
                    const ingredient = ingredientData.child;
                    const card = document.createElement('div');
                    card.className = 'ingredient-card' + (selectedIngredient && selectedIngredient.child.id === ingredient.id ? ' selected' : '');
                    card.dataset.ingredientId = ingredient.id;
                    card.innerHTML = `
                        <div class="card-header">
                            <h4>${ingredient.name || 'Unknown'}</h4>
                            <div class="button-group">
                                <button class="modify-button" onclick="event.stopPropagation(); openModifyIngredientModal(JSON.parse(this.dataset.ingredientData))";>Modify</button>
                                <button class="remove-button" onclick="event.stopPropagation(); removeIngredient('${ingredient.id}')">
                                    <img src="/graphics/icons/trash.svg" alt="Remove" class="icon-button">
                                </button>
                            </div>
                        </div>
                        <p>Recipe Weight: ${ingredientData.recipeWeight || 0} g</p>
                        <p>Recipe W. Ratio: ${(ingredientData.ratio * 100).toFixed(1)} %</p>
                        <p>Calc. value: ${(currentAdventure.ingredientWeights && currentAdventure.ingredientWeights[ingredient.id] !== undefined ? currentAdventure.ingredientWeights[ingredient.id].toFixed(3) : '0.000')} kg</p>
                    `;
                    // Store the full ingredient data on the modify button for the modal
                    card.querySelector('.modify-button').dataset.ingredientData = JSON.stringify(ingredientData);
                    
                    // Still allow clicking the card to select for detail view (optional)
                    card.onclick = (e) => {
                         // Prevent card click if a button inside was clicked
                        if (e.target.tagName !== 'BUTTON') {
                            selectIngredient(ingredientData); 
                        }
                    };
                    ingredientsListDiv.appendChild(card);
                }
            });
        } else {
             ingredientsListDiv.innerHTML = '<p>No ingredients added yet.</p>';
        }
    } else {
        // Hide meal details if no meal is selected
        if (selectedMealSection) selectedMealSection.style.display = 'none'; // Hide whole section
        if (selectedMealDetailsDiv) selectedMealDetailsDiv.style.display = 'none';
        if (selectedMealTitle) selectedMealTitle.textContent = 'Selected Meal Details'; // Reset title
        if (addIngredientButton) addIngredientButton.style.display = 'none'; // Hide Add Ingredient button
        
        // Also hide ingredient details when meal is deselected
        if (selectedIngredientSection) selectedIngredientSection.style.display = 'none';
        if (selectedIngredientDetailsDiv) selectedIngredientDetailsDiv.style.display = 'none';
        if (selectedIngredientTitle) selectedIngredientTitle.textContent = 'Selected Ingredient Details'; // Reset title
        selectedIngredient = null; // Reset selected ingredient state
    }

    // --- Update Selected Ingredient Section --- 
    if (selectedIngredient && selectedIngredient.child && selectedIngredient.child.id) {
        // Find the latest version from the *selected meal's* children
        const currentIngredientData = selectedMeal?.child?.allChildren?.find(i => i.child && i.child.id === selectedIngredient.child.id);
        
        if (!currentIngredientData) {
            if (selectedIngredientSection) selectedIngredientSection.style.display = 'none';
            selectedIngredient = null;
             console.warn('Selected ingredient not found in current meal data.');
             // Don't return here, just ensure the section is hidden
        } else { 
             selectedIngredient = currentIngredientData; // Update state
             const ingredient = selectedIngredient.child;
             const ingredientRatio = selectedIngredient.ratio;
             const ingredientWeight = selectedIngredient.recipeWeight;
     
             if (selectedIngredientSection) selectedIngredientSection.style.display = 'block'; // Show section
             if (selectedIngredientDetailsDiv) selectedIngredientDetailsDiv.style.display = 'flex'; // Use flex for two-column
             if (selectedIngredientTitle) selectedIngredientTitle.textContent = `Ingredient: ${ingredient.name || 'Unnamed'}`;
     
             document.getElementById('selectedIngredientName').textContent = ingredient.name || 'Unknown';
             document.getElementById('selectedIngredientWeight').textContent = `${ingredientWeight || 0}g`;
             document.getElementById('selectedIngredientRatio').textContent = `${(ingredientRatio * 100).toFixed(1)}%`;
             const calculatedIngredientWeight = (currentAdventure.ingredientWeights && currentAdventure.ingredientWeights[ingredient.id] !== undefined)
                ? currentAdventure.ingredientWeights[ingredient.id]
                : 0;
             document.getElementById('selectedIngredientCalcWeight').textContent = `${calculatedIngredientWeight.toFixed(3)} kg`;
     
             // Render Ingredient Nutrients
             renderNutrients(ingredient.nutrientsMap, 'selectedIngredientNutrients');
        }
    } else {
        // Hide ingredient details if no ingredient is selected
        if (selectedIngredientSection) selectedIngredientSection.style.display = 'none';
        if (selectedIngredientDetailsDiv) selectedIngredientDetailsDiv.style.display = 'none';
         if (selectedIngredientTitle) selectedIngredientTitle.textContent = 'Selected Ingredient Details'; // Reset title
   }
}

function selectMeal(mealData) {
    if (!mealData || !mealData.child || !mealData.child.id) {
        console.warn('Attempted to select invalid meal data:', mealData);
        selectedMeal = null;
        selectedIngredient = null; // Reset ingredient when meal changes
    } else {
        // If the same meal is clicked again, deselect it
        if (selectedMeal && selectedMeal.child && selectedMeal.child.id === mealData.child.id) {
            selectedMeal = null;
            selectedIngredient = null;
        } else {
             selectedMeal = mealData; // Store the whole meal data including ratio
             selectedIngredient = null; // Reset ingredient when meal changes
        }
    }
    
    // Update the visual selection state for meal cards
    document.querySelectorAll('.meal-card').forEach(card => {
        const mealId = card.dataset.mealId;
        if (selectedMeal && mealId && mealId === selectedMeal.child.id.toString()) {
            card.classList.add('selected');
        } else {
            card.classList.remove('selected');
        }
    });
    updateAdventureDisplay(); // Refresh the entire display
}

// Function to handle ingredient selection (by clicking card)
function selectIngredient(ingredientData) {
    if (!ingredientData || !ingredientData.child || !ingredientData.child.id) {
        console.warn('Attempted to select invalid ingredient data:', ingredientData);
        selectedIngredient = null;
    } else {
         // If the same ingredient is clicked again, deselect it
        if (selectedIngredient && selectedIngredient.child && selectedIngredient.child.id === ingredientData.child.id) {
            selectedIngredient = null;
        } else {
            selectedIngredient = ingredientData;
        }
    }
    
    // Update visual selection in the list
    document.querySelectorAll('.ingredient-card').forEach(item => {
         if (selectedIngredient && item.dataset.ingredientId === selectedIngredient.child.id.toString()) {
             item.classList.add('selected');
         } else {
             item.classList.remove('selected');
         }
     });

    updateAdventureDisplay(); // Refresh display to show ingredient details
}

// Fetch and display adventures (Now handled by initializeDemo)
/*
async function fetchAdventures() {
    // ... (kept for reference, but not called)
}
*/

// Fetch and display crew members (Now handled within adventure display)
/*
async function fetchCrewMembers() {
    // ... (kept for reference, but not called)
}
*/

// Fetch and display base classes (Not currently used in this layout)
/*
async function fetchBaseClasses() {
    // ... (kept for reference, but not called)
}
*/

// Initialize the demo
async function initializeDemo() {
    try {
        // Fetch existing adventures
        const response = await fetch(`${API_BASE_URL}/adventures`);
        if (response.ok) {
            adventures = await response.json();
            updateAdventureDropdown();
            // Optionally select the first adventure by default?
            // if (adventures.length > 0) {
            //    await selectAdventure(adventures[0].id);
            // }
        } else {
             throw new Error('Failed to fetch initial adventures');
        }
    } catch (error) {
        console.error('Error initializing demo:', error);
        alert('Failed to load initial adventure data. Please check the API connection.');
    }

    // Add Enter key listeners for modal inputs
    if (modalMealNameInput) {
        modalMealNameInput.addEventListener('keypress', function(event) {
            if (event.key === 'Enter') {
                event.preventDefault(); // Prevent default form submission (if any)
                addMeal();
            }
        });
    }
    if (modalIngredientNameInput) {
        modalIngredientNameInput.addEventListener('keypress', function(event) {
             if (event.key === 'Enter') {
                event.preventDefault();
                addIngredient();
            }
        });
    }
    if (modalDaysInput) { // Add listener for days input
         modalDaysInput.addEventListener('keypress', function(event) {
             if (event.key === 'Enter') {
                event.preventDefault();
                setDays();
            }
        });
    }
     if (modalIngredientWeightInput) { // Add listener for modify ingredient weight
         modalIngredientWeightInput.addEventListener('keypress', function(event) {
             if (event.key === 'Enter') {
                event.preventDefault();
                updateIngredient();
            }
        });
    }

    // Add Escape key listener for modals
    document.addEventListener('keydown', function(event) {
        if (event.key === "Escape") {
            if (addCrewModal && addCrewModal.style.display === 'block') {
                closeAddCrewModal();
            } else if (addMealModal && addMealModal.style.display === 'block') {
                closeAddMealModal();
            } else if (addIngredientModal && addIngredientModal.style.display === 'block') {
                closeAddIngredientModal();
            } else if (setDaysModal && setDaysModal.style.display === 'block') {
                closeSetDaysModal();
            } else if (modifyIngredientModal && modifyIngredientModal.style.display === 'block') {
                 closeModifyIngredientModal();
            }
        }
    });

    // Ensure the adventure info is hidden initially by updateAdventureDisplay
    updateAdventureDisplay(); 

    // Add Enter key listeners AND INPUT listeners for modify modal nutrient inputs
    const nutrientInputs = [
        modalIngredientProteinInput,
        modalIngredientFatInput,
        modalIngredientCarbsInput,
        // modalIngredientWaterInput, // No listener needed for disabled input
        modalIngredientFiberInput,
        modalIngredientSaltInput
    ];

    nutrientInputs.forEach(input => {
        if (input) {
            // Recalculate water on input change
            input.addEventListener('input', calculateAndUpdateWaterPercentage); 
            
            // Trigger update on Enter key press
            input.addEventListener('keypress', function(event) {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    updateIngredient();
                }
            });
        }
    });
    
    // Add listener for weight input too, as Enter should trigger update
     if (modalIngredientWeightInput) { 
         modalIngredientWeightInput.addEventListener('keypress', function(event) {
             if (event.key === 'Enter') {
                event.preventDefault();
                updateIngredient();
            }
        });
    }

    // Add Enter key listeners for Crew Member modal inputs
    const crewModalInputs = [
        document.getElementById('modalCrewName'),
        document.getElementById('modalCrewAge'),
        document.getElementById('modalCrewHeight'),
        document.getElementById('modalCrewWeight'),
        // Select elements don't usually trigger keypress for submit, but included for completeness if desired
        // document.getElementById('modalCrewGender'), 
        // document.getElementById('modalCrewActivity'), 
        // document.getElementById('modalCrewStrategy')
    ];

    crewModalInputs.forEach(input => {
        if (input) {
            input.addEventListener('keypress', function(event) {
                if (event.key === 'Enter') {
                    event.preventDefault(); 
                    addCrewMember(); 
                }
            });
        }
    });
}

// Start the demo when the page loads
document.addEventListener('DOMContentLoaded', initializeDemo);

// Crew Member Removal
async function removeCrewMember(crewId) {
    if (!currentAdventure) {
        alert('Please select an adventure first');
        return;
    }
    
    // Find crew member name for confirmation (optional but nice)
    const member = currentAdventure.allCrewMembers.find(m => m.id === crewId);
    const confirmMsg = member 
        ? `Are you sure you want to remove crew member "${member.name}"?` 
        : 'Are you sure you want to remove this crew member?';

    if (!confirm(confirmMsg)) return;

    try {
        const response = await fetch(`${API_BASE_URL}/adventures/${currentAdventure.id}/crew/${crewId}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error('Failed to remove crew member');
        }

        await refreshCurrentAdventure();
    } catch (error) {
        alert('Error removing crew member: ' + error.message);
    }
}

// Meal Removal
async function removeMeal(mealId) {
    if (!currentAdventure) {
        alert('Please select an adventure first');
        return;
    }

    // Find meal name for confirmation
    const mealData = currentAdventure.allChildren.find(m => m.child && m.child.id === mealId);
    const confirmMsg = mealData && mealData.child
        ? `Are you sure you want to remove meal "${mealData.child.name}"?`
        : 'Are you sure you want to remove this meal?';
    
    if (!confirm(confirmMsg)) return;

    try {
        const response = await fetch(`${API_BASE_URL}/adventures/${currentAdventure.id}/meals/${mealId}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error('Failed to remove meal');
        }

        // If the removed meal was the selected one, reset selection
        if (selectedMeal && selectedMeal.child && selectedMeal.child.id === mealId) {
            selectedMeal = null;
            selectedIngredient = null; // Also reset ingredient if its meal is removed
        }
        await refreshCurrentAdventure(); // This will trigger updateAdventureDisplay
    } catch (error) {
        alert('Error removing meal: ' + error.message);
    }
}

// Ingredient Removal
async function removeIngredient(ingredientId) {
    if (!selectedMeal || !selectedMeal.child) { // Check if a meal is selected
        alert('Cannot remove ingredient: No meal selected.');
        return;
    }

    const mealId = selectedMeal.child.id; // Get meal ID from state

     // Find ingredient name for confirmation
    const ingredientData = selectedMeal?.child?.allChildren?.find(i => i.child && i.child.id === ingredientId);
    const confirmMsg = ingredientData && ingredientData.child
        ? `Are you sure you want to remove ingredient "${ingredientData.child.name}" from meal "${selectedMeal.child.name}"?`
        : `Are you sure you want to remove this ingredient from meal "${selectedMeal.child.name}"?`;

    if (!confirm(confirmMsg)) return;

    try {
        const response = await fetch(`${API_BASE_URL}/meals/${mealId}/ingredients/${ingredientId}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error('Failed to remove ingredient');
        }

        // If the removed ingredient was selected, reset selection
        if (selectedIngredient && selectedIngredient.child.id === ingredientId) {
            selectedIngredient = null;
        }

        await refreshCurrentAdventure(); // Refresh to update meal/adventure state
    } catch (error) {
        alert('Error removing ingredient: ' + error.message);
    }
}

// NEW Function called by the Reset Fields button
function resetModalFieldsToLastValidState() {
    // Priority 1: Use the specific state provided by the last error, if available
    if (lastValidNutrientsOnError) {
        console.log("Resetting modal inputs to state from last error:", lastValidNutrientsOnError);
        updateModalInputsFromNutrients(lastValidNutrientsOnError); 
        // Update previous state to match the error state we just restored
        updatePreviousStateFromNutrients(lastValidNutrientsOnError);
    }
    // Priority 2: Fallback to the state stored when the modal was opened
    else if (previousIngredientModalState && previousIngredientModalState.nutrients) {
         console.log("Resetting modal inputs to state from modal open:", previousIngredientModalState);
         // Restore weight from previous state as well
         if (modalIngredientWeightInput && previousIngredientModalState.weight !== undefined) {
             modalIngredientWeightInput.value = previousIngredientModalState.weight;
         }
         // Restore nutrients from previous state
         updateModalInputsFromNutrients(previousIngredientModalState.nutrients, true); // Pass true to indicate use % map
    } 
    else {
        console.warn("Reset button clicked but no valid state available to restore.");
        return; // Nothing to do
    }

    // Clear feedback and recalculate water after resetting
    if(modalIngredientFeedback) {
        modalIngredientFeedback.textContent = '';
        modalIngredientFeedback.className = 'modal-feedback';
    }
    // calculateAndUpdateWaterPercentage(); // REMOVE: This recalculates water unnecessarily after reset
    // After resetting, clear the error state
    lastValidNutrientsOnError = null; 
}

// Helper function to update modal inputs from a nutrient map
// Added flag 'isPercentageMap' for handling previousIngredientModalState format
function updateModalInputsFromNutrients(nutrientMap, isPercentageMap = false) {
     const nutrientInputsMap = {
        protein: modalIngredientProteinInput,
        fat:     modalIngredientFatInput,
        carbs:   modalIngredientCarbsInput,
        water:   modalIngredientWaterInput, // Update the disabled water field too
        fiber:   modalIngredientFiberInput,
        salt:    modalIngredientSaltInput
    };

    for (const key in nutrientInputsMap) {
        const inputElement = nutrientInputsMap[key];
        if (inputElement && nutrientMap[key] !== undefined) {
            // If it's already a percentage string (from previous state), use directly
            // Otherwise, convert ratio to percentage string
            inputElement.value = isPercentageMap ? nutrientMap[key] : (nutrientMap[key] * 100).toFixed(1);
        }
    }
    // Ensure input highlighting is cleared
    clearInputHighlighting(); 
}

// Helper function to update previousIngredientModalState from nutrients (ratios)
function updatePreviousStateFromNutrients(nutrientRatioMap) {
    previousIngredientModalState.nutrients = {};
    const nutrientKeys = ["protein", "fat", "carbs", "water", "fiber", "salt"];
    for (const key of nutrientKeys) {
        if (nutrientRatioMap[key] !== undefined) {
            previousIngredientModalState.nutrients[key] = (nutrientRatioMap[key] * 100).toFixed(1);
        }
    }
    // Note: Weight is not updated here
}

// Helper function to clear input highlighting only
function clearInputHighlighting() {
    const inputsToClear = [
        modalIngredientWeightInput,
        modalIngredientProteinInput,
        modalIngredientFatInput,
        modalIngredientCarbsInput,
        modalIngredientWaterInput,
        modalIngredientFiberInput,
        modalIngredientSaltInput
    ];
    inputsToClear.forEach(input => {
        if(input) {
            input.classList.remove('input-increased', 'input-decreased');
        }
    });
} 