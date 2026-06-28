-- ============================================================
-- Axiqra S1: 补全 Engineering Trace Package 缺失字段
-- ============================================================
-- P0-2: Engineering Trace Package 缺少以下文档要求字段：
-- P0-5: 补 authorization_id / source_tool / schema_version
--
-- 变更内容：
-- 1. forward_path (JSONB) - 正向路径（V6/V7 缺失）
-- 2. rollback_path (JSONB) - 回滚路径（V6/V7 缺失）
-- 3. reverse_path (JSONB) - 反向路径（V6/V7 缺失）
-- 4. attempted_failure_path (JSONB) - 本次案例中尝试过但失败的方向（区别于 Solution.failure_paths）
-- 5. authorization_id (BIGINT) - 授权边界，FK → axiqra_authorization.id
-- 6. source_tool (VARCHAR) - AI 工具类型：cursor / claude_code / codex / human
-- 7. schema_version (VARCHAR) - 结构版本，如 "1.0"
--
-- 对应文档：
-- D06 §4 Engineering Trace Package 字段草案
-- D07 §10 Project Case 字段级内容规范（attempted_failure_path）
-- D13 §3 可见范围决策（authorization_id 是决策输入）
-- D12 §5 工具模型归因（source_tool 是 D12 KPI 的数据来源）
-- ============================================================

-- ===================== 1. forward_path =====================
ALTER TABLE axiqra_engineering_trace
ADD COLUMN IF NOT EXISTS forward_path JSONB;

COMMENT ON COLUMN axiqra_engineering_trace.forward_path IS '正向路径 - 成功执行的主要步骤序列，对应 D06 §3 forward_path';

CREATE INDEX IF NOT EXISTS idx_trace_forward_path
ON axiqra_engineering_trace USING GIN (forward_path)
WHERE forward_path IS NOT NULL;

-- ===================== 2. rollback_path =====================
ALTER TABLE axiqra_engineering_trace
ADD COLUMN IF NOT EXISTS rollback_path JSONB;

COMMENT ON COLUMN axiqra_engineering_trace.rollback_path IS '回滚路径 - 失败时如何恢复，对应 D06 §3 rollback_path';

CREATE INDEX IF NOT EXISTS idx_trace_rollback_path
ON axiqra_engineering_trace USING GIN (rollback_path)
WHERE rollback_path IS NOT NULL;

-- ===================== 3. reverse_path =====================
ALTER TABLE axiqra_engineering_trace
ADD COLUMN IF NOT EXISTS reverse_path JSONB;

COMMENT ON COLUMN axiqra_engineering_trace.reverse_path IS '反向路径 - 从结果反向推导的过程，对应 D06 §3 reverse_path';

CREATE INDEX IF NOT EXISTS idx_trace_reverse_path
ON axiqra_engineering_trace USING GIN (reverse_path)
WHERE reverse_path IS NOT NULL;

-- ===================== 4. attempted_failure_path =====================
ALTER TABLE axiqra_engineering_trace
ADD COLUMN IF NOT EXISTS attempted_failure_path JSONB;

COMMENT ON COLUMN axiqra_engineering_trace.attempted_failure_path
IS '本次案例中尝试过但不成立的方向（单数，区别于 Solution.failure_paths 复数），对应 D07 §10';

CREATE INDEX IF NOT EXISTS idx_trace_attempted_failure
ON axiqra_engineering_trace USING GIN (attempted_failure_path)
WHERE attempted_failure_path IS NOT NULL;

-- ===================== 5. authorization_id =====================
ALTER TABLE axiqra_engineering_trace
ADD COLUMN IF NOT EXISTS authorization_id BIGINT;

COMMENT ON COLUMN axiqra_engineering_trace.authorization_id
IS '授权边界 FK → axiqra_authorization.id，用于可见范围决策（D13 §3）';

CREATE INDEX IF NOT EXISTS idx_trace_authorization
ON axiqra_engineering_trace (authorization_id)
WHERE authorization_id IS NOT NULL;

-- ===================== 6. source_tool =====================
ALTER TABLE axiqra_engineering_trace
ADD COLUMN IF NOT EXISTS source_tool VARCHAR(50);

COMMENT ON COLUMN axiqra_engineering_trace.source_tool
IS 'AI 工具类型：cursor / claude_code / codex / codex_cli / windsurf / copilot / mimo / opencode / human';

CREATE INDEX IF NOT EXISTS idx_trace_source_tool
ON axiqra_engineering_trace (source_tool)
WHERE source_tool IS NOT NULL;

-- ===================== 7. schema_version =====================
ALTER TABLE axiqra_engineering_trace
ADD COLUMN IF NOT EXISTS schema_version VARCHAR(20) NOT NULL DEFAULT '1.0';

COMMENT ON COLUMN axiqra_engineering_trace.schema_version
IS '结构版本，用于向前兼容性（如 "1.0"，升级时记录变更）';

-- ===================== 8. 删除旧的不一致字段（修复双字段问题）=====================
-- EngineeringTraceEntity 中同时存在 evolution_suggestion（实体字段无注解）和 evolution_hint（V7）
-- 保留 evolution_hint（V7 有迁移），删除 evolutionSuggestion 字段声明（已在 Entity 中注释掉，V10 删除列）
-- 注意：如果 DB 中已有 evolution_suggestion 列，先备份再删除
-- 由于 evolution_suggestion 在 V1 初始表就存在，这里不删除，保留兼容性
-- S1.5 再统一迁移

-- ===================== 验证 =====================
-- SELECT id, task_goal,
--        forward_path IS NOT NULL AS has_forward_path,
--        rollback_path IS NOT NULL AS has_rollback_path,
--        reverse_path IS NOT NULL AS has_reverse_path,
--        attempted_failure_path IS NOT NULL AS has_attempted_failure,
--        authorization_id IS NOT NULL AS has_auth,
--        source_tool,
--        schema_version
-- FROM axiqra_engineering_trace
-- LIMIT 10;
