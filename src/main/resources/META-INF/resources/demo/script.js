// API endpoints
const API_BASE_URL = window.location.origin;

// State variables
let adventures = [];  // Store all adventures
let currentAdventure = null;
let selectedMeal = null;
let selectedIngredient = null; // Add state for selected ingredient
let ingredientToModify = null; // Temp store for ingredient being modified
let lastAdventureData = null;  // Store last fetched adventure data for comparison
let previousIngredientModalState = {}; // Store previous values for comparison
let selectedExistingIngredientId = null; // Store ID of ingredient selected from search
let lastValidNutrientsOnError = null; // Stores nutrient state for the reset button if an update fails
let availableMeasurementUnits = []; // Holds array of {name, standardGrams, isVolume}
let selectedMeasurementUnit = 'GRAM'; // Tracks selected unit NAME (string)
let currentPcsWeight = null; // Store the current pcsWeight for the ingredient being modified
let currentDensity = 1.0; // Store the current density (g/ml)

// Unit Abbreviations
const UNIT_ABBREVIATIONS = {
    GRAM: "g",
    KILOGRAM: "kg",
    PCS: "g/pcs",
    TEASPOON: "tsp",
    TABLESPOON: "tbsp",
    CUP: "cup",
    MILLILITER: "ml",
    LITER: "l"
};


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
const modalIngredientDensityInput = document.getElementById('modalIngredientDensity'); // Modify modal density input
const modalIngredientDensityUpdateButton = document.getElementById('modalIngredientDensityUpdateButton'); // Density update button
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
const modalIngredientFeedback = document.getElementById('modalIngredientFeedback'); // Feedback element in modify modal
const modalResetButton = document.getElementById('modalResetButton'); // Reset button in modify modal
// Unit-related elements in modify modal
const modalIngredientConvertedValue = document.getElementById('modalIngredientConvertedValue');
const modalIngredientUnitButton = document.getElementById('modalIngredientUnitButton');
const modalIngredientUnitDropdown = document.getElementById('modalIngredientUnitDropdown');
const modalIngredientUnitDropdownContainer = document.getElementById('modalIngredientUnitDropdownContainer');
const modalIngredientPcsButton = document.getElementById('modalIngredientPcsButton');

// New DOM elements for Add Ingredient Modal Search
const modalIngredientSearchInput = document.getElementById('modalIngredientSearch');
const searchExistingIngredientButton = document.getElementById('searchExistingIngredientButton');
const ingredientSearchResultsDiv = document.getElementById('ingredientSearchResults');
const loadSelectedIngredientButton = document.getElementById('loadSelectedIngredientButton');
const modalExistingIngredientFeedback = document.getElementById('modalExistingIngredientFeedback');
const addNewIngredientButton = document.getElementById('addNewIngredientButton'); // Reference to the Add New button

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

// Fetches measurement unit configurations from the backend.
async function fetchMeasurementUnits() {
	const response = await makeApiCall(`${API_BASE_URL}/configuration/measurement-units`);
	if (response.success && Array.isArray(response.data)) {
		availableMeasurementUnits = response.data; // Store the array of objects
		console.log('Fetched Measurement Unit Configs:', availableMeasurementUnits);
	} else {
		console.error('Failed to fetch measurement units:', response.error);
		alert('Error: Could not load measurement units configuration from the server.');
		// Fallback with basic structure if API fails
		availableMeasurementUnits = [
			{ name: 'GRAM', standardGrams: 1.0, isVolume: false },
			{ name: 'PCS', standardGrams: null, isVolume: false },
			{ name: 'KILOGRAM', standardGrams: 1000.0, isVolume: false }
		];
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
				const updatedMealData = getChildrenFromMap(currentAdventure.childMap).find(m => m.child && m.child.id === selectedMeal.child.id);
				selectedMeal = updatedMealData || null;
				if (selectedMeal && selectedIngredient && selectedIngredient.child) {
					const updatedIngredientData = getChildrenFromMap(selectedMeal.child.childMap).find(i => i.child && i.child.id === selectedIngredient.child.id);
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
function calculateAndUpdateWaterPercentage(force = false) {
	console.log('calculateAndUpdateWaterPercentage');
	// If the modal is not visible, don't calculate unless force is true
	if ((!modifyIngredientModal || modifyIngredientModal.style.display !== 'block') && !force) {
		console.log('modal not visible and function not forced');
		return; // Only calculate if the modal is visible    }
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

// Clears feedback message and input highlighting in the modify ingredient modal.
function clearModalFeedbackAndStyles() {
	if (modalIngredientFeedback) {
		modalIngredientFeedback.textContent = '';
		modalIngredientFeedback.className = 'modal-feedback'; // Reset classes
	}
	// Clear the error state if feedback is cleared
	lastValidNutrientsOnError = null;

	clearInputHighlighting();
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

	if (addCrewModal) addCrewModal.style.display = 'block';
	if (document.getElementById('modalCrewName')) document.getElementById('modalCrewName').focus(); // Focus Name field
}

function closeAddCrewModal() {
	if (addCrewModal) addCrewModal.style.display = 'none';
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
	// Clear previous inputs and state
	if (modalIngredientNameInput) modalIngredientNameInput.value = '';
	if (modalIngredientSearchInput) modalIngredientSearchInput.value = '';
	if (ingredientSearchResultsDiv) {
		ingredientSearchResultsDiv.innerHTML = '<p class="placeholder-text">Search results will appear here.</p>';
	}
	if (loadSelectedIngredientButton) loadSelectedIngredientButton.disabled = true;
	if (addNewIngredientButton) addNewIngredientButton.disabled = false; // Ensure Add New is enabled initially
	if (modalExistingIngredientFeedback) modalExistingIngredientFeedback.textContent = '';

	selectedExistingIngredientId = null; // Reset selected ID

	if (addIngredientModal) addIngredientModal.style.display = 'block';
	if (modalIngredientNameInput) modalIngredientNameInput.focus(); // Focus the 'new' input first
}

function closeAddIngredientModal() {
	if (addIngredientModal) addIngredientModal.style.display = 'none';
	// Clear state on close as well
	if (modalIngredientNameInput) modalIngredientNameInput.value = '';
	if (modalIngredientSearchInput) modalIngredientSearchInput.value = '';
	if (ingredientSearchResultsDiv) {
		ingredientSearchResultsDiv.innerHTML = '<p class="placeholder-text">Search results will appear here.</p>';
	}
	if (loadSelectedIngredientButton) loadSelectedIngredientButton.disabled = true;
	if (addNewIngredientButton) addNewIngredientButton.disabled = false;
	if (modalExistingIngredientFeedback) modalExistingIngredientFeedback.textContent = '';

	selectedExistingIngredientId = null;
}

function openSetDaysModal() {
	if (!currentAdventure) {
		alert('Please select an adventure first');
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

// Updates the label showing the weight converted to the selected unit.
function updateConvertedValueLabel() {
	if (!modalIngredientConvertedValue || !modalIngredientWeightInput) return;

	const weightInGrams = parseFloat(modalIngredientWeightInput.value) || 0;
	const unitConfig = availableMeasurementUnits.find(u => u.name === selectedMeasurementUnit);

	if (!unitConfig) {
		console.error(`Config not found for unit: ${selectedMeasurementUnit}`);
		modalIngredientConvertedValue.style.display = 'none';
		return;
	}

	modalIngredientConvertedValue.style.display = 'inline-block'; // Show label generally

	switch (unitConfig.name) {
		case 'GRAM':
			console.log('GRAM');
			modalIngredientUnitDropdownContainer.style.marginLeft = '';
			modalIngredientConvertedValue.style.display = 'none'; // Hide for GRAM
			modalIngredientConvertedValue.textContent = '';
			break;
		case 'PCS':
			modalIngredientUnitDropdownContainer.style.marginLeft = '60px';
			if (currentPcsWeight && currentPcsWeight > 0) {
				// Use a small tolerance for zero check due to potential floating point inaccuracies
				const pieceCount = weightInGrams / currentPcsWeight;
				modalIngredientConvertedValue.textContent = Math.abs(pieceCount) < 0.001 ? '0.00' : pieceCount.toFixed(1);
			} else {
				modalIngredientConvertedValue.textContent = '-';
			}
			break;
		default: // Handles KG, LITER, ML, TSP, TBSP, CUP, etc. based on config
			modalIngredientUnitDropdownContainer.style.marginLeft = '60px';
			let standardGrams = unitConfig.standardGrams;
			if (standardGrams === null || standardGrams === undefined) {
				console.warn(`Standard grams not defined for unit: ${unitConfig.name}`);
				modalIngredientConvertedValue.textContent = `(?)`; // Indicate missing config
				break;
			}

			let valueToShow = 0;
			if (unitConfig.isVolume) {
				// Volume unit: Convert grams to volume (ml) using density, then to target unit
				if (!currentDensity || currentDensity <= 0) {
					console.warn("Cannot calculate volume conversion without valid density (> 0).");
					modalIngredientConvertedValue.textContent = `(dens?)`; // Indicate missing density
					break;
				}
				const volumeInMl = weightInGrams / currentDensity;
				const mlPerUnit = standardGrams; // For volume units, standardGrams is the ml equivalent at 1g/ml density
				if (!mlPerUnit || mlPerUnit <= 0) {
					console.warn(`Standard volume (ml) is invalid for unit: ${unitConfig.name}`);
					modalIngredientConvertedValue.textContent = `(vol?)`;
					break;
				}
				valueToShow = volumeInMl / mlPerUnit;
			} else {
				// Weight unit: Direct conversion from grams
				valueToShow = weightInGrams / standardGrams;
			}

			// Use a small tolerance for zero check due to potential floating point inaccuracies
			const isEffectivelyZero = Math.abs(valueToShow) < 0.001;
			// Display with specific 0.00 format or reasonable precision otherwise
			modalIngredientConvertedValue.textContent = isEffectivelyZero
				? '0.00'
				: valueToShow.toFixed(valueToShow < 0.1 ? 3 : (valueToShow < 10 ? 2 : 1));
			break;
	}
}

// Updates the step attribute of the weight input based on the selected unit.
function updateWeightInputStep() {
	if (!modalIngredientWeightInput) return;

	let stepValue = 1; // Default to 1 gram
	let disableStepping = false;

	const unitConfig = availableMeasurementUnits.find(u => u.name === selectedMeasurementUnit);

	if (!unitConfig) {
		console.error(`Config not found for step calculation: ${selectedMeasurementUnit}`);
		disableStepping = true;
	} else {
		switch (unitConfig.name) {
			case 'GRAM':
				stepValue = 1;
				break;
			case 'PCS':
				if (currentPcsWeight && currentPcsWeight > 0) {
					stepValue = currentPcsWeight;
				} else {
					disableStepping = true; // Cannot step by pieces if weight is not set
				}
				break;
			default: // Handles KG, LITER, ML, TSP, TBSP, CUP, etc. based on config
				let standardGrams = unitConfig.standardGrams;
				if (standardGrams === null || standardGrams === undefined || standardGrams <= 0) {
					console.warn(`Invalid standard grams (${standardGrams}) for step calculation: ${unitConfig.name}`);
					disableStepping = true;
				} else {
					if (unitConfig.isVolume) {
						// Step for volume units depends on density
						if (!currentDensity || currentDensity <= 0) {
							console.warn("Cannot calculate volume step without valid density (> 0).");
							disableStepping = true;
						} else {
							// Calculate the gram/unit based on ml/unit and density g/ml.
							// e.g: g/unit = ml/unit * g/ml
							stepValue = standardGrams * currentDensity;
						}
					} else {
						// Step for weight units is just the standard grams
						stepValue = standardGrams;
					}
				}
				break;
		}
	}

	if (disableStepping) {
		modalIngredientWeightInput.step = '1'; // Fallback step
		console.warn(`Stepping potentially disabled or defaulted for unit: ${selectedMeasurementUnit}`);
	} else {
		// Use a reasonable minimum step, especially for small units
		modalIngredientWeightInput.step = Math.max(0.001, stepValue).toString();
	}
}

// Updates the PCS Set/Unset button's visibility and text based on the current unit and state.
function updatePcsButtonState() {
	if (!modalIngredientPcsButton || !modalIngredientWeightInput) return;

	if (selectedMeasurementUnit === 'PCS') {
		modalIngredientPcsButton.style.display = 'inline-block'; // Show button
		const weightInGrams = parseFloat(modalIngredientWeightInput.value) || 0;

		if (currentPcsWeight && currentPcsWeight > 0) {
			// If pcsWeight is set, check if current input matches it
			if (Math.abs(weightInGrams - currentPcsWeight) < 0.001) { // Use tolerance for float comparison
				modalIngredientPcsButton.textContent = 'Unset';
			} else {
				modalIngredientPcsButton.textContent = 'Set';
			}
		} else {
			// pcsWeight is not set
			modalIngredientPcsButton.textContent = 'Set';
		}
	} else {
		modalIngredientPcsButton.style.display = 'none'; // Hide button if unit is not PCS
	}
}

// Handles clicks on the PCS Set/Unset button.
async function handlePcsButtonClick() {
	if (!ingredientToModify || !ingredientToModify.child) return;

	const ingredientId = ingredientToModify.child.id;
	const mealId = selectedMeal.child.id;
	const currentAction = modalIngredientPcsButton.textContent;

	clearModalFeedbackAndStyles(); // Clear feedback before API call

	let targetPcsWeight = null;
	let url = `${API_BASE_URL}/meals/${mealId}/ingredients/${ingredientId}?`;

	if (currentAction === 'Set') {
		targetPcsWeight = parseFloat(modalIngredientWeightInput.value) || 0;
		if (targetPcsWeight <= 0) {
			setModalFeedback('PCS weight must be greater than 0 to set.', true);
			modalIngredientWeightInput.focus();
			return;
		}
		url += `pcsWeight=${targetPcsWeight}`;
	} else { // Action is 'Unset'
		targetPcsWeight = null;
		url += `pcsWeight=null`; // Send explicit null query parameter
	}

	console.log(`PCS Button Click (${currentAction}): Sending PUT ${url}`);

	try {
		// Send minimal PUT request containing only the pcsWeight parameter
		const response = await fetch(url, {
			method: 'PUT',
			headers: { 'Content-Type': 'application/json' }
			// No body is needed for query parameter updates
		});

		if (!response.ok) {
			const errorData = await response.json().catch(() => ({ message: `HTTP error! Status: ${response.status}` }));
			throw new Error(errorData.message || `Failed to ${currentAction.toLowerCase()} PCS weight.`);
		}

		// Success
		console.log(`PCS weight successfully ${currentAction.toLowerCase()}ed.`);
		setModalFeedback(`PCS weight ${currentAction.toLowerCase()} successfully.`, false);

		// Update local state immediately for UI consistency
		currentPcsWeight = targetPcsWeight;
		if (ingredientToModify && ingredientToModify.child) {
			ingredientToModify.child.pcsWeight = currentPcsWeight;
		}
		if (previousIngredientModalState) {
			previousIngredientModalState.pcsWeight = currentPcsWeight; // Update previous state as well
		}

		// Update UI elements that depend on pcsWeight
		updatePcsButtonState();
		updateConvertedValueLabel();
		updateWeightInputStep();

		// Consider uncommenting if the main adventure display needs immediate reflection of pcsWeight change
		// refreshCurrentAdventure();

	} catch (error) {
		// Error
		console.error(`Error ${currentAction.toLowerCase()}ing PCS weight:`, error);
		setModalFeedback(`Error: ${error.message}`, true);
	}
}

// Populates the unit selection dropdown menu based on fetched units.
function populateUnitDropdown() {
	if (!modalIngredientUnitDropdown) return;
	modalIngredientUnitDropdown.innerHTML = ''; // Clear previous options

	availableMeasurementUnits.forEach(unitConfig => {
		const item = document.createElement('a');
		item.href = '#';
		item.textContent = UNIT_ABBREVIATIONS[unitConfig.name] || unitConfig.name.toLowerCase();
		item.dataset.unit = unitConfig.name; // Store the enum name

		// Construct and set the title attribute for the dropdown item
		let title = unitConfig?.description || unitConfig.name;
		item.title = title;

		item.onclick = (e) => {
			e.preventDefault();
			selectUnit(unitConfig.name); // Select using the unit name
			modalIngredientUnitDropdown.style.display = 'none'; // Hide dropdown after selection
		};
		modalIngredientUnitDropdown.appendChild(item);
	});
}

// Handles selecting a unit from the dropdown.
function selectUnit(unitName) { // Parameter is the unit name (string)
	const unitConfig = availableMeasurementUnits.find(u => u.name === unitName);
	if (!unitConfig) {
		console.warn(`Invalid unit selected: ${unitName}`);
		return;
	}

	selectedMeasurementUnit = unitName; // Update the state
	console.log(`Unit selected: ${selectedMeasurementUnit}`);

	// Update button text and title attribute for tooltip
	if (modalIngredientUnitButton) {
		modalIngredientUnitButton.querySelector('.unit-text').textContent = 
		UNIT_ABBREVIATIONS[unitConfig.name] || unitConfig.name.toLowerCase();
		// Construct title string
		let title = unitConfig?.description || '';
		modalIngredientUnitButton.title = title;
	}

	// Update dependent UI elements
	updateConvertedValueLabel();
	updateWeightInputStep();
	updatePcsButtonState(); // PCS button visibility depends on the selected unit

	// Update previous state for comparison logic
	if (previousIngredientModalState) {
		previousIngredientModalState.measurementUnit = selectedMeasurementUnit;
	}
}

// Opens the modify ingredient modal and populates it with the ingredient's data.
function openModifyIngredientModal(ingredientData) {
	if (!ingredientData || !ingredientData.child) {
		console.error('Invalid ingredient data for modification modal');
		return;
	}
	ingredientToModify = { ...ingredientData }; // Store a copy for modification
	const ingredient = ingredientToModify.child;
	const nutrients = ingredient.nutrientsMap || {};

	clearModalFeedbackAndStyles();
	previousIngredientModalState = {}; // Reset previous state tracking

	// Basic Info
	if (modalIngredientModifyName) modalIngredientModifyName.textContent = ingredient.name || 'Unknown';
	if (modalIngredientModifyId) modalIngredientModifyId.value = ingredient.id;

	// Weight (always in grams)
	const currentWeight = ingredientData.recipeWeight || 0;
	if (modalIngredientWeightInput) modalIngredientWeightInput.value = currentWeight;
	previousIngredientModalState.weight = currentWeight;

	// Measurement Unit, PCS Weight, Density
	selectedMeasurementUnit = ingredient.measurementUnit || 'GRAM';
	currentPcsWeight = ingredient.pcsWeight !== undefined ? ingredient.pcsWeight : null;
	currentDensity = ingredient.density !== undefined && ingredient.density !== null ? ingredient.density : 1.0; // Default density to 1.0 (like water) if null/undefined
	if (modalIngredientDensityInput) modalIngredientDensityInput.value = currentDensity.toFixed(2); // Set density input
	previousIngredientModalState.measurementUnit = selectedMeasurementUnit;
	previousIngredientModalState.pcsWeight = currentPcsWeight;
	previousIngredientModalState.density = currentDensity; // Store density for potential volume calcs

	// Update Unit Button Text and Title
	if (modalIngredientUnitButton) {
		const unitConfig = availableMeasurementUnits.find(u => u.name === selectedMeasurementUnit);
		modalIngredientUnitButton.querySelector('.unit-text').textContent = 
		UNIT_ABBREVIATIONS[selectedMeasurementUnit] || selectedMeasurementUnit.toLowerCase();

		// Set initial title attribute
		let title = unitConfig?.description || '';
		modalIngredientUnitButton.title = title;
	}

	// Populate Dropdown with available units
	populateUnitDropdown();

	// Initial UI updates based on loaded unit/weight/pcsWeight/density
	updateConvertedValueLabel();
	updateWeightInputStep();
	updatePcsButtonState();

	// Nutrients (convert ratios to percentages for display)
	const nutrientValues = {
		protein: ((nutrients.protein || 0) * 100).toFixed(1),
		fat: ((nutrients.fat || 0) * 100).toFixed(1),
		carbs: ((nutrients.carbs || 0) * 100).toFixed(1),
		fiber: ((nutrients.fiber || 0) * 100).toFixed(1),
		salt: ((nutrients.salt || 0) * 100).toFixed(1)
	};

	if (modalIngredientProteinInput) modalIngredientProteinInput.value = nutrientValues.protein;
	if (modalIngredientFatInput) modalIngredientFatInput.value = nutrientValues.fat;
	if (modalIngredientCarbsInput) modalIngredientCarbsInput.value = nutrientValues.carbs;
	if (modalIngredientFiberInput) modalIngredientFiberInput.value = nutrientValues.fiber;
	if (modalIngredientSaltInput) modalIngredientSaltInput.value = nutrientValues.salt;

	calculateAndUpdateWaterPercentage(); // Calculate and display initial water %
	nutrientValues.water = modalIngredientWaterInput.value; // Read calculated water
	previousIngredientModalState.nutrients = nutrientValues; // Store initial nutrient percentages

	// Finalize
	lastValidNutrientsOnError = null; // Clear any previous error state
	if (modifyIngredientModal) modifyIngredientModal.style.display = 'block';
	if (modalIngredientWeightInput) modalIngredientWeightInput.focus(); // Focus weight input
}

// Closes the modify ingredient modal and resets related state.
function closeModifyIngredientModal() {
	if (modifyIngredientModal) modifyIngredientModal.style.display = 'none';
	// Hide dropdown menu if it was open
	if (modalIngredientUnitDropdown) modalIngredientUnitDropdown.style.display = 'none';
	ingredientToModify = null;
	previousIngredientModalState = {};
	clearModalFeedbackAndStyles();
	lastValidNutrientsOnError = null;
	// Reset unit-related state variables to defaults
	selectedMeasurementUnit = 'GRAM';
	currentPcsWeight = null;
	currentDensity = 1.0; // Reset density
}

// Close modal if user clicks outside of it
window.onclick = function (event) {
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
};

// Listeners to track mousedown target and reset on mouseup (for modal closing logic)
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

// Handles the main "Update" button click in the modify ingredient modal.
async function updateIngredient() {
	const ingredientId = modalIngredientModifyId.value;
	const newWeightValue = modalIngredientWeightInput.value; // Weight input is always in grams
	const newDensityValue = modalIngredientDensityInput.value; // Read density input

	// Read nutrient values (as percentages from inputs)
	const proteinPercent = parseFloat(modalIngredientProteinInput.value) || 0;
	const fatPercent = parseFloat(modalIngredientFatInput.value) || 0;
	const carbsPercent = parseFloat(modalIngredientCarbsInput.value) || 0;
	const fiberPercent = parseFloat(modalIngredientFiberInput.value) || 0;
	const saltPercent = parseFloat(modalIngredientSaltInput.value) || 0;

	calculateAndUpdateWaterPercentage(); // Ensure water is up-to-date before reading
	const waterPercent = parseFloat(modalIngredientWaterInput.value) || 0; // Read calculated value

	clearModalFeedbackAndStyles();

	// Basic Validation
	if (!ingredientId || !selectedMeal || !selectedMeal.child) {
		setModalFeedback('Error: Cannot identify ingredient or meal.', true);
		return;
	}
	if (newWeightValue === '' || newWeightValue === null || parseFloat(newWeightValue) < 0) {
		setModalFeedback('Please enter a valid weight (minimum 0).', true);
		modalIngredientWeightInput.focus();
		return;
	}
	const newWeight = parseFloat(newWeightValue); // Use the validated gram value
	if (newDensityValue === '' || newDensityValue === null || parseFloat(newDensityValue) <= 0) {
		setModalFeedback('Please enter a valid density (must be greater than 0).', true);
		modalIngredientDensityInput.focus();
		return;
	}
	const newDensity = parseFloat(newDensityValue); // Use the validated density value

	const nutrientsPercent = [proteinPercent, fatPercent, carbsPercent, waterPercent, fiberPercent, saltPercent];
	if (nutrientsPercent.some(n => n < 0 || n > 100)) {
		setModalFeedback('Nutrient percentages must be between 0 and 100.', true);
		return;
	}
	// Check only editable nutrients again (redundant but safe)
	const nutrientsToCheck = [proteinPercent, fatPercent, carbsPercent, fiberPercent, saltPercent];
	if (nutrientsToCheck.some(n => n < 0 || n > 100)) {
		setModalFeedback('Editable nutrient percentages must be between 0 and 100.', true);
		return;
	}

	// Prepare API Call: Convert percentages to ratios
	const proteinRatio = proteinPercent / 100;
	const fatRatio = fatPercent / 100;
	const carbsRatio = carbsPercent / 100;
	const waterRatio = waterPercent / 100;
	const fiberRatio = fiberPercent / 100;
	const saltRatio = saltPercent / 100;

	const mealId = selectedMeal.child.id;

	// This update includes weight, all nutrients, and the user's selected measurement unit preference.
	// pcsWeight is handled separately by its dedicated button/API call.
	const params = new URLSearchParams();
	params.append('weight', newWeight); // Always send weight in grams
	params.append('protein', proteinRatio);
	params.append('fat', fatRatio);
	params.append('carbs', carbsRatio);
	params.append('water', waterRatio);
	params.append('fiber', fiberRatio);
	params.append('salt', saltRatio);
	params.append('measurementUnit', selectedMeasurementUnit); // Send current unit preference
	params.append('density', newDensity); // Send current density value

	console.log(`Update Ingredient: Sending PUT /meals/${mealId}/ingredients/${ingredientId}?${params.toString()}`);

	try {
		const response = await fetch(`${API_BASE_URL}/meals/${mealId}/ingredients/${ingredientId}?${params.toString()}`, {
			method: 'PUT',
			headers: { 'Content-Type': 'application/json' }
		});

		const responseData = await response.json();

		if (!response.ok) {
			// Throw an error with backend message if available
			const error = new Error(responseData.message || `HTTP error! status: ${response.status}`);
			error.data = responseData; // Attach full error data if possible
			throw error;
		}

		// Success
		setModalFeedback('Ingredient updated successfully!', false);
		lastValidNutrientsOnError = null; // Clear any previous error state

		const updatedIngredient = responseData; // Backend returns the updated Ingredient object
		const updatedNutrients = updatedIngredient.nutrientsMap || {};
		// Weight: The backend Ingredient model doesn't include recipeWeight.
		// Use the 'newWeight' we just sent as the new "current" weight for UI feedback.
		const currentReturnedWeight = newWeight;

		if (modalIngredientWeightInput) modalIngredientWeightInput.value = currentReturnedWeight;

		// Update nutrient input fields from the response's nutrient map (converting back to %)
		const returnedNutrientsPercent = {
			protein: ((updatedNutrients.protein || 0) * 100).toFixed(1),
			fat: ((updatedNutrients.fat || 0) * 100).toFixed(1),
			carbs: ((updatedNutrients.carbs || 0) * 100).toFixed(1),
			water: ((updatedNutrients.water || 0) * 100).toFixed(1), // Update water too
			fiber: ((updatedNutrients.fiber || 0) * 100).toFixed(1),
			salt: ((updatedNutrients.salt || 0) * 100).toFixed(1)
		};
		updateModalInputsFromNutrients(returnedNutrientsPercent, true); // true = input map is percentages

		// Update unit-related fields based on returned data (though only measurementUnit might change here)
		selectedMeasurementUnit = updatedIngredient.measurementUnit || 'GRAM';
		currentPcsWeight = updatedIngredient.pcsWeight !== undefined ? updatedIngredient.pcsWeight : null; // Reflect backend state for pcsWeight too
		currentDensity = updatedIngredient.density !== undefined && updatedIngredient.density !== null ? updatedIngredient.density : 1.0; // Update density from response
		if (modalIngredientDensityInput) modalIngredientDensityInput.value = currentDensity.toFixed(2); // Update density input field

		if (modalIngredientUnitButton) {
			modalIngredientUnitButton.querySelector('.unit-text').textContent = selectedMeasurementUnit.toLowerCase();
		}
		// Re-calculate/update unit-dependent UI
		updateConvertedValueLabel();
		updateWeightInputStep();
		updatePcsButtonState();


		// Highlight input fields that changed compared to the state *before* this update
		highlightInputChanges(currentReturnedWeight, returnedNutrientsPercent);

		// Update the 'previous' state to reflect the successful update for the next comparison or reset
		previousIngredientModalState.weight = currentReturnedWeight;
		previousIngredientModalState.nutrients = returnedNutrientsPercent; // Store the returned percentages
		previousIngredientModalState.measurementUnit = selectedMeasurementUnit;
		previousIngredientModalState.pcsWeight = currentPcsWeight;
		previousIngredientModalState.density = currentDensity; // Store updated density

		// Update the local ingredientToModify object to match the backend response
		if (ingredientToModify && ingredientToModify.child) {
			// Merge the updated ingredient fields from the backend response
			ingredientToModify.child = { ...ingredientToModify.child, ...updatedIngredient };
			// Manually update recipeWeight in the wrapper object based on what we sent
			ingredientToModify.recipeWeight = newWeight;
		}


		await refreshCurrentAdventure(); // Refresh main display

	} catch (error) {
		// Error Handling
		let errorMessage = "An unexpected error occurred.";
		let currentNutrientsFromError = null; // Holds nutrient ratios from backend on error

		if (error.data && error.data.message) {
			errorMessage = error.data.message;
			// Backend sends current nutrient *ratios* in 'currentNutrients' field on validation error
			currentNutrientsFromError = error.data.currentNutrients;
		} else if (error.message) {
			errorMessage = error.message;
		}

		setModalFeedback('Error: ' + errorMessage, true);
		console.error('Update Ingredient Error:', errorMessage, error.data || error);

		lastValidNutrientsOnError = null; // Reset first

		// If backend provided current nutrient state on error, reset modal inputs to that state
		// and store it (as ratios) for the reset button functionality.
		if (currentNutrientsFromError) {
			console.log("Resetting modal inputs based on backend state after error:", currentNutrientsFromError);
			// Update modal inputs using the nutrient RATIO map from backend
			updateModalInputsFromNutrients(currentNutrientsFromError, false); // false = input map is ratios
			calculateAndUpdateWaterPercentage(); // Recalculate water display based on reset values

			// Store the error state (as RATIOS) for the reset button
			lastValidNutrientsOnError = { ...currentNutrientsFromError };

			// Update the 'previousIngredientModalState' nutrients as well to reflect the reset state
			// (This converts ratios back to percentages for consistency in previous state format)
			updatePreviousStateFromNutrients(currentNutrientsFromError);
		}
	}
}

// Helper to set modal feedback message and style (success or error).
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

// Helper function to compare current modal inputs to the previous state and apply highlighting.
function highlightInputChanges(newWeight, newNutrientsPercent) {
	const tolerance = 0.01; // Tolerance for float comparison

	// Weight
	if (modalIngredientWeightInput) {
		const oldWeight = previousIngredientModalState.weight || 0;
		modalIngredientWeightInput.classList.remove('input-increased', 'input-decreased'); // Reset
		if (Math.abs(newWeight - oldWeight) > tolerance) {
			modalIngredientWeightInput.classList.add(newWeight > oldWeight ? 'input-increased' : 'input-decreased');
		}
	}

	// Nutrients
	const nutrientInputsMap = {
		protein: modalIngredientProteinInput,
		fat: modalIngredientFatInput,
		carbs: modalIngredientCarbsInput,
		water: modalIngredientWaterInput, // Include read-only water input for highlighting
		fiber: modalIngredientFiberInput,
		salt: modalIngredientSaltInput
	};
	const oldNutrients = previousIngredientModalState.nutrients || {};
	for (const key in newNutrientsPercent) {
		const inputElement = nutrientInputsMap[key];
		// Check if previous state and the specific nutrient value exist before comparing
		if (inputElement && oldNutrients[key] !== undefined) {
			const oldValue = parseFloat(oldNutrients[key]) || 0;
			const newValue = parseFloat(newNutrientsPercent[key]) || 0;
			inputElement.classList.remove('input-increased', 'input-decreased'); // Reset first
			if (Math.abs(newValue - oldValue) > tolerance) {
				inputElement.classList.add(newValue > oldValue ? 'input-increased' : 'input-decreased');
			}
		} else if (inputElement) {
			// If previous state didn't have the key, just remove classes
			inputElement.classList.remove('input-increased', 'input-decreased');
		}
	}

	// Note: Highlighting unit/pcs/density changes could be added here if needed.
	// Example: Compare selectedMeasurementUnit with previousIngredientModalState.measurementUnit
	// Example: Compare currentPcsWeight with previousIngredientModalState.pcsWeight
	// Example: Compare currentDensity with previousIngredientModalState.density

	// Density Highlighting
	if (modalIngredientDensityInput && previousIngredientModalState.density !== undefined) {
		const oldDensity = previousIngredientModalState.density;
		const newDensity = currentDensity; // Use the updated currentDensity
		modalIngredientDensityInput.classList.remove('input-increased', 'input-decreased'); // Reset
		if (Math.abs(newDensity - oldDensity) > tolerance) {
			modalIngredientDensityInput.classList.add(newDensity > oldDensity ? 'input-increased' : 'input-decreased');
		}
	} else if (modalIngredientDensityInput) {
		modalIngredientDensityInput.classList.remove('input-increased', 'input-decreased');
	}
}

// Updates the main UI display based on the current adventure, selected meal, and selected ingredient.
function updateAdventureDisplay() {
	const adventureInfoDiv = document.getElementById('adventureInfo');
	const selectedMealSection = document.getElementById('selectedMealDetails').closest('.info-group'); // Get parent info-group
	const selectedIngredientSection = document.getElementById('selectedIngredientDetails').closest('.info-group'); // Get parent info-group
	const selectedMealDetailsDiv = document.getElementById('selectedMealDetails');
	const selectedIngredientDetailsDiv = document.getElementById('selectedIngredientDetails');

	if (!currentAdventure) {
		if (adventureTitle) adventureTitle.textContent = 'Create or select an adventure';
		if (adventureTitle) adventureTitle.style.display = 'block';
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
	if (setDaysButton) setDaysButton.style.display = 'inline-block'; // Show Set Days button
	if (adventureInfoDiv) adventureInfoDiv.style.display = 'block'; // Show the main info block
	if (adventureTitle) adventureTitle.style.display = 'none';
	// Update Adventure Basic Info
	document.getElementById('adventureNameDisplay').textContent = currentAdventure.name || '-';
	document.getElementById('adventureDuration').textContent = `${currentAdventure.days || 1} days`;
	document.getElementById('adventureCrewSize').textContent = `${currentAdventure.crewSize || 0} ${(currentAdventure.crewSize === 1) ? 'person' : 'persons'}`;
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
	const meals = getChildrenFromMap(currentAdventure.childMap);
	if (meals && meals.length > 0) {
		meals.forEach(mealData => {
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
                    <p>Calc. Weight: ${(currentAdventure.mealWeights && currentAdventure.mealWeights[meal.id] !== undefined ? currentAdventure.mealWeights[meal.id].toFixed(3) : '0.000')} kg</p>
                    <p>Energy Density: ${meal.formattedEnergyDensity || '0.0'} kCal / kg</p>
                    <p>Ratio: ${Math.round(mealData.ratio * 100) || '0.0'} %</p>
                    <p>${(getChildrenFromMap(meal.childMap) ? getChildrenFromMap(meal.childMap).length : 0)} ingredients</p>
                `;
				// Pass the full meal object from currentAdventure to preserve nutrientMap
				const fullMealData = meals.find(m => m.child && m.child.id === meal.id);
				card.onclick = () => selectMeal(fullMealData);
				mealsList.appendChild(card);
			}
		});
	} else {
		mealsList.innerHTML = '<p>No meals added yet.</p>';
	}

	// --- Update Selected Meal Section (Ingredients List) ---
	if (selectedMeal && selectedMeal.child && selectedMeal.child.id) {
		// Find the *latest* version of the selected meal from the refreshed adventure data
		const currentMealData = getChildrenFromMap(currentAdventure.childMap).find(m => m.child && m.child.id === selectedMeal.child.id);

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
		// Use the weight from the adventure's mealWeights map
		const calculatedMealWeight = (currentAdventure.mealWeights && currentAdventure.mealWeights[meal.id] !== undefined)
			? currentAdventure.mealWeights[meal.id]
			: 0;
		document.getElementById('selectedMealWeight').textContent = `${calculatedMealWeight.toFixed(3)} kg`;
		document.getElementById('selectedMealEnergyDensity').textContent = `${meal.formattedEnergyDensity || '0.0'} kCal / kg`;
		document.getElementById('selectedMealRatio').textContent = `${(mealRatio * 100).toFixed(1)} %`;

		// Render Meal Nutrients
		renderNutrients(meal.nutrientsMap, 'selectedMealNutrients');

		// Update Ingredients List (Now uses cards)
		const ingredientsListDiv = document.getElementById('selectedMealIngredients');
		ingredientsListDiv.innerHTML = '';
		const ingredients = getChildrenFromMap(meal.childMap);
		if (ingredients && ingredients.length > 0) {
			ingredients.forEach(ingredientData => {
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
                        <p>Density: ${ingredient.density.toFixed(2)} g/ml</p>
                        <p>Recipe Weight: ${ingredientData.recipeWeight || 0} g</p>
                        <p>Recipe W. Ratio: ${(ingredientData.ratio * 100).toFixed(1)} %</p>
                        <p>Calc. value: ${(currentAdventure.ingredientWeights && currentAdventure.ingredientWeights[ingredient.id] !== undefined ? currentAdventure.ingredientWeights[ingredient.id].toFixed(3) : '0.000')} kg</p>
                    `;
					card.querySelector('.modify-button').dataset.ingredientData = JSON.stringify(ingredientData);
					card.onclick = (e) => {
						if (e.target.tagName !== 'BUTTON' && !e.target.closest('button')) { // Check parent buttons too
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
		const currentIngredientData = getChildrenFromMap(selectedMeal?.child?.childMap).find(i => i.child && i.child.id === selectedIngredient.child.id);

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

// Initialization function run when the DOM is loaded.
async function initializeDemo() {
	// Fetch units configuration FIRST, as other parts depend on it.
	await fetchMeasurementUnits();

	try {
		// Fetch existing adventures to populate the dropdown
		const response = await fetch(`${API_BASE_URL}/adventures`);
		if (response.ok) {
			adventures = await response.json();
			updateAdventureDropdown();
			// Consider selecting the first adventure automatically on load:
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

	// Add Enter key listeners for modal inputs to trigger submission
	if (modalMealNameInput) {
		modalMealNameInput.addEventListener('keypress', function (event) {
			if (event.key === 'Enter') {
				event.preventDefault(); // Prevent default form submission behavior
				addMeal();
			}
		});
	}
	if (modalIngredientNameInput) {
		modalIngredientNameInput.addEventListener('keypress', function (event) {
			if (event.key === 'Enter') {
				event.preventDefault();
				addIngredient();
			}
		});
	}
	if (modalDaysInput) { // Listener for set days input
		modalDaysInput.addEventListener('keypress', function (event) {
			if (event.key === 'Enter') {
				event.preventDefault();
				setDays();
			}
		});
	}
	// Listener for modify ingredient weight (Enter submits the main update)
	if (modalIngredientWeightInput) {
		modalIngredientWeightInput.addEventListener('keypress', function (event) {
			if (event.key === 'Enter') {
				event.preventDefault();
				updateIngredient();
			}
		});
	}

	// Add Escape key listener to close any open modal
	document.addEventListener('keydown', function (event) {
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
				// If the unit dropdown is open, Escape closes it first. Otherwise, close the modal.
				if (modalIngredientUnitDropdown && modalIngredientUnitDropdown.style.display === 'block') {
					modalIngredientUnitDropdown.style.display = 'none';
				} else {
					closeModifyIngredientModal();
				}
			}
		}
	});

	// Ensure the adventure info section is hidden initially if no adventure is selected
	updateAdventureDisplay();

	// Add Enter/Input listeners for modify modal nutrient inputs
	const nutrientInputs = [
		modalIngredientProteinInput,
		modalIngredientFatInput,
		modalIngredientCarbsInput,
		// modalIngredientWaterInput, // No listener needed for disabled input as it's calculated
		modalIngredientFiberInput,
		modalIngredientSaltInput
	];

	nutrientInputs.forEach(input => {
		if (input) {
			// Recalculate water percentage whenever a nutrient value changes
			input.addEventListener('input', calculateAndUpdateWaterPercentage);

			// Trigger the main update function on Enter key press in any nutrient field
			input.addEventListener('keypress', function (event) {
				if (event.key === 'Enter') {
					event.preventDefault();
					updateIngredient();
				}
			});
		}
	});

	// Add INPUT listener for the weight input in the modify modal
	if (modalIngredientWeightInput) {
		modalIngredientWeightInput.addEventListener('input', () => {
			// Update the converted value label and PCS button state dynamically as weight changes
			updateConvertedValueLabel();
			updatePcsButtonState();
		});
		// Enter key in weight also triggers main update
		modalIngredientWeightInput.addEventListener('keypress', function (event) {
			if (event.key === 'Enter') {
				event.preventDefault();
				updateIngredient();
			}
		});
	}

	// Add click listener for Unit Dropdown Button (toggles the dropdown menu)
	if (modalIngredientUnitButton) {
		modalIngredientUnitButton.addEventListener('click', (e) => {
			e.stopPropagation(); // Prevent the window click listener from closing it immediately
			if (modalIngredientUnitDropdown) {
				const isVisible = modalIngredientUnitDropdown.style.display === 'block';
				modalIngredientUnitDropdown.style.display = isVisible ? 'none' : 'block';
			}
		});
	}

	// Add click listener for the PCS Set/Unset Button
	if (modalIngredientPcsButton) {
		modalIngredientPcsButton.addEventListener('click', handlePcsButtonClick);
	}

	// Add global click listener to close the unit dropdown if clicked outside
	document.addEventListener('click', (event) => {
		if (modalIngredientUnitDropdown && modalIngredientUnitDropdown.style.display === 'block') {
			// Check if the click was outside the dropdown container and its button
			if (!modalIngredientUnitButton.contains(event.target) && !modalIngredientUnitDropdown.contains(event.target)) {
				modalIngredientUnitDropdown.style.display = 'none';
			}
		}
	});

	// Add Enter key listeners for Crew Member modal inputs
	const crewModalInputs = [
		document.getElementById('modalCrewName'),
		document.getElementById('modalCrewAge'),
		document.getElementById('modalCrewHeight'),
		document.getElementById('modalCrewWeight'),
		// Select elements don't typically trigger keypress for submit
		// document.getElementById('modalCrewGender'),
		// document.getElementById('modalCrewActivity'),
		// document.getElementById('modalCrewStrategy')
	];

	crewModalInputs.forEach(input => {
		if (input) {
			input.addEventListener('keypress', function (event) {
				if (event.key === 'Enter') {
					event.preventDefault();
					addCrewMember(); // Trigger add crew member action
				}
			});
		}
	});

	// Add INPUT listener for the density input in the modify modal
	if (modalIngredientDensityInput) {
		modalIngredientDensityInput.addEventListener('input', () => {
			// Update currentDensity state and any dependent UI when density input changes
			// Use parseFloat defensively, default to 1.0 if invalid
			const newDensityValue = parseFloat(modalIngredientDensityInput.value);
			currentDensity = !isNaN(newDensityValue) && newDensityValue > 0 ? newDensityValue : 1.0;
			updateConvertedValueLabel();
			updateWeightInputStep(); // Step might change for volume units
		});
		// REMOVED: Enter key in density no longer triggers main update
		/*
		modalIngredientDensityInput.addEventListener('keypress', function(event) {
			if (event.key === 'Enter') {
			   event.preventDefault();
			   updateIngredient();
		   }
	   });
	   */
	}

	// Add click listener for the Density Update Button
	if (modalIngredientDensityUpdateButton) {
		modalIngredientDensityUpdateButton.addEventListener('click', updateIngredientDensity);
	}

	// Add global click listener to close the unit dropdown if clicked outside
	document.addEventListener('click', (event) => {
		if (modalIngredientUnitDropdown && modalIngredientUnitDropdown.style.display === 'block') {
			// Check if the click was outside the dropdown container and its button
			if (!modalIngredientUnitButton.contains(event.target) && !modalIngredientUnitDropdown.contains(event.target)) {
				modalIngredientUnitDropdown.style.display = 'none';
			}
		}
	});
}

// **** NEW FUNCTION: Handles partial update for density only ****
async function updateIngredientDensity() {
	const ingredientId = modalIngredientModifyId.value;
	const newDensityValue = modalIngredientDensityInput.value;

	clearModalFeedbackAndStyles(); // Clear feedback first

	// Basic Validation
	if (!ingredientId || !selectedMeal || !selectedMeal.child) {
		setModalFeedback('Error: Cannot identify ingredient or meal.', true);
		return;
	}
	if (newDensityValue === '' || newDensityValue === null || parseFloat(newDensityValue) <= 0) {
		setModalFeedback('Please enter a valid density (must be greater than 0).', true);
		modalIngredientDensityInput.focus();
		return;
	}
	const newDensity = parseFloat(newDensityValue);

	const mealId = selectedMeal.child.id;
	const params = new URLSearchParams();
	params.append('density', newDensity);

	const ingredientName = ingredientToModify?.child?.name || 'this ingredient';

	console.log(`Update Density Only: Sending PUT /meals/${mealId}/ingredients/${ingredientId}?${params.toString()}`);

	try {
		const response = await fetch(`${API_BASE_URL}/meals/${mealId}/ingredients/${ingredientId}?${params.toString()}`, {
			method: 'PUT',
			headers: { 'Content-Type': 'application/json' }
		});

		const responseData = await response.json();

		if (!response.ok) {
			const error = new Error(responseData.message || `HTTP error! status: ${response.status}`);
			error.data = responseData;
			throw error;
		}

		// Success
		setModalFeedback(`Density updated successfully for ${ingredientName}.`, false);

		const updatedIngredient = responseData;
		// Update local state from response
		currentDensity = updatedIngredient.density !== undefined && updatedIngredient.density !== null ? updatedIngredient.density : 1.0;
		if (modalIngredientDensityInput) modalIngredientDensityInput.value = currentDensity.toFixed(2); // Update input field

		// Update previous state for comparison logic
		if (previousIngredientModalState) {
			previousIngredientModalState.density = currentDensity;
		}

		// Update UI elements dependent on density
		updateConvertedValueLabel();
		updateWeightInputStep();

		// Highlight the change
		if (modalIngredientDensityInput) {
			modalIngredientDensityInput.classList.remove('input-increased', 'input-decreased'); // Reset
			// Check if previous state exists before highlighting (might not on first load/error)
			if (previousIngredientModalState && previousIngredientModalState.density !== undefined) {
				const oldDensity = previousIngredientModalState.density;
				if (Math.abs(currentDensity - oldDensity) > 0.01) { // Use tolerance
					modalIngredientDensityInput.classList.add(currentDensity > oldDensity ? 'input-increased' : 'input-decreased');
				}
			}
		}

		// Update the ingredient object in the local state if needed (less critical for density-only update)
		if (ingredientToModify && ingredientToModify.child) {
			ingredientToModify.child.density = currentDensity;
		}

		// Optionally refresh the main adventure display if density changes affect it significantly
		// await refreshCurrentAdventure(); 

	} catch (error) {
		let errorMessage = error.data?.message || error.message || "An unexpected error occurred.";
		setModalFeedback('Error: ' + errorMessage, true);
		console.error('Update Density Error:', errorMessage, error.data || error);
		// Optionally revert input field on error?
		// if (modalIngredientDensityInput && previousIngredientModalState?.density) {
		//    modalIngredientDensityInput.value = previousIngredientModalState.density.toFixed(2);
		// }
	}
}

// Start the demo initialization when the page loads
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
	const mealData = getChildrenFromMap(currentAdventure.childMap).find(m => m.child && m.child.id === mealId);
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
	const ingredientData = getChildrenFromMap(selectedMeal?.child?.childMap).find(i => i.child && i.child.id === ingredientId);
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

// Resets modify modal fields to the last known valid state (either from modal open or after a backend error).
// Also resets unit/pcsWeight/density fields.
function resetModalFieldsToLastValidState() {
	let targetState = null;
	let isPercentageMap = false; // Flag to know nutrient map format (ratio vs percentage)

	// Priority 1: Use state saved after the last backend error (nutrients as RATIOS).
	if (lastValidNutrientsOnError) {
		console.log("Resetting modal inputs to state from last error:", lastValidNutrientsOnError);
		targetState = { nutrients: lastValidNutrientsOnError }; // Nutrient map is ratios
		isPercentageMap = false;
		// Restore non-nutrient fields from the general 'previous state' as error state doesn't have them
		if (previousIngredientModalState.weight !== undefined) targetState.weight = previousIngredientModalState.weight;
		if (previousIngredientModalState.measurementUnit) targetState.measurementUnit = previousIngredientModalState.measurementUnit;
		if (previousIngredientModalState.pcsWeight !== undefined) targetState.pcsWeight = previousIngredientModalState.pcsWeight;
		if (previousIngredientModalState.density !== undefined) targetState.density = previousIngredientModalState.density;

	}
	// Priority 2: Fallback to state captured when the modal was opened (nutrients as PERCENTAGES).
	else if (previousIngredientModalState && Object.keys(previousIngredientModalState).length > 0) {
		console.log("Resetting modal inputs to state from modal open:", previousIngredientModalState);
		targetState = { ...previousIngredientModalState }; // Use the full previous state copy
		isPercentageMap = true; // Previous state stores nutrients as percentages
	}
	else {
		console.warn("Reset button clicked but no valid state available to restore.");
		return; // Nothing to restore
	}

	// Restore Weight
	if (modalIngredientWeightInput && targetState.weight !== undefined) {
		modalIngredientWeightInput.value = targetState.weight;
	}

	// Restore Nutrients (using appropriate format)
	if (targetState.nutrients) {
		updateModalInputsFromNutrients(targetState.nutrients, isPercentageMap);
	}
	calculateAndUpdateWaterPercentage(); // Always recalculate water display after setting nutrients

	// Restore Unit, PCS Weight, Density state variables
	if (targetState.density !== undefined) {
		currentDensity = targetState.density;
	}
	if (targetState.measurementUnit) {
		// Use selectUnit to update state AND UI button text
		selectUnit(targetState.measurementUnit);
	}
	currentPcsWeight = targetState.pcsWeight !== undefined ? targetState.pcsWeight : null;

	// Update UI based on restored state
	updateConvertedValueLabel();
	updateWeightInputStep();
	updatePcsButtonState();

	// Clear feedback and highlighting
	if (modalIngredientFeedback) {
		modalIngredientFeedback.textContent = '';
		modalIngredientFeedback.className = 'modal-feedback';
	}
	clearInputHighlighting();
	lastValidNutrientsOnError = null; // Clear the error state after a successful reset

	// Restore Density Input Field
	if (modalIngredientDensityInput && targetState.density !== undefined) {
		modalIngredientDensityInput.value = targetState.density.toFixed(2);
	}
}


// Updates the `previousIngredientModalState.nutrients` object after a backend error.
// It converts the ratio map received from the backend into a percentage map
// for consistency with the format used when the modal is opened.
// Note: Only updates nutrients; other previous state fields (weight, unit, pcsWeight) are retained.
function updatePreviousStateFromNutrients(nutrientRatioMap) {
	// Only update nutrients, keep existing weight/unit/pcsWeight etc. in previous state
	if (!previousIngredientModalState.nutrients) previousIngredientModalState.nutrients = {};
	const nutrientKeys = ["protein", "fat", "carbs", "water", "fiber", "salt"];
	for (const key of nutrientKeys) {
		if (nutrientRatioMap[key] !== undefined) {
			// Convert ratio back to percentage string for storage consistency
			previousIngredientModalState.nutrients[key] = (nutrientRatioMap[key] * 100).toFixed(1);
		}
	}
	console.log("Updated previous state nutrients after error reset:", previousIngredientModalState);
}


// Helper function to update modal nutrient input fields from a nutrient map.
// Handles both ratio maps (isPercentageMap=false) and percentage maps (isPercentageMap=true).
function updateModalInputsFromNutrients(nutrientMap, isPercentageMap = false) {
	const nutrientInputsMap = {
		protein: modalIngredientProteinInput,
		fat: modalIngredientFatInput,
		carbs: modalIngredientCarbsInput,
		water: modalIngredientWaterInput, // Update the disabled water field too
		fiber: modalIngredientFiberInput,
		salt: modalIngredientSaltInput
	};

	for (const key in nutrientInputsMap) {
		const inputElement = nutrientInputsMap[key];
		if (inputElement && nutrientMap[key] !== undefined) {
			// If input map is already percentages (e.g., from previous state), use directly.
			// Otherwise (e.g., from backend error state), convert ratio to percentage string.
			inputElement.value = isPercentageMap ? nutrientMap[key] : (nutrientMap[key] * 100).toFixed(1);
		}
	}
	// Ensure input highlighting is cleared after setting values
	clearInputHighlighting();
}

// Helper function to clear input highlighting styles only.
function clearInputHighlighting() {
	const inputsToClear = [
		modalIngredientWeightInput,
		modalIngredientProteinInput,
		modalIngredientFatInput,
		modalIngredientCarbsInput,
		modalIngredientWaterInput,
		modalIngredientFiberInput,
		modalIngredientSaltInput,
		modalIngredientDensityInput // Add density input
	];
	inputsToClear.forEach(input => {
		if (input) {
			input.classList.remove('input-increased', 'input-decreased');
		}
	});
}

// --- Add Ingredient Modal - Search and Load Logic ---

async function searchExistingIngredients() {
	const query = modalIngredientSearchInput.value.trim();
	if (!query) {
		if (ingredientSearchResultsDiv) {
			ingredientSearchResultsDiv.innerHTML = '<p class="placeholder-text">Please enter a search term.</p>';
		}
		loadSelectedIngredientButton.disabled = true;
		selectedExistingIngredientId = null;
		return;
	}

	// Indicate loading state
	if (ingredientSearchResultsDiv) {
		ingredientSearchResultsDiv.innerHTML = '<p class="placeholder-text">Searching...</p>';
	}
	loadSelectedIngredientButton.disabled = true;
	selectedExistingIngredientId = null;

	try {
		const response = await fetch(`${API_BASE_URL}/ingredients/search?query=${encodeURIComponent(query)}`);
		if (!response.ok) {
			throw new Error(`Search failed (status: ${response.status})`);
		}
		const results = await response.json();
		displaySearchResults(results);

	} catch (error) {
		console.error('Error searching ingredients:', error);
		if (ingredientSearchResultsDiv) {
			ingredientSearchResultsDiv.innerHTML = `<p class="placeholder-text error-text">Error: ${error.message}</p>`;
		}
		loadSelectedIngredientButton.disabled = true;
		selectedExistingIngredientId = null;
	}
}

function displaySearchResults(results) {
	if (!ingredientSearchResultsDiv) return;
	ingredientSearchResultsDiv.innerHTML = ''; // Clear previous results or placeholder

	if (!results || results.length === 0) {
		ingredientSearchResultsDiv.innerHTML = '<p class="placeholder-text">No ingredients found matching your search.</p>';
		loadSelectedIngredientButton.disabled = true;
		selectedExistingIngredientId = null;
		return;
	}

	results.forEach(ingredient => {
		const card = document.createElement('div');
		// Reuse ingredient-card style for consistency, add specific class for modal context
		card.className = 'ingredient-card modal-search-result-item' + (ingredient.created_by_user_id !== null ? ' user-created' : ' system-default');
		card.dataset.ingredientId = ingredient.id;
		card.innerHTML = `
            <div class="card-header-simple">
                <h4>${ingredient.name || 'Unnamed Ingredient'}</h4>
                ${ingredient.created_by_user_id !== null ? '<p>User Created</p>' : ''}
            </div>
        `; // Keep it simple for search results
		card.onclick = () => selectExistingIngredient(ingredient.id, card);
		ingredientSearchResultsDiv.appendChild(card);
	});
}

function selectExistingIngredient(ingredientId, selectedCardElement) {
	// Remove 'selected' class from all other items
	ingredientSearchResultsDiv.querySelectorAll('.modal-search-result-item').forEach(item => {
		item.classList.remove('selected');
	});

	// If the clicked item was already selected, deselect it
	if (selectedExistingIngredientId === ingredientId) {
		selectedExistingIngredientId = null;
		loadSelectedIngredientButton.disabled = true;
		// Optionally re-enable the 'Add New' button if it was disabled
		// if (addNewIngredientButton) addNewIngredientButton.disabled = false;
		if (modalExistingIngredientFeedback) modalExistingIngredientFeedback.textContent = '';
	} else {
		// Select the new item
		selectedCardElement.classList.add('selected');
		selectedExistingIngredientId = ingredientId;
		loadSelectedIngredientButton.disabled = false;
		// Optionally disable the 'Add New' button/input to prevent adding both
		// if (addNewIngredientButton) addNewIngredientButton.disabled = true;
		if (modalExistingIngredientFeedback) modalExistingIngredientFeedback.textContent = ''; // Clear feedback on new selection
	}
}

async function loadSelectedIngredient() {
	if (!selectedMeal || !selectedMeal.child) {
		setExistingIngredientFeedback('Error: No meal selected.', true);
		return;
	}
	if (!selectedExistingIngredientId) {
		setExistingIngredientFeedback('Error: No ingredient selected from search results.', true);
		return;
	}

	const mealId = selectedMeal.child.id;

	// Indicate loading/processing
	setExistingIngredientFeedback('Adding selected ingredient...', false);
	loadSelectedIngredientButton.disabled = true; // Prevent double-click

	let response;

	try {

		// Call the LOAD endpoint with the ingredient ID from the search results, loading the ingredient data into memory
		const ingredientResponse = await fetch(`${API_BASE_URL}/ingredients/${selectedExistingIngredientId}`, {
			method: 'GET',
			headers: {
				'Content-Type': 'application/json',
			}
		});

		const ingredientJson = await ingredientResponse.json();

		const ingredientId = ingredientJson.id;

		if (ingredientId === selectedExistingIngredientId) {
			// Call the POST endpoint with the ingredient ID from the LOAD endpoint (should be the same) as a query parameter
			response = await fetch(`${API_BASE_URL}/meals/${mealId}/ingredients?ingredientId=${ingredientId}`, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
				}
			});
		} else {
			console.error('Ingredient ID mismatch: ', ingredientId, selectedExistingIngredientId);
			// Ingredient ID mismatch between database and memory.
			throw new Error('Ingredient ID mismatch. Contact support.');
		}

		if (!response.ok) {
			// Try to parse error message from backend if available
			let errorMsg = `Failed to add ingredient (status: ${response.status})`;
			try {
				const errorData = await response.json();
				if (errorData && errorData.message) {
					errorMsg = errorData.message;
				}
			} catch (parseError) { /* Ignore if response is not JSON */ }
			throw new Error(errorMsg);
		}

		// Success
		setExistingIngredientFeedback('Ingredient added successfully!', false);
		closeAddIngredientModal();
		await refreshCurrentAdventure();

	} catch (error) {
		console.error('Error loading/adding ingredient:', error);
		setExistingIngredientFeedback(`Error: ${error.message}`, true);
		// Re-enable button on error unless it was a selection issue
		if (selectedExistingIngredientId) {
			loadSelectedIngredientButton.disabled = false;
		}
	}
}

// Helper to set feedback in the 'Load Existing' section
function setExistingIngredientFeedback(message, isError) {
	if (modalExistingIngredientFeedback) {
		modalExistingIngredientFeedback.textContent = message;
		modalExistingIngredientFeedback.className = 'modal-feedback small-feedback'; // Reset
		if (isError) {
			modalExistingIngredientFeedback.classList.add('error');
		} else {
			modalExistingIngredientFeedback.classList.add('success'); // Use success class for loading/success msg
		}
	}
}

// Add Enter key listener for the search input
if (modalIngredientSearchInput) {
	modalIngredientSearchInput.addEventListener('keypress', function (event) {
		if (event.key === 'Enter') {
			event.preventDefault();
			searchExistingIngredients();
		}
	});
}

// --- End Add Ingredient Modal Search Logic --- 

// Helper function to convert childMap to an array of children
function getChildrenFromMap(childMap) {
	if (typeof childMap !== 'object') {
		console.error('getChildrenFromMap() received non-object argument:', childMap);
		return [];
	}
	// Object.values(childMap) returns an array of the wrapper objects.
	// We filter to ensure each wrapper is valid and has a .child property.
	return Object.values(childMap).filter(entry => entry && entry.child);
}