-- Migration: 005-add-tool-model-attribution-to-invocation
-- Target: axiqra_invocation
-- Purpose: Add tool and model attribution fields to invocation table
--          These fields allow reporting tool/model info with each invocation

-- Step 1: Add tool attribution columns
ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS tool_name STRING;
ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS tool_vendor STRING;
ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS tool_version STRING;
ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS client_channel STRING;

-- Step 2: Add model attribution columns
ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS model_provider STRING;
ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS model_name STRING;
ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS model_version STRING;
ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS model_source STRING;
ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS model_confidence STRING;

-- Step 3: Create index for tool model queries
CREATE INDEX IF NOT EXISTS idx_invocation_tool_name ON axiqra_invocation(tool_name) WHERE tool_name IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_invocation_model_name ON axiqra_invocation(model_name) WHERE model_name IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_invocation_client_channel ON axiqra_invocation(client_channel) WHERE client_channel IS NOT NULL;
