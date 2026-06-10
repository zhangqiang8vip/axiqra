-- Migration: 003-fix-candidate-seed-unique-index
-- Target: axiqra_candidate_seed
-- Purpose: Align DB uniqueness with application idempotency semantics.
--          Candidate seeds should be unique only within the same workspace
--          and only for active (is_deleted = FALSE) rows.
-- Run after init.sql on an existing database.

-- Step 1: Drop old global unique index if it exists.
DROP INDEX IF EXISTS axiqra_candidate_seed@idx_query_hash;

-- Step 2: Add workspace-scoped partial unique index.
CREATE UNIQUE INDEX IF NOT EXISTS idx_workspace_query_hash_active
ON axiqra_candidate_seed (workspace_id, query_hash)
WHERE is_deleted = FALSE;
