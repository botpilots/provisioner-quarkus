// API endpoints
const API_BASE_URL = 'http://localhost:8080';

// State variables
let adventures = [];  // Store all adventures
let currentAdventure = null;
let selectedMeal = null;
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
        
        const newData = await response.json();
        if (hasDataChanged(newData, lastAdventureData)) {
            lastAdventureData = newData;
            currentAdventure = newData;
            updateAdventureDisplay();
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
        updateAdventureDisplay();
    } catch (error) {
        console.error('Error fetching adventure details:', error);
        alert('Error loading adventure details: ' + error.message);
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
        const response = await fetch(`${API_BASE_URL}/meals/${selectedMeal.id}/ingredients?name=${encodeURIComponent(name)}`, {
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
    if (!currentAdventure || !selectedMeal) {
        alert('Please select a meal first');
        return;
    }

    const weight = document.getElementById('ingredientWeight').value;
    const selectedIngredient = document.querySelector('.ingredient-item.selected');
    if (!selectedIngredient) {
        alert('Please select an ingredient first');
        return;
    }

    const ingredientId = selectedIngredient.dataset.ingredientId;
    try {
        const response = await fetch(`${API_BASE_URL}/meals/${selectedMeal.id}/ingredients/${ingredientId}?weight=${weight}`, {
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
    if (!currentAdventure) {
        document.getElementById('adventureInfo').style.display = 'none';
        return;
    }

    document.getElementById('adventureInfo').style.display = 'block';
    document.getElementById('adventureNameDisplay').textContent = currentAdventure.name || '';
    document.getElementById('adventureDuration').textContent = `${currentAdventure.days || 0} days`;

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
                card.className = 'meal-card' + (selectedMeal && selectedMeal.id === meal.id ? ' selected' : '');
                card.innerHTML = `
                    <div class="card-header">
                        <h4>${meal.name || 'Unknown'}</h4>
                        <button class="remove-button" onclick="event.stopPropagation(); removeMeal('${meal.id}')">Remove</button>
                    </div>
                    <p>Weight: ${meal.weight || 0}g</p>
                    <p>Energy Density: ${meal.formattedEnergyDensity || '0.0'}</p>
                    <p>${(meal.allChildren && Array.isArray(meal.allChildren) ? meal.allChildren.length : 0)} ingredients</p>
                `;
                card.onclick = () => selectMeal(meal);
                mealsList.appendChild(card);
            }
        });
    }

    // Update Selected Meal Details
    if (selectedMeal && selectedMeal.id) {
        const meal = currentAdventure.allChildren && 
            currentAdventure.allChildren.find(m => m.child && m.child.id === selectedMeal.id)?.child;
        if (meal) {
            document.getElementById('selectedMealName').textContent = meal.name || 'Unknown';
            const ingredientsList = document.getElementById('selectedMealIngredients');
            ingredientsList.innerHTML = '';
            if (meal.allChildren && Array.isArray(meal.allChildren)) {
                meal.allChildren.forEach(ingredientData => {
                    if (ingredientData && ingredientData.child) {
                        const ingredient = ingredientData.child;
                        const item = document.createElement('div');
                        item.className = 'ingredient-item';
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
                            document.querySelectorAll('.ingredient-item').forEach(i => i.classList.remove('selected'));
                            item.classList.add('selected');
                        };
                        ingredientsList.appendChild(item);
                    }
                });
            }
        }
    }
}

function selectMeal(meal) {
    if (!meal || !meal.id) {
        console.warn('Attempted to select invalid meal:', meal);
        return;
    }
    
    selectedMeal = meal;
    document.querySelectorAll('.meal-card').forEach(card => {
        const mealId = card.dataset.mealId;
        if (mealId && mealId === meal.id.toString()) {
            card.classList.add('selected');
        } else {
            card.classList.remove('selected');
        }
    });
    updateAdventureDisplay();
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
        }
        await refreshCurrentAdventure();
    } catch (error) {
        alert('Error removing meal: ' + error.message);
    }
}

// Ingredient Removal
async function removeIngredient(ingredientId) {
    if (!selectedMeal) {
        alert('Please select a meal first');
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/meals/${selectedMeal.id}/ingredients/${ingredientId}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error('Failed to remove ingredient');
        }

        await refreshCurrentAdventure();
    } catch (error) {
        alert('Error removing ingredient: ' + error.message);
    }
} 