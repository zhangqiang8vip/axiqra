-- axiqra-project/axiqra-infra/docker/postgres-audit/init.sql
-- PostgreSQL 审计库建表脚本（append-only）
-- 独立部署，仅存储 append-only 审计日志

-- 创建审计日志表（禁止 UPDATE 和 DELETE）
CREATE TABLE IF NOT EXISTS axq_audit_log (
    id              BIGSERIAL        PRIMARY KEY,
    gmt_create      TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    event_name      VARCHAR(100)     NOT NULL,
    actor_id        BIGINT           NULL,
    actor_type      VARCHAR(30)      NULL,
    object_type     VARCHAR(50)      NULL,
    object_id       BIGINT           NULL,
    action_type     VARCHAR(50)      NOT NULL,
    decision        VARCHAR(50)       NULL,
    reason_code     VARCHAR(50)       NULL,
    payload         JSONB            NULL,
    ip_address      VARCHAR(64)       NULL,
    user_agent      VARCHAR(500)     NULL,
    request_id      VARCHAR(64)      NULL,
    tenant_id       BIGINT          NULL
);

-- 列注释
COMMENT ON COLUMN axq_audit_log.gmt_create IS '发生时间';
COMMENT ON COLUMN axq_audit_log.event_name IS '事件名称';
COMMENT ON COLUMN axq_audit_log.actor_id IS '操作者用户ID';
COMMENT ON COLUMN axq_audit_log.actor_type IS '操作者类型：user/system/ai_tool';
COMMENT ON COLUMN axq_audit_log.object_type IS '对象类型';
COMMENT ON COLUMN axq_audit_log.object_id IS '对象ID';
COMMENT ON COLUMN axq_audit_log.action_type IS '动作类型：create/read/update/delete/publish/revoke';
COMMENT ON COLUMN axq_audit_log.decision IS '决策结果：allow/deny/quota_exceeded';
COMMENT ON COLUMN axq_audit_log.reason_code IS '原因码';
COMMENT ON COLUMN axq_audit_log.payload IS '附加数据';
COMMENT ON COLUMN axq_audit_log.ip_address IS 'IP地址';
COMMENT ON COLUMN axq_audit_log.user_agent IS 'User-Agent';
COMMENT ON COLUMN axq_audit_log.request_id IS '请求追踪ID（与日志 traceId 一致）';
COMMENT ON COLUMN axq_audit_log.tenant_id IS '租户ID';

-- 禁止 UPDATE 和 DELETE（PostgreSQL 行级安全策略强制）
ALTER TABLE axq_audit_log ENABLE ROW LEVEL SECURITY;
CREATE POLICY audit_no_update ON axq_audit_log FOR UPDATE USING (false);
CREATE POLICY audit_no_delete ON axq_audit_log FOR DELETE USING (false);
CREATE POLICY audit_allow_insert ON axq_audit_log FOR INSERT WITH CHECK (true);

-- 索引
CREATE INDEX IF NOT EXISTS idx_audit_event_name ON axq_audit_log (event_name);
CREATE INDEX IF NOT EXISTS idx_audit_actor_id ON axq_audit_log (actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_object_id ON axq_audit_log (object_id);
CREATE INDEX IF NOT EXISTS idx_audit_action_type ON axq_audit_log (action_type);
CREATE INDEX IF NOT EXISTS idx_audit_request_id ON axq_audit_log (request_id);
CREATE INDEX IF NOT EXISTS idx_audit_gmt_create ON axq_audit_log (gmt_create);

COMMENT ON TABLE axq_audit_log IS '审计日志表（append-only，禁止更新和删除，通过 PostgreSQL RLS 强制）';
