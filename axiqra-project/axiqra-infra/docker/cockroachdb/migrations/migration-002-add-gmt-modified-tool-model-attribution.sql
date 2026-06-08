-- Migration: 002-add-gmt-modified-tool-model-attribution
-- Target: axiqra_tool_model_attribution
-- Purpose: Add gmt_modified column with NOT NULL constraint for existing installs.
--          First-time installs get it from init.sql; this script handles upgrades.
-- Run after init.sql on an existing database with NULL gmt_modified values.

-- Step 1: Add column if missing
ALTER TABLE axiqra_tool_model_attribution ADD COLUMN IF NOT EXISTS gmt_modified TIMESTAMPTZ;

-- Step 2: Backfill NULL values using gmt_create
UPDATE axiqra_tool_model_attribution
SET gmt_modified = COALESCE(gmt_create, now())
WHERE gmt_modified IS NULL;

-- Step 3: Enforce NOT NULL (requires all NULLs resolved first)
ALTER TABLE axiqra_tool_model_attribution ALTER COLUMN gmt_modified SET NOT NULL;
ALTER TABLE axiqra_tool_model_attribution ALTER COLUMN gmt_modified SET DEFAULT now();
