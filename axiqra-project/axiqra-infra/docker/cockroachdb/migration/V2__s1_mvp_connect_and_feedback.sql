-- ============================================================
-- Axiqra 业务库 Flyway 迁移
-- 
-- 此文件用于 Flyway 管理的业务库迁移
-- CockroachDB 兼容语法
-- 
-- 迁移历史:
-- V2__s1_mvp_connect_and_feedback: Connect Session 状态机 + Feedback 幂等性
-- ============================================================

USE axiqra;

-- ===================== Connect Session 新增字段 =====================
-- D09 文档第 9 节规定的接入会话必须记录的字段

ALTER TABLE axiqra_connect_session ADD COLUMN IF NOT EXISTS tool_capability VARCHAR(500);
ALTER TABLE axiqra_connect_session ADD COLUMN IF NOT EXISTS auth_scope VARCHAR(200);
ALTER TABLE axiqra_connect_session ADD COLUMN IF NOT EXISTS doctor_result TEXT;
ALTER TABLE axiqra_connect_session ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMPTZ;
ALTER TABLE axiqra_connect_session ADD COLUMN IF NOT EXISTS instruction_snapshot TEXT;
ALTER TABLE axiqra_connect_session ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(500);

-- ===================== Feedback 新增幂等键 =====================
-- D09 文档第 11 节规定的幂等性支持

ALTER TABLE axiqra_feedback ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100);

-- ===================== 索引 =====================
-- idempotency_key 查询优化
CREATE INDEX IF NOT EXISTS idx_feedback_idempotency_key ON axiqra_feedback (idempotency_key) WHERE idempotency_key IS NOT NULL;

-- last_seen_at 查询优化
CREATE INDEX IF NOT EXISTS idx_connect_last_seen ON axiqra_connect_session (last_seen_at) WHERE last_seen_at IS NOT NULL;
