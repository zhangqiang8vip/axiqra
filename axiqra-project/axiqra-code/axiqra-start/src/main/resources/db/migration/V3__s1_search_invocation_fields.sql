-- Axiqra S1 MVP 迁移脚本
-- 版本: V3__s1_search_invocation_fields.sql
-- 描述: SearchRequest/SearchResultItemVO/InvocationEntity 字段补全
-- 
-- 变更内容:
-- 1. axiqra_invocation: 新增 caller_type, invocation_status, task_goal, error_signature, 
--                       tech_stack, environment, context_hash, fit_score, returned_results_count,
--                       prior_attempts, problem_type, user_intent
-- 
-- 对应文档:
-- - D12 §3/§11: SearchRequest 补充字段 (error_signature, environment, project_context, risk_hint, expected_action, prior_attempts, problem_type)
-- - D12 §7/§14: SearchResultItemVO 补充字段 (result_type, matched_terms, fit_reason, caution_reason, evidence_summary, verification_explanation, risk_explanation, space_source, recommended_next_action, sample_count, success_rate, failure_paths)
-- - D06 §15: InvocationEntity 补充字段 (caller_type, invocation_status, task_goal, error_signature, tech_stack, environment, context_hash, fit_score, returned_results_count, prior_attempts, problem_type, user_intent)
-- 
-- CockroachDB 兼容

USE axiqra;

-- ===================== Invocation 新增字段 (D06 §15) =====================

ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS caller_type VARCHAR(50);
ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS invocation_status VARCHAR(50);
ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS task_goal TEXT;
ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS error_signature TEXT;
ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS tech_stack VARCHAR(500);
ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS environment VARCHAR(500);
ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS context_hash VARCHAR(100);
ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS fit_score DOUBLE PRECISION;
ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS returned_results_count INT;
ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS prior_attempts TEXT;
ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS problem_type VARCHAR(100);
ALTER TABLE axiqra_invocation ADD COLUMN IF NOT EXISTS user_intent VARCHAR(50);

-- ===================== 索引 =====================
-- context_hash 查询优化
CREATE INDEX IF NOT EXISTS idx_invocation_context_hash ON axiqra_invocation (context_hash) WHERE context_hash IS NOT NULL;

-- task_goal 全文搜索优化 (可选)
CREATE INDEX IF NOT EXISTS idx_invocation_task_goal ON axiqra_invocation (task_goal) WHERE task_goal IS NOT NULL;

-- error_signature 精确匹配
CREATE INDEX IF NOT EXISTS idx_invocation_error_signature ON axiqra_invocation (error_signature) WHERE error_signature IS NOT NULL;

-- problem_type + invocation_status 组合查询
CREATE INDEX IF NOT EXISTS idx_invocation_problem_status ON axiqra_invocation (problem_type, invocation_status) WHERE problem_type IS NOT NULL;
