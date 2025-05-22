#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

# === Configuration ===
# TODO: Review and fill in these values before running the script.

# Google Cloud Project ID
GCP_PROJECT_ID="provisioner-quarkus" # Ensure this is your correct project ID

# Cloud SQL Configuration
# Choose a region for your Cloud SQL instance (e.g., us-central1, europe-west1)
CLOUD_SQL_REGION="europe-north2" # Match your other resources or choose appropriately
# Name for your Cloud SQL instance (e.g., my-app-db-instance)
CLOUD_SQL_INSTANCE_NAME="provisioner-db-instance"
# Database engine version (e.g., POSTGRES_15)
DB_VERSION="POSTGRES_15"
# Name for the database within the instance (e.g., my_app_db)
DB_NAME="provisioner_db"
# Name for the database user to connect as (default 'postgres' often works)
DB_USER="postgres"
# NOTE: Password management is not handled here. 'gcloud sql connect' typically uses IAM.
# If your application needs a password, create the user and set the password securely
# (e.g., 'gcloud sql users set-password' or via Secret Manager).

# Cloud SQL Instance Machine Type (e.g., db-f1-micro, db-g1-small, or custom)
# Using a small, shared-core machine type for cost-effectiveness in dev/test.
# See https://cloud.google.com/sql/docs/postgres/instance-settings#machine-type-2nd-gen
CLOUD_SQL_TIER="db-f1-micro" # Or adjust as needed, e.g., "db-g1-small"

# Cloud SQL Storage Settings
CLOUD_SQL_STORAGE_TYPE="SSD"  # Or HDD
CLOUD_SQL_STORAGE_SIZE="10GB" # Minimum size

# === Prerequisites Check (Informational) ===
echo "INFO: Ensure you have:"
echo "1. Google Cloud SDK ('gcloud') installed and authenticated ('gcloud auth login')."
echo "2. The correct GCP project selected ('gcloud config set project ${GCP_PROJECT_ID}')."
echo "3. Permissions in GCP to manage Cloud SQL (e.g., roles/cloudsql.admin)."
read -p "Press Enter to continue if prerequisites are met..."

# === Script Execution ===

echo "INFO: Enabling necessary Google Cloud APIs..."
gcloud services enable sqladmin.googleapis.com \
	--project="${GCP_PROJECT_ID}"

echo "INFO: Checking Cloud SQL instance '${CLOUD_SQL_INSTANCE_NAME}'..."
# Check if instance exists by trying to describe it; suppress errors if it doesn't exist.
if gcloud sql instances describe "${CLOUD_SQL_INSTANCE_NAME}" --project="${GCP_PROJECT_ID}" >/dev/null 2>&1; then
	echo "INFO: Cloud SQL Instance '${CLOUD_SQL_INSTANCE_NAME}' already exists."
else
	echo "INFO: Instance '${CLOUD_SQL_INSTANCE_NAME}' does not exist. Creating..."
	echo "NOTE: Creating an instance can take several minutes."
	gcloud sql instances create "${CLOUD_SQL_INSTANCE_NAME}" \
		--project="${GCP_PROJECT_ID}" \
		--database-version="${DB_VERSION}" \
		--tier="${CLOUD_SQL_TIER}" \
		--region="${CLOUD_SQL_REGION}" \
		--storage-type="${CLOUD_SQL_STORAGE_TYPE}" \
		--storage-size="${CLOUD_SQL_STORAGE_SIZE}"
	# Add --root-password=PASSWORD flag here ONLY IF NEEDED and handle securely, IAM auth is preferred.
	# Add --database-flags=FLAG=VALUE for specific Postgres settings if required.
	echo "INFO: Instance creation initiated. It may take several minutes to become fully available."
	# Note: The script proceeds without explicitly waiting for RUNNABLE state,
	# subsequent commands might fail if the instance isn't ready fast enough.
	# A more robust script might poll `gcloud sql instances describe` status.
fi

# Ensure the database exists within the instance
echo "INFO: Checking/Creating database '${DB_NAME}' in instance '${CLOUD_SQL_INSTANCE_NAME}'..."
if gcloud sql databases describe "${DB_NAME}" --instance="${CLOUD_SQL_INSTANCE_NAME}" --project="${GCP_PROJECT_ID}" >/dev/null 2>&1; then
	echo "INFO: Database '${DB_NAME}' already exists."
else
	gcloud sql databases create "${DB_NAME}" \
		--instance="${CLOUD_SQL_INSTANCE_NAME}" \
		--project="${GCP_PROJECT_ID}"
	echo "INFO: Database '${DB_NAME}' created."
fi

# Note: User '${DB_USER}' (usually 'postgres') should exist by default.
# If using a custom user, you would add steps here to create it and set its password:
# gcloud sql users create NEW_USER --instance=... --password=... --project=...
# Make sure the user has necessary permissions on the database.

echo "INFO: Cloud SQL database deployment and initialization process completed!"
echo "--------------------------------------------------"
echo "  Instance Name: ${CLOUD_SQL_INSTANCE_NAME}"
echo "  Database Name: ${DB_NAME}"
echo "  User:          ${DB_USER}"
echo "  Region:        ${CLOUD_SQL_REGION}"
echo "  Connect using: gcloud sql connect ${CLOUD_SQL_INSTANCE_NAME} --user=${DB_USER} --project=${GCP_PROJECT_ID}"
echo "--------------------------------------------------"
