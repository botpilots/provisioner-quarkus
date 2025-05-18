#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Variables to track test results
total_tests=0
passed_tests=0
failed_tests=0

# Function to run a test and get confirmation
run_test() {
    local description=$1
    local command=$2
    local expected_output=$3
    
    echo -e "\n${YELLOW}Test: $description${NC}"
    echo "Running: $command"
    
    # Execute the command and capture output
    output=$(eval "$command" 2>&1)
    
    # Format the output with jq if it's JSON
    if [[ $output == *"{"* ]] || [[ $output == *"["* ]]; then
        formatted_output=$(echo "$output" | jq .)
    else
        formatted_output="$output"
    fi
    
    echo -e "\nResponse:"
    echo "$formatted_output"
    
    echo -e "\nExpected output:"
    echo "$expected_output"
    
    # Ask for confirmation
    while true; do
        read -p "Does the output match the expected output? (y/n): " yn
        case $yn in
            [Yy]* ) 
                echo -e "${GREEN}✓ Test passed${NC}"
                ((passed_tests++))
                break;;
            [Nn]* ) 
                echo -e "${RED}✗ Test failed${NC}"
                ((failed_tests++))
                break;;
            * ) echo "Please answer y or n.";;
        esac
    done
    
    ((total_tests++))
}

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo "Error: jq is not installed. Please install it first."
    echo "On macOS: brew install jq"
    echo "On Ubuntu/Debian: sudo apt-get install jq"
    exit 1
fi

echo -e "${YELLOW}Starting API Tests...${NC}"

# Test 1: Create Adventure
run_test "Create a New Adventure" \
    'curl -s -X POST "http://localhost:8080/adventures?name=Summer%20Hiking%20Trip"' \
    '{
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
  "name": "Summer Hiking Trip",
  "creationTime": "ISO-8601 timestamp",
  "crewDailyKcalNeed": 0,
  "days": 0,
  "crewSize": 0,
  "allCrewMembers": [],
  "formattedTotalRatio": "0,0",
  "formattedEnergyDensity": "0.0"
}'

# Store the adventure ID from the response
adventure_id=$(echo "$output" | jq -r '.id')

# Test 2: Add Crew Member
run_test "Add Crew Member" \
    "curl -s -X POST \"http://localhost:8080/adventures/$adventure_id/crew?name=John&age=30&height=180&weight=75&gender=MALE&activity=HEAVY&strategy=harris_benedict_original\"" \
    '{
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
  "name": "Summer Hiking Trip",
  "creationTime": "ISO-8601 timestamp",
  "crewDailyKcalNeed": 3052,
  "days": 0,
  "crewSize": 1,
  "allCrewMembers": [
    {
      "name": "John",
      "age": 30,
      "height": 180,
      "weight": 75,
      "gender": "MALE",
      "activity": "HEAVY",
      "kCalCalculationStrategy": {},
      "creationTime": "ISO-8601 timestamp",
      "dailyKCalNeed": 3052,
      "id": "id_[uuid]"
    }
  ],
  "formattedTotalRatio": "0,0",
  "formattedEnergyDensity": "0.0"
}'

# Test 3: Set Number of Days
run_test "Set Number of Days" \
    "curl -s -X PUT \"http://localhost:8080/adventures/$adventure_id/days?days=5\"" \
    '{
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
  "name": "Summer Hiking Trip",
  "creationTime": "ISO-8601 timestamp",
  "crewDailyKcalNeed": 3052,
  "days": 5,
  "crewSize": 1,
  "allCrewMembers": [
    {
      "name": "John",
      "age": 30,
      "height": 180,
      "weight": 75,
      "gender": "MALE",
      "activity": "HEAVY",
      "kCalCalculationStrategy": {},
      "creationTime": "ISO-8601 timestamp",
      "dailyKCalNeed": 3052,
      "id": "id_[uuid]"
    }
  ],
  "formattedTotalRatio": "0,0",
  "formattedEnergyDensity": "0.0"
}'

# Test 4: Add Meal
run_test "Add Meal" \
    "curl -s -X POST \"http://localhost:8080/adventures/$adventure_id/meals?name=Breakfast%20Oatmeal\"" \
    '"id_[uuid]"'

# Store the meal ID from the response
meal_id=$(echo "$output" | tr -d '"')

# Test 5: Add Ingredient
run_test "Add Ingredient" \
    "curl -s -X POST \"http://localhost:8080/meals/$meal_id/ingredient?name=Oats\"" \
    '"id_[uuid]"'

# Store the ingredient ID from the response
ingredient_id=$(echo "$output" | tr -d '"')

# Test 6: Modify Ingredient Weight
run_test "Modify Ingredient Weight" \
    "curl -s -X PUT \"http://localhost:8080/meals/$meal_id/ingredients/$ingredient_id?weight=100\"" \
    ''

# Test 7: Get Adventure Information
run_test "Get Adventure Information" \
    "curl -s -X GET \"http://localhost:8080/adventures/$adventure_id/info\"" \
    '"Adventure info printed to console"'

# Test 8: Get Meal Information
run_test "Get Meal Information" \
    "curl -s -X GET \"http://localhost:8080/meals/$meal_id/info\"" \
    '"Meal info printed to console"'

# Test 9: Get All Adventures
run_test "Get All Adventures" \
    "curl -s -X GET \"http://localhost:8080/adventures\"" \
    '[{
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
  "name": "Summer Hiking Trip",
  "creationTime": "ISO-8601 timestamp",
  "crewDailyKcalNeed": 3052,
  "days": 5,
  "crewSize": 1,
  "allCrewMembers": [
    {
      "name": "John",
      "age": 30,
      "height": 180,
      "weight": 75,
      "gender": "MALE",
      "activity": "HEAVY",
      "kCalCalculationStrategy": {},
      "creationTime": "ISO-8601 timestamp",
      "dailyKCalNeed": 3052,
      "id": "id_[uuid]"
    }
  ],
  "formattedTotalRatio": "0,0",
  "formattedEnergyDensity": "0.0"
}]'

# Print test summary
echo -e "\n${YELLOW}Test Summary:${NC}"
echo "Total tests: $total_tests"
echo "Passed: $passed_tests"
echo "Failed: $failed_tests"

if [ $failed_tests -gt 0 ]; then
    echo -e "${RED}Some tests failed. Please check the output above.${NC}"
    exit 1
else
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
fi 