-- Engineering Trace 添加 evidence_path 字段
-- 对应 D05 双主线流程：Engineering Trace Package 包含 6 路径（forward/reverse/decision/evidence/rollback/evolution）

ALTER TABLE axiqra_engineering_trace
ADD COLUMN IF NOT EXISTS evidence_path JSONB;

COMMENT ON COLUMN axiqra_engineering_trace.evidence_path IS '证据路径 - 证据文件引用列表 (JSON array)，包含日志、diff、测试结果、截图等';

-- 创建索引支持证据查询
CREATE INDEX IF NOT EXISTS idx_trace_evidence_path ON axiqra_engineering_trace USING GIN (evidence_path)
    WHERE evidence_path IS NOT NULL;
