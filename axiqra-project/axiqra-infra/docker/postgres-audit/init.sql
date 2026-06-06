-- axiqra-project/axiqra-infra/docker/postgres-audit/init.sql
-- PostgreSQL 审计库建表脚本（append-only）
-- 表前缀统一为 axiqra_（与 axiqra- 项目名一致）

-- ===================== 通用审计事件 =====================
CREATE TABLE IF NOT EXISTS axiqra_audit_event (
    id BIGSERIAL PRIMARY KEY,
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    request_id VARCHAR(96) NULL,
    actor_id BIGINT NULL,
    actor_type VARCHAR(32) NULL,
    action VARCHAR(64) NOT NULL,
    object_type VARCHAR(64) NULL,
    object_id BIGINT NULL,
    result VARCHAR(32) NULL,
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(500) NULL,
    payload JSONB NULL,
    tenant_id BIGINT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_request_id ON axiqra_audit_event (request_id);
CREATE INDEX IF NOT EXISTS idx_audit_actor_id ON axiqra_audit_event (actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_object ON axiqra_audit_event (object_type, object_id);
CREATE INDEX IF NOT EXISTS idx_audit_action ON axiqra_audit_event (action);
CREATE INDEX IF NOT EXISTS idx_audit_gmt_create ON axiqra_audit_event (gmt_create);

-- ===================== Policy Decision 决策日志 =====================
CREATE TABLE IF NOT EXISTS axiqra_policy_decision_log (
    id BIGSERIAL PRIMARY KEY,
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    request_id VARCHAR(96) NOT NULL,
    subject JSONB NOT NULL,
    object JSONB NOT NULL,
    action VARCHAR(64) NOT NULL,
    decision VARCHAR(32) NOT NULL,
    policy_code VARCHAR(64) NULL,
    policy_version VARCHAR(32) NULL,
    reason_code VARCHAR(64) NULL,
    tenant_id BIGINT NULL
);

CREATE INDEX IF NOT EXISTS idx_policy_request_id ON axiqra_policy_decision_log (request_id);
CREATE INDEX IF NOT EXISTS idx_policy_decision ON axiqra_policy_decision_log (decision);
CREATE INDEX IF NOT EXISTS idx_policy_tenant ON axiqra_policy_decision_log (tenant_id);

-- ===================== Invocation 调用日志 =====================
CREATE TABLE IF NOT EXISTS axiqra_invocation_log (
    id BIGSERIAL PRIMARY KEY,
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    request_id VARCHAR(96) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    tool_type VARCHAR(50) NULL,
    caller_id BIGINT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    workspace_id BIGINT NULL,
    risk_level VARCHAR(32) NULL,
    confirmation_obtained SMALLINT NOT NULL DEFAULT 0,
    result VARCHAR(32) NULL,
    latency_ms BIGINT NULL,
    status VARCHAR(32) NULL
);

CREATE INDEX IF NOT EXISTS idx_invoke_request_id ON axiqra_invocation_log (request_id);
CREATE INDEX IF NOT EXISTS idx_invoke_channel ON axiqra_invocation_log (channel);
CREATE INDEX IF NOT EXISTS idx_invoke_target ON axiqra_invocation_log (target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_invoke_caller ON axiqra_invocation_log (caller_id);

-- ===================== Review Decision 审核决策日志 =====================
CREATE TABLE IF NOT EXISTS axiqra_review_decision_log (
    id BIGSERIAL PRIMARY KEY,
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    request_id VARCHAR(96) NOT NULL,
    review_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    decision VARCHAR(32) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    reason_code VARCHAR(64) NOT NULL,
    evidence_snapshot JSONB NULL,
    tenant_id BIGINT NULL
);

CREATE INDEX IF NOT EXISTS idx_review_request_id ON axiqra_review_decision_log (request_id);
CREATE INDEX IF NOT EXISTS idx_review_reviewer ON axiqra_review_decision_log (reviewer_id);

-- ===================== Quota Events 每日 Quota 事件 =====================
CREATE TABLE IF NOT EXISTS axiqra_quota_event (
    id BIGSERIAL PRIMARY KEY,
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    request_id VARCHAR(96) NOT NULL,
    subject_key VARCHAR(128) NOT NULL,
    quota_type VARCHAR(32) NOT NULL,
    amount INT NOT NULL,
    consumed INT NOT NULL DEFAULT 0,
    remaining INT NOT NULL,
    reset_at TIMESTAMPTZ NULL,
    result VARCHAR(32) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_quota_subject ON axiqra_quota_event (subject_key);
CREATE INDEX IF NOT EXISTS idx_quota_gmt_create ON axiqra_quota_event (gmt_create);

-- ===================== Rate Limit Events 每分钟风控事件 =====================
CREATE TABLE IF NOT EXISTS axiqra_rate_limit_event (
    id BIGSERIAL PRIMARY KEY,
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    request_id VARCHAR(96) NOT NULL,
    limiter_key VARCHAR(256) NOT NULL,
    window_key VARCHAR(64) NOT NULL,
    limit_value INT NOT NULL,
    current_count INT NOT NULL,
    result VARCHAR(32) NOT NULL,
    retry_after INT NULL
);

CREATE INDEX IF NOT EXISTS idx_rate_key ON axiqra_rate_limit_event (limiter_key);
CREATE INDEX IF NOT EXISTS idx_rate_gmt_create ON axiqra_rate_limit_event (gmt_create);

-- ===================== Authorization Audit 授权变更日志 =====================
CREATE TABLE IF NOT EXISTS axiqra_authorization_audit_log (
    id BIGSERIAL PRIMARY KEY,
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    request_id VARCHAR(96) NOT NULL,
    authorization_id BIGINT NOT NULL,
    from_state VARCHAR(32) NULL,
    to_state VARCHAR(32) NOT NULL,
    actor_id BIGINT NOT NULL,
    license_scope VARCHAR(32) NULL
);

CREATE INDEX IF NOT EXISTS idx_auth_audit_auth_id ON axiqra_authorization_audit_log (authorization_id);

-- ===================== Tool Model Attribution 工具模型归因审计日志 =====================
CREATE TABLE IF NOT EXISTS axiqra_tool_model_attribution_log (
    id BIGSERIAL PRIMARY KEY,
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    request_id VARCHAR(96) NOT NULL,
    solution_id BIGINT NOT NULL,
    tool_name VARCHAR(160) NOT NULL,
    reported_model_name VARCHAR(160) NOT NULL,
    source VARCHAR(32) NOT NULL,
    result VARCHAR(32) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_attr_log_request_id ON axiqra_tool_model_attribution_log (request_id);
CREATE INDEX IF NOT EXISTS idx_attr_log_solution ON axiqra_tool_model_attribution_log (solution_id);

-- ===================== RLS：所有审计表禁止 UPDATE 和 DELETE =====================
ALTER TABLE axiqra_audit_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE axiqra_audit_event FORCE ROW LEVEL SECURITY;
CREATE POLICY audit_allow_select ON axiqra_audit_event FOR SELECT USING (true);
CREATE POLICY audit_no_update ON axiqra_audit_event FOR UPDATE USING (false);
CREATE POLICY audit_no_delete ON axiqra_audit_event FOR DELETE USING (false);
CREATE POLICY audit_allow_insert ON axiqra_audit_event FOR INSERT WITH CHECK (true);

ALTER TABLE axiqra_policy_decision_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE axiqra_policy_decision_log FORCE ROW LEVEL SECURITY;
CREATE POLICY policy_allow_select ON axiqra_policy_decision_log FOR SELECT USING (true);
CREATE POLICY policy_no_update ON axiqra_policy_decision_log FOR UPDATE USING (false);
CREATE POLICY policy_no_delete ON axiqra_policy_decision_log FOR DELETE USING (false);
CREATE POLICY policy_allow_insert ON axiqra_policy_decision_log FOR INSERT WITH CHECK (true);

ALTER TABLE axiqra_invocation_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE axiqra_invocation_log FORCE ROW LEVEL SECURITY;
CREATE POLICY invoke_allow_select ON axiqra_invocation_log FOR SELECT USING (true);
CREATE POLICY invoke_no_update ON axiqra_invocation_log FOR UPDATE USING (false);
CREATE POLICY invoke_no_delete ON axiqra_invocation_log FOR DELETE USING (false);
CREATE POLICY invoke_allow_insert ON axiqra_invocation_log FOR INSERT WITH CHECK (true);

ALTER TABLE axiqra_review_decision_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE axiqra_review_decision_log FORCE ROW LEVEL SECURITY;
CREATE POLICY review_allow_select ON axiqra_review_decision_log FOR SELECT USING (true);
CREATE POLICY review_no_update ON axiqra_review_decision_log FOR UPDATE USING (false);
CREATE POLICY review_no_delete ON axiqra_review_decision_log FOR DELETE USING (false);
CREATE POLICY review_allow_insert ON axiqra_review_decision_log FOR INSERT WITH CHECK (true);

ALTER TABLE axiqra_quota_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE axiqra_quota_event FORCE ROW LEVEL SECURITY;
CREATE POLICY quota_allow_select ON axiqra_quota_event FOR SELECT USING (true);
CREATE POLICY quota_no_update ON axiqra_quota_event FOR UPDATE USING (false);
CREATE POLICY quota_no_delete ON axiqra_quota_event FOR DELETE USING (false);
CREATE POLICY quota_allow_insert ON axiqra_quota_event FOR INSERT WITH CHECK (true);

ALTER TABLE axiqra_rate_limit_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE axiqra_rate_limit_event FORCE ROW LEVEL SECURITY;
CREATE POLICY rate_allow_select ON axiqra_rate_limit_event FOR SELECT USING (true);
CREATE POLICY rate_no_update ON axiqra_rate_limit_event FOR UPDATE USING (false);
CREATE POLICY rate_no_delete ON axiqra_rate_limit_event FOR DELETE USING (false);
CREATE POLICY rate_allow_insert ON axiqra_rate_limit_event FOR INSERT WITH CHECK (true);

ALTER TABLE axiqra_authorization_audit_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE axiqra_authorization_audit_log FORCE ROW LEVEL SECURITY;
CREATE POLICY auth_allow_select ON axiqra_authorization_audit_log FOR SELECT USING (true);
CREATE POLICY auth_no_update ON axiqra_authorization_audit_log FOR UPDATE USING (false);
CREATE POLICY auth_no_delete ON axiqra_authorization_audit_log FOR DELETE USING (false);
CREATE POLICY auth_allow_insert ON axiqra_authorization_audit_log FOR INSERT WITH CHECK (true);

ALTER TABLE axiqra_tool_model_attribution_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE axiqra_tool_model_attribution_log FORCE ROW LEVEL SECURITY;
CREATE POLICY attr_allow_select ON axiqra_tool_model_attribution_log FOR SELECT USING (true);
CREATE POLICY attr_no_update ON axiqra_tool_model_attribution_log FOR UPDATE USING (false);
CREATE POLICY attr_no_delete ON axiqra_tool_model_attribution_log FOR DELETE USING (false);
CREATE POLICY attr_allow_insert ON axiqra_tool_model_attribution_log FOR INSERT WITH CHECK (true);

-- 授权给应用用户（axiqra_audit 由 docker-compose 通过 POSTGRES_USER 环境变量创建）
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO axiqra_audit;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO axiqra_audit;
