# Database Schema Management with Flyway

Hi Oskar,

This document outlines the database schema management strategy implemented using Flyway within the Provisioner Quarkus application.

## 1. Goal Achieved

The primary goal was to establish an automated and reliable process for managing database schema changes, ensuring consistency across environments (development, testing, production) and preventing accidental data loss or schema state mismatches during deployments.

## 2. What Was Done

*   **Flyway Integration:** The `quarkus-flyway` extension was added to the project (`pom.xml`). Flyway is a widely-used database migration tool.
*   **Configuration:** Necessary properties were added to `src/main/resources/application.properties` to enable and configure Flyway:
    *   `quarkus.flyway.migrate-at-start=true`: Tells Quarkus to automatically run Flyway migrations when the application starts up.
    *   `quarkus.flyway.baseline-on-migrate=true`: Handles the scenario where the database might already have the initial schema (e.g., from previous manual setup). It creates Flyway's tracking table and marks the baseline version as applied without re-running the script, preventing errors.
*   **Migration Structure:**
    *   The standard Flyway migration directory `src/main/resources/db/migration` was created.
    *   The existing `create_ingredients.sql` was converted into the first versioned migration script: `V1__create_ingredients_table.sql`. It includes `IF NOT EXISTS` clauses for idempotency.
    *   A placeholder script `V2__load_initial_ingredients.sql` was created for loading initial seed data.
*   **Compatibility:** Explicit dependencies for `flyway-core` and `flyway-database-postgresql` (version 10.13.0) were added to `pom.xml` to ensure compatibility with the PostgreSQL 15.2 database version being used.

## 3. How It Works

1.  **Migration Files:** Database changes (schema creation, alterations, data seeding) are defined in SQL scripts placed within `src/main/resources/db/migration`.
2.  **Naming Convention:** Scripts follow the pattern `V<VERSION>__<Description>.sql` (e.g., `V1__create_ingredients_table.sql`, `V2__load_initial_ingredients.sql`). The `V`, two underscores `__`, and `.sql` are mandatory. The version numbers must be unique and sequential (1, 2, 3...).
3.  **Tracking Table:** Flyway creates and manages a table in your database (usually named `flyway_schema_history`) to keep track of which migration scripts have already been successfully applied.
4.  **Automatic Execution:** When the Quarkus application starts, Flyway checks the migrations directory against the history table. It automatically executes any new, pending migration scripts in version order.

## 4. Current Status & Next Steps for You

Flyway is integrated and configured. The initial schema creation script (`V1`) exists.

Here are the immediate next steps:

1.  **Populate Initial Data:** Add your initial seed data `INSERT` statements to `V2__load_initial_ingredients.sql`.
    *   **Crucially:** Make these `INSERT` statements **idempotent**. Since `ingredients.name` is `UNIQUE`, you can use `INSERT INTO ingredients (...) VALUES (...) ON CONFLICT (name) DO NOTHING;`. This prevents errors or duplicate data if the application restarts and Flyway runs again.
2.  **Test:** Run the application (`./mvnw quarkus:dev`) and ensure Flyway runs without errors (check the logs). Verify the `ingredients` and `flyway_schema_history` tables exist in your database.
3.  **Cleanup Old Scripts:**
    *   Once Flyway is confirmed working, **delete** the now-obsolete `src/main/resources/scripts` directory.
    *   Modify `init_db.sh`: Remove the `docker exec ... psql ... -f /scripts/...` lines. The script should only set up the Docker container; Quarkus/Flyway handles the schema now.
    *   Modify `deploy_db_gcloud.sh`: Remove the `cat ... | gcloud sql connect ...` lines that applied the old scripts. This script should now only manage the Cloud SQL instance/database creation; the deployed application will handle migrations on startup.

## 5. Adding Future Migrations

This is the core workflow going forward:

1.  **Identify Change:** Need to add a column, create a new table, modify data, etc.?
2.  **Create New Script:** Add a *new* file to `src/main/resources/db/migration`.
3.  **Name Correctly:** Use the next available version number. If the last script was `V2__...`, the next one *must* start with `V3__` (e.g., `V3__Add_description_to_ingredients.sql`).
4.  **Write SQL:** Add your `ALTER TABLE`, `CREATE TABLE`, `UPDATE`, `INSERT`, etc. statements. Aim for idempotency where practical (e.g., `ALTER TABLE ... ADD COLUMN IF NOT EXISTS ...`).
5.  **Commit:** Add the new migration script to Git along with your application code changes.
6.  **Deploy:** When the application starts (locally or in deployed environments), Flyway will automatically detect and apply the new `V3__...` script if it hasn't run against that specific database instance before.

## 6. Why Idempotency Matters (`IF NOT EXISTS` / `ON CONFLICT`)

You'll notice the V1 script uses `CREATE TABLE IF NOT EXISTS` and the recommendation for V2 is to use `INSERT ... ON CONFLICT (...) DO NOTHING`. This practice of making migration scripts **idempotent** (safe to run multiple times) is highly recommended:

*   **`IF NOT EXISTS` (for `CREATE TABLE`, `CREATE INDEX`, `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`, etc.):**
    *   **Robustness:** Prevents hard database errors if the object already exists. This is common in local development (restarting `./mvnw quarkus:dev` without clearing the DB volume), after failed test cleanups, or potential manual interventions.
    *   **Low Cost:** Adds negligible performance overhead and doesn't complicate the SQL significantly.
    *   **Safety:** Acts as a valuable safety net, allowing Flyway's execution (and features like baselining) to proceed more gracefully even if the database isn't in a perfectly expected initial state.

*   **`INSERT ... ON CONFLICT (unique_column) DO NOTHING` (for initial data loads/seeding):
    *   **Resilience to Retries:** This is crucial for recovering from *external interruptions* (crashes) during the *first run* of a data load script. If the application crashes midway through V2, Flyway will retry V2 on the next startup. `ON CONFLICT DO NOTHING` allows the retry to skip rows already inserted in the previous partial attempt and continue inserting the rest, leading to automatic recovery. Without it, the retry would likely fail immediately on the first existing row, requiring manual cleanup.
    *   **Handling Script Duplicates:** It also silently handles cases where your *source data script itself* might contain duplicate unique keys (e.g., two 'Apple' rows). While this hides potential data quality issues in the script (which ideally should be validated elsewhere), it prevents the entire migration from failing.
    *   **Note:** This is primarily for the *initial* data load. Subsequent *updates* to existing data should always be done via standard `UPDATE` statements in *new* migration scripts (V3, V4, etc.).

While Flyway runs each migration script within an atomic transaction (rolling back on SQL errors *within* that run), idempotency addresses scenarios *across* runs and retries, making the overall migration process significantly more robust and developer-friendly, especially in non-production environments.

## 7. Verifying the Database Schema

You might want to manually inspect the database to confirm any changes:

1.  **Ensure the container is running:**
    ```bash
    docker ps
    ```
    You should see `provisioner_db` listed. If not, start it with `./init_db.sh`.

2.  **Execute `psql` inside the container:**
    ```bash
    docker exec -it provisioner_db psql -U postgres
    ```
    *   `docker exec`: Runs a command in a running container.
    *   `-it`: Allocates an interactive pseudo-TTY (allows you to interact with `psql`).
    *   `provisioner_db`: The name of your database container.
    *   `psql -U postgres`: Runs the `psql` client, connecting as the default `postgres` user to the default `postgres` database.

3.  **Inside the `psql` prompt:**
    *   To describe the `ingredients` table structure and see the columns:
        ```sql
        \d ingredients
        ```
        This will list all columns, their types, default values, nullability, and constraints/indexes. Visually confirm that `date_created`, `date_modified`, `is_user_created`, etc., are present with the correct definitions.
    *   To check the Flyway history table:
        ```sql
        SELECT * FROM flyway_schema_history;
        ```
        You should see rows indicating that V1 and V2 have been applied successfully (their `success` column should be true).
    *   To exit `psql`, type:
        ```sql
        \q
        ```

This will confirm that Flyway applied the changes of the new migration script.

---

Using Flyway provides a controlled, versioned, and automated approach to managing your database schema alongside your application code. Let me know if anything is unclear before I sign off!

Best regards,

Your Consultant AI 