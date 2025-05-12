CREATE TABLE IF NOT EXISTS ingredients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    name_sv VARCHAR(255) NOT NULL UNIQUE,
    -- Core nutrient ratios
    protein_ratio NUMERIC(5, 4) NOT NULL DEFAULT 0.0 CHECK (protein_ratio >= 0.0 AND protein_ratio <= 1.0),
    fat_ratio NUMERIC(5, 4) NOT NULL DEFAULT 0.0 CHECK (fat_ratio >= 0.0 AND fat_ratio <= 1.0),
    carbs_ratio NUMERIC(5, 4) NOT NULL DEFAULT 0.0 CHECK (carbs_ratio >= 0.0 AND carbs_ratio <= 1.0),
    fiber_ratio NUMERIC(5, 4) NOT NULL DEFAULT 0.0 CHECK (fiber_ratio >= 0.0 AND fiber_ratio <= 1.0),
    salt_ratio NUMERIC(5, 4) NOT NULL DEFAULT 0.0 CHECK (salt_ratio >= 0.0 AND salt_ratio <= 1.0),
    -- Density, null should result in that Ingredient default density 1.0 will be used.
    density_g_ml NUMERIC(6, 3) CHECK (density_g_ml > 0.0),

    -- Metadata columns
    date_created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modified TIMESTAMP WITH TIME ZONE NULL,
    created_by_user_id UUID NULL, -- Assuming user IDs are UUIDs

    -- Constraints
    -- The sum of all nutrient ratios must be less than or equal to 1.0.
    CONSTRAINT chk_nutrient_ratios_sum CHECK (
        (protein_ratio + fat_ratio + carbs_ratio + fiber_ratio + salt_ratio) <= 1.0
    )
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_ingredients_name ON ingredients (name);
CREATE INDEX IF NOT EXISTS idx_ingredients_name_sv ON ingredients (name_sv);
CREATE INDEX IF NOT EXISTS idx_ingredients_created_by_user_id ON ingredients (created_by_user_id);

-- Reminder: Trigger for automatically updating date_modified on UPDATE
-- should be added in a separate, subsequent migration (e.g., V_Next__...). 