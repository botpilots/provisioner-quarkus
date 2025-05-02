CREATE TABLE ingredients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,

    -- Core nutrient ratios (stored as exact decimals, e.g., 0.2500 for 25%)
    protein_ratio NUMERIC(5, 4) NOT NULL DEFAULT 0.0 CHECK (protein_ratio >= 0.0 AND protein_ratio <= 1.0),
    fat_ratio NUMERIC(5, 4) NOT NULL DEFAULT 0.0 CHECK (fat_ratio >= 0.0 AND fat_ratio <= 1.0),
    carbs_ratio NUMERIC(5, 4) NOT NULL DEFAULT 0.0 CHECK (carbs_ratio >= 0.0 AND carbs_ratio <= 1.0),
    fiber_ratio NUMERIC(5, 4) NOT NULL DEFAULT 0.0 CHECK (fiber_ratio >= 0.0 AND fiber_ratio <= 1.0),
    salt_ratio NUMERIC(5, 4) NOT NULL DEFAULT 0.0 CHECK (salt_ratio >= 0.0 AND salt_ratio <= 1.0),

    -- Density (e.g., in g/mL or kg/L - stored as exact decimal)
    density_g_ml NUMERIC(6, 3) NOT NULL CHECK (density_g_ml > 0.0),

    -- Constraint to ensure the sum of specified ratios does not exceed 1.0
    -- NUMERIC handles sums exactly, so no tolerance needed here.
    CONSTRAINT chk_nutrient_ratios_sum CHECK (
        (protein_ratio + fat_ratio + carbs_ratio + fiber_ratio + salt_ratio) <= 1.0
    )
);

-- Index for efficient name searching
CREATE INDEX idx_ingredients_name ON ingredients (name); 