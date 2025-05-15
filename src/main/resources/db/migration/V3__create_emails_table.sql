CREATE TABLE IF NOT EXISTS emails (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
	date_modified TIMESTAMP,
	date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	-- opt out status
	opt_out BOOLEAN NOT NULL DEFAULT FALSE,
	-- opt out date
	opt_out_date TIMESTAMP,
	-- opt out reason
	opt_out_reason VARCHAR(255),

    CONSTRAINT chk_nutrient_ratios_sum CHECK (
        -- email must be valid
        email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
    )
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_emails_email ON emails (email);
