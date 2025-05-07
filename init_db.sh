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