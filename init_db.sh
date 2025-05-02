#!/bin/bash
set -euo pipefail
# Assuming script is run from project root

# Stop only the database container if running
docker stop provisioner_db &> /dev/null || true

docker run -d --rm \
  --name provisioner_db \
  -p 5432:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -v ./src/main/resources/scripts:/scripts:ro \
  postgres:15.2

echo 'waiting for db startup'
until docker exec provisioner_db pg_isready -h localhost > /dev/null; do
  sleep 0.5
done;

echo 'database container started'

# Load the schema
echo 'loading schema into database'
docker exec provisioner_db psql -U postgres -f /scripts/create_ingredients.sql

# Load initial data
echo 'loading initial data into database'
docker exec provisioner_db psql -U postgres -f /scripts/load_ingredients_data.sql

echo 'database initialized successfully'

echo 'Verifying contents of ingredients table:'
docker exec provisioner_db psql -U postgres -c "SELECT * FROM ingredients;"