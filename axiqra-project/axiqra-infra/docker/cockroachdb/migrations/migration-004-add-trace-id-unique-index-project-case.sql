-- Migration: 004-add-trace-id-unique-index-project-case
-- Target: axiqra_project_case
-- Purpose: Prevent duplicate ProjectCase from the same Trace.
--          Each Trace can only generate one ProjectCase.
-- Run after init.sql on an existing database.
-- If the index already exists, this is a no-op (IF NOT EXISTS).

CREATE UNIQUE INDEX IF NOT EXISTS idx_trace_id_unique
ON axiqra_project_case (trace_id)
WHERE is_deleted = FALSE;
