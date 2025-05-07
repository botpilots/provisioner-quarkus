#!/bin/bash
set -e

# This script stops and removes the local PostgreSQL Docker container
# used for development (provisioner_db).
# WARNING: This permanently deletes all data stored in the local
# development database, including the Flyway schema history table.
#
# Run this script when you have modified an *already applied* Flyway
# migration script (like V1) and need to force Flyway to apply the
# modified version against a fresh database during the next
# application startup ('./mvnw quarkus:dev').
# This is generally discouraged in favor of creating new migration scripts,
# but can be used carefully during early development BEFORE committing changes.

CONTAINER_NAME="provisioner_db"

echo "INFO: Attempting to stop container '${CONTAINER_NAME}'..."
docker stop "${CONTAINER_NAME}" &> /dev/null || echo "INFO: Container '${CONTAINER_NAME}' was not running or does not exist."

echo "INFO: Attempting to remove container '${CONTAINER_NAME}'..."
# The 'docker rm' command will remove the container and its anonymous volumes.
docker rm "${CONTAINER_NAME}" &> /dev/null || echo "INFO: Container '${CONTAINER_NAME}' was already removed or does not exist."

echo "INFO: Local database container '${CONTAINER_NAME}' stopped and removed."
echo "INFO: You can now run 'init_db.sh' to start a fresh container, and then start your application."
echo "INFO: Flyway should now apply migrations from V1 onwards." 