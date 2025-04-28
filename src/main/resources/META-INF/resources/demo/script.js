// API endpoints
const API_BASE_URL = 'http://localhost:8080';

// State variables
let adventures = [];  // Store all adventures
let currentAdventure = null;
let selectedMeal = null;
let selectedIngredient = null; // Add state for selected ingredient
let lastAdventureData = null;  // Store last fetched adventure data for comparison

// DOM elements
const adventuresList = document.getElementById('adventures-list');
const crewMembersList = document.getElementById('crew-members-list');
const baseClassesList = document.getElementById('base-classes-list');

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
    
    try {
        const response = await fetch(`${API_BASE_URL}/adventures/${currentAdventure.id}`);
        if (!response.ok) {
            throw new Error('Failed to fetch adventure details');
        }
        
		// Only update if response has changed.
        const newData = await response.json();
        if (hasDataChanged(newData, lastAdventureData)) {
            lastAdventureData = newData;
            currentAdventure = newData;
            updateAdventureDisplay();
			// Print for debug purposes.
			console.log(currentAdventure);
        }
    } catch (error) {
        console.error('Error refreshing adventure:', error);
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
        currentAdventure = data;
        document.getElementById('adventureSelect').value = data.id;
        await refreshCurrentAdventure();
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
        updateAdventureDropdown();
        updateAdventureDisplay();
    }
}

// Adventure Removal
async function removeSelectedAdventure() {
    if (!currentAdventure) {
        alert('Please select an adventure first');
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/adventures/${currentAdventure.id}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error('Failed to remove adventure');
        }

        adventures = adventures.filter(a => a.id !== currentAdventure.id);
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
    select.innerHTML = '<option value="">Select an adventure...</option>';
    
    adventures.forEach(adventure => {
        const option = document.createElement('option');
        option.value = adventure.id;
        option.textContent = adventure.name;
        if (currentAdventure && adventure.id === currentAdventure.id) {
            option.selected = true;
        }
        select.appendChild(option);
    });
}

// Crew Member Management
async function addCrewMember() {
    if (!currentAdventure) {
        alert('Please create an adventure first');
        return;
    }

    const params = new URLSearchParams({
        name: document.getElementById('crewName').value,
        age: document.getElementById('crewAge').value,
        height: document.getElementById('crewHeight').value,
        weight: document.getElementById('crewWeight').value,
        gender: document.getElementById('crewGender').value,
        activity: document.getElementById('crewActivity').value,
        strategy: document.getElementById('crewStrategy').value
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

        await refreshCurrentAdventure();
    } catch (error) {
        alert('Error adding crew member: ' + error.message);
    }
}

// Days Management
async function setDays() {
    if (!currentAdventure) {
        alert('Please create an adventure first');
        return;
    }

    const days = document.getElementById('days').value;
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

        await refreshCurrentAdventure();
    } catch (error) {
        alert('Error setting days: ' + error.message);
    }
}

// Meal Management
async function addMeal() {
    if (!currentAdventure) {
        alert('Please create an adventure first');
        return;
    }

    const name = document.getElementById('mealName').value;
    if (!name) {
        alert('Please enter a meal name');
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

        await refreshCurrentAdventure();
    } catch (error) {
        alert('Error adding meal: ' + error.message);
    }
}

// Ingredient Management
async function addIngredient() {
    if (!currentAdventure || !selectedMeal) {
        alert('Please select a meal first');
        return;
    }

    const name = document.getElementById('ingredientName').value;
    if (!name) {
        alert('Please enter an ingredient name');
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

        await refreshCurrentAdventure();
    } catch (error) {
        alert('Error adding ingredient: ' + error.message);
    }
}

async function modifyIngredientWeight() {
    if (!currentAdventure || !selectedMeal || !selectedMeal.child) {
        alert('Please select a meal first');
        return;
    }
    if (!selectedIngredient || !selectedIngredient.child) { // Check if an ingredient is selected
        alert('Please select an ingredient first');
        return;
    }

    const weight = document.getElementById('ingredientWeight').value;
    const mealId = selectedMeal.child.id; // Use correct meal ID
    const ingredientId = selectedIngredient.child.id; // Use correct ingredient ID from state

    if (!ingredientId) {
        alert('Could not determine the selected ingredient ID.');
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/meals/${mealId}/ingredients/${ingredientId}?weight=${weight}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        if (!response.ok) {
            throw new Error('Failed to modify ingredient weight');
        }

        await refreshCurrentAdventure();
    } catch (error) {
        alert('Error modifying ingredient weight: ' + error.message);
    }
}

// UI Updates
function updateAdventureDisplay() {
    const adventureInfoDiv = document.getElementById('adventureInfo');
    const selectedMealDetailsDiv = document.getElementById('selectedMealDetails');
    const selectedIngredientDetailsDiv = document.getElementById('selectedIngredientDetails');

    if (!currentAdventure) {
        if (adventureInfoDiv) adventureInfoDiv.style.display = 'none';
        // Clear all displays if no adventure is selected
        document.getElementById('adventureNameDisplay').textContent = '-';
        document.getElementById('adventureDuration').textContent = '-';
        document.getElementById('adventureCrewSize').textContent = '-';
        document.getElementById('adventureCrewDailyKcalNeed').textContent = '-';
        document.getElementById('adventureWeight').textContent = '-';
        document.getElementById('adventureEnergyDensity').textContent = '-';
        renderNutrients(null, 'adventureNutrients');
        document.getElementById('crewMembersList').innerHTML = '';
        document.getElementById('mealsList').innerHTML = '';
        if (selectedMealDetailsDiv) selectedMealDetailsDiv.style.display = 'none';
        if (selectedIngredientDetailsDiv) selectedIngredientDetailsDiv.style.display = 'none';
        return;
    }

    if (adventureInfoDiv) adventureInfoDiv.style.display = 'block';
    
    // Update Adventure Basic Info
    document.getElementById('adventureNameDisplay').textContent = currentAdventure.name || '-';
    document.getElementById('adventureDuration').textContent = `${currentAdventure.days || 0} days`;
    document.getElementById('adventureCrewSize').textContent = `${currentAdventure.crewSize || 0} persons`;
    document.getElementById('adventureCrewDailyKcalNeed').textContent = `${currentAdventure.crewDailyKcalNeed || 0} kCal / day`;
    document.getElementById('adventureWeight').textContent = `${currentAdventure.weight || 0} kg`;
    document.getElementById('adventureEnergyDensity').textContent = `${currentAdventure.formattedEnergyDensity || '0.0'} kCal / kg`;

    // Render Adventure Nutrients
    renderNutrients(currentAdventure.nutrientsMap, 'adventureNutrients');

    // Update Crew Members
    const crewList = document.getElementById('crewMembersList');
    crewList.innerHTML = '';
    if (currentAdventure.allCrewMembers && Array.isArray(currentAdventure.allCrewMembers)) {
        currentAdventure.allCrewMembers.forEach(member => {
            if (member) {
                const card = document.createElement('div');
                card.className = 'crew-card';
                card.innerHTML = `
                    <div class="card-header">
                        <h4>${member.name || 'Unknown'}</h4>
                        <button class="remove-button" onclick="removeCrewMember('${member.id}')">Remove</button>
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
    }

    // Update Meals
    const mealsList = document.getElementById('mealsList');
    mealsList.innerHTML = '';
    if (currentAdventure.allChildren && Array.isArray(currentAdventure.allChildren)) {
        currentAdventure.allChildren.forEach(mealData => {
            if (mealData && mealData.child) {
                const meal = mealData.child;
                const card = document.createElement('div');
                card.dataset.mealId = meal.id;
                card.className = 'meal-card' + (selectedMeal && selectedMeal.child && selectedMeal.child.id === meal.id ? ' selected' : '');
                card.innerHTML = `
                    <div class="card-header">
                        <h4>${meal.name || 'Unknown'}</h4>
                        <button class="remove-button" onclick="event.stopPropagation(); removeMeal('${meal.id}')">Remove</button>
                    </div>
                    <p>Weight: ${meal.weight || 0}g</p>
                    <p>Energy Density: ${meal.formattedEnergyDensity || '0.0'}</p>
                    <p>Ratio: ${Math.round(mealData.ratio*100) + ' %' || '0.0'}</p>
                    <p>${(meal.allChildren && Array.isArray(meal.allChildren) ? meal.allChildren.length : 0)} ingredients</p>
                `;
                // Pass the full meal object from currentAdventure to preserve nutrientMap
                const fullMealData = currentAdventure.allChildren.find(m => m.child && m.child.id === meal.id);
                card.onclick = () => selectMeal(fullMealData);
                mealsList.appendChild(card);
            }
        });
    }

    // Update Selected Meal Details
    if (selectedMeal && selectedMeal.child && selectedMeal.child.id) {
        // Find the *latest* version of the selected meal from the refreshed adventure data
        const currentMealData = currentAdventure.allChildren.find(m => m.child && m.child.id === selectedMeal.child.id);

        if (!currentMealData) { // Meal might have been deleted in the meantime
            if (selectedMealDetailsDiv) selectedMealDetailsDiv.style.display = 'none';
            if (selectedIngredientDetailsDiv) selectedIngredientDetailsDiv.style.display = 'none';
            selectedMeal = null;
            selectedIngredient = null;
            // Optionally re-render display or handle error
            console.warn('Selected meal not found in current adventure data.');
            return; // Stop further processing for this meal
        }

        const meal = currentMealData.child; // Use the up-to-date child object
        const mealRatio = currentMealData.ratio; // Use the up-to-date ratio

        if (selectedMealDetailsDiv) selectedMealDetailsDiv.style.display = 'block'; 

        document.getElementById('selectedMealName').textContent = meal.name || 'Unknown';
        document.getElementById('selectedMealWeight').textContent = `${meal.weight || 0}g`;
        document.getElementById('selectedMealEnergyDensity').textContent = meal.formattedEnergyDensity || '0.0';
        document.getElementById('selectedMealRatio').textContent = `${(mealRatio * 100).toFixed(1)}%`;

        // Render Meal Nutrients
        renderNutrients(meal.nutrientsMap, 'selectedMealNutrients');

        const ingredientsList = document.getElementById('selectedMealIngredients');
        ingredientsList.innerHTML = '';
        if (meal.allChildren && Array.isArray(meal.allChildren)) {
            meal.allChildren.forEach(ingredientData => {
                if (ingredientData && ingredientData.child) {
                    const ingredient = ingredientData.child;
                    const item = document.createElement('div');
                    item.className = 'ingredient-item' + (selectedIngredient && selectedIngredient.child.id === ingredient.id ? ' selected' : '');
                    item.dataset.ingredientId = ingredient.id;
                    item.innerHTML = `
                        <div class="ingredient-content">
                            <span class="ingredient-name">${ingredient.name || 'Unknown'}</span>
                            <span class="ingredient-details">
                                <span class="ingredient-weight">Weight: ${ingredientData.recipeWeight || 0}g</span>
                                <span class="ingredient-ratio">Ratio: ${(ingredientData.ratio * 100).toFixed(1)}%</span>
                            </span>
                        </div>
                        <button class="remove-button" onclick="event.stopPropagation(); removeIngredient('${ingredient.id}')">Remove</button>
                    `;
                    item.onclick = (e) => {
                        e.stopPropagation();
                        selectIngredient(ingredientData); // Pass the full ingredient data
                    };
                    ingredientsList.appendChild(item);
                }
            });
        } else {
             ingredientsList.innerHTML = '<p>No ingredients added yet.</p>';
        }
    } else {
        // Hide meal details if no meal is selected
        if (selectedMealDetailsDiv) selectedMealDetailsDiv.style.display = 'none';
        // Also hide ingredient details when meal is deselected
        if (selectedIngredientDetailsDiv) selectedIngredientDetailsDiv.style.display = 'none';
        selectedIngredient = null; // Reset selected ingredient
    }

    // Update Selected Ingredient Details
    if (selectedIngredient && selectedIngredient.child && selectedIngredient.child.id) {
        const ingredient = selectedIngredient.child;
        const ingredientRatio = selectedIngredient.ratio;
        const ingredientWeight = selectedIngredient.recipeWeight;

        if (selectedIngredientDetailsDiv) selectedIngredientDetailsDiv.style.display = 'flex'; // Use flex

        document.getElementById('selectedIngredientName').textContent = ingredient.name || 'Unknown';
        document.getElementById('selectedIngredientWeight').textContent = `${ingredientWeight || 0}g`;
        document.getElementById('selectedIngredientRatio').textContent = `${(ingredientRatio * 100).toFixed(1)}%`;

        // Render Ingredient Nutrients
        renderNutrients(ingredient.nutrientsMap, 'selectedIngredientNutrients');
    } else {
        // Hide ingredient details if no ingredient is selected
        if (selectedIngredientDetailsDiv) selectedIngredientDetailsDiv.style.display = 'none';
    }
}

function selectMeal(mealData) {
    if (!mealData || !mealData.child || !mealData.child.id) {
        console.warn('Attempted to select invalid meal data:', mealData);
        selectedMeal = null;
        selectedIngredient = null; // Reset ingredient when meal changes
    } else {
        selectedMeal = mealData; // Store the whole meal data including ratio
        selectedIngredient = null; // Reset ingredient when meal changes
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

// Function to handle ingredient selection
function selectIngredient(ingredientData) {
    if (!ingredientData || !ingredientData.child || !ingredientData.child.id) {
        console.warn('Attempted to select invalid ingredient data:', ingredientData);
        selectedIngredient = null;
    } else {
        selectedIngredient = ingredientData;
        // Update visual selection in the list
        document.querySelectorAll('.ingredient-item').forEach(item => {
            if (item.dataset.ingredientId === selectedIngredient.child.id.toString()) {
                item.classList.add('selected');
            } else {
                item.classList.remove('selected');
            }
        });
    }
    updateAdventureDisplay(); // Refresh display to show ingredient details
}

// Fetch and display adventures
async function fetchAdventures() {
    try {
        const response = await fetch(`${API_BASE_URL}/adventures`);
        const adventures = await response.json();
        
        adventuresList.innerHTML = adventures.map(adventure => `
            <div class="item-card">
                <div class="item-name">${adventure.name}</div>
                <div class="item-details">
                    Created: ${new Date(adventure.creationTime).toLocaleString()}
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error fetching adventures:', error);
        adventuresList.innerHTML = '<div class="error">Failed to load adventures</div>';
    }
}

// Fetch and display crew members
async function fetchCrewMembers() {
    try {
        const response = await fetch(`${API_BASE_URL}/crew-members`);
        const crewMembers = await response.json();
        
        crewMembersList.innerHTML = crewMembers.map(member => `
            <div class="item-card">
                <div class="item-name">${member.name}</div>
                <div class="item-details">
                    ID: ${member.id}
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error fetching crew members:', error);
        crewMembersList.innerHTML = '<div class="error">Failed to load crew members</div>';
    }
}

// Fetch and display base classes
async function fetchBaseClasses() {
    try {
        const response = await fetch(`${API_BASE_URL}/base-classes`);
        const baseClasses = await response.json();
        
        baseClassesList.innerHTML = baseClasses.map(baseClass => `
            <div class="item-card">
                <div class="item-name">${baseClass.name}</div>
                <div class="item-details">
                    Type: ${baseClass.type}<br>
                    Created: ${new Date(baseClass.creationTime).toLocaleString()}
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error fetching base classes:', error);
        baseClassesList.innerHTML = '<div class="error">Failed to load base classes</div>';
    }
}

// Initialize the demo
async function initializeDemo() {
    try {
        // Fetch existing adventures
        const response = await fetch(`${API_BASE_URL}/adventures`);
        if (response.ok) {
            adventures = await response.json();
            updateAdventureDropdown();
        }
    } catch (error) {
        console.error('Error fetching adventures:', error);
    }

    // Ensure the adventure info is hidden initially
    const adventureInfo = document.getElementById('adventureInfo');
    if (adventureInfo) {
        adventureInfo.style.display = 'none';
    }
}

// Start the demo when the page loads
document.addEventListener('DOMContentLoaded', initializeDemo);

// Crew Member Removal
async function removeCrewMember(crewId) {
    if (!currentAdventure) {
        alert('Please select an adventure first');
        return;
    }

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

    try {
        const response = await fetch(`${API_BASE_URL}/adventures/${currentAdventure.id}/meals/${mealId}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error('Failed to remove meal');
        }

        if (selectedMeal && selectedMeal.id === mealId) {
            selectedMeal = null;
            selectedIngredient = null; // Also reset ingredient if its meal is removed
        }
        await refreshCurrentAdventure();
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

    try {
        const response = await fetch(`${API_BASE_URL}/meals/${mealId}/ingredients/${ingredientId}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error('Failed to remove ingredient');
        }

        if (selectedIngredient && selectedIngredient.child.id === ingredientId) {
            selectedIngredient = null;
        }

        await refreshCurrentAdventure();
    } catch (error) {
        alert('Error removing ingredient: ' + error.message);
    }
} 