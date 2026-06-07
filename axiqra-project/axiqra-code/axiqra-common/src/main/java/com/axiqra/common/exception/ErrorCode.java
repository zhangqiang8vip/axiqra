package com.axiqra.common.exception;

import lombok.Getter;

/**
 * 统一错误码（5 位数字）
 *
 * <p>格式：XYYYY
 * X = 错误大类（1~9）
 * YYYY = 错误序号（0001~9999）
 *
 * <p>大类默认 HTTP 状态映射：
 * 0 - 成功（无错误码）                       → HTTP 200
 * 1 - 参数错误（10001~19999）               → HTTP 400
 * 2 - 认证授权错误（20001~29999）            → HTTP 401/403
 * 3 - 资源不存在（30001~39999）              → HTTP 404
 * 4 - 业务逻辑错误（40001~49999）            → HTTP 409（默认；个别条目可覆盖，如 40004/40005/40006 返回 400）
 * 5 - 系统错误（50001~59999）                → HTTP 500（默认；个别条目可覆盖，如 50008 返回 429，50010 返回 413）
 * 6 - 配额/限流错误（60001~69999）            → HTTP 429（默认；个别条目可覆盖，如 60002 QUOTA_WARNING 返回 200）
 * 7 - 治理/审核错误（70001~79999）            → HTTP 403
 * 8 - 搜索/Solution 错误（80001~89999）       → HTTP 400（默认；个别条目可覆盖，如 80001 SEARCH_RESULT_EMPTY 返回 200）
 * 9 - Trace/Case 错误（90001~99999）          → HTTP 400
 *
 * <p>每个枚举条目可通过 {@code httpStatus} 字段覆盖上述默认映射。
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Getter
public enum ErrorCode {

    // ==================== 成功 ====================
    SUCCESS(0, "操作成功", 200),

    // ==================== 参数错误（1xxxx） ====================
    PARAM_INVALID(10001, "参数无效", 400),
    PARAM_MISSING(10002, "缺少必需参数", 400),
    PARAM_TYPE_MISMATCH(10003, "参数类型不匹配", 400),
    PARAM_VALIDATION_FAILED(10004, "参数校验失败", 400),

    // ==================== 认证授权错误（2xxxx） ====================
    UNAUTHORIZED(20001, "未登录或登录已过期", 401),
    TOKEN_INVALID(20002, "Token 无效", 401),
    TOKEN_EXPIRED(20003, "Token 已过期", 401),
    FORBIDDEN(20004, "无权访问该资源", 403),
    PERMISSION_DENIED(20005, "权限不足", 403),
    API_SIGNATURE_INVALID(20006, "API 签名无效", 401),
    API_SIGNATURE_EXPIRED(20007, "API 签名已过期", 401),
    API_NONCE_REUSED(20008, "Nonce 已使用（重放攻击）", 401),

    // ==================== 资源不存在（3xxxx） ====================
    RESOURCE_NOT_FOUND(30001, "资源不存在", 404),
    USER_NOT_FOUND(30002, "用户不存在", 404),
    WORKSPACE_NOT_FOUND(30003, "工作空间不存在", 404),
    SOLUTION_NOT_FOUND(30004, "Solution 不存在", 404),
    TRACE_NOT_FOUND(30005, "Trace 不存在", 404),
    PROJECT_CASE_NOT_FOUND(30006, "Project Case 不存在", 404),
    PUBLIC_CASE_NOT_FOUND(30007, "Public Case 不存在", 404),
    INVOCATION_NOT_FOUND(30008, "Invocation 不存在", 404),
    CONNECT_SESSION_NOT_FOUND(30009, "接入会话不存在", 404),

    // ==================== 业务逻辑错误（4xxxx） ====================
    DUPLICATE_ENTRY(40001, "记录已存在", 409),
    IDEMPOTENCY_KEY_CONFLICT(40002, "幂等键冲突，请勿重复提交", 409),
    STATUS_TRANSITION_INVALID(40003, "状态转换无效", 409),
    WORKSPACE_TYPE_MISMATCH(40004, "工作空间类型不匹配", 400),
    LICENSE_SCOPE_MISSING(40005, "授权范围缺失，无法发布", 400),
    REDACTION_REQUIRED(40006, "脱敏未完成，无法发布", 400),
    USER_CONFIRMATION_REQUIRED(40007, "需要用户确认", 409),
    INVOCATION_ALREADY_EXISTS(40008, "Invocation 记录已存在", 409),
    AUTHORIZATION_REVOKED(40009, "授权已撤回", 409),

    // ==================== 系统错误（5xxxx） ====================
    SYSTEM_ERROR(50001, "系统内部错误", 500),
    DATABASE_ERROR(50002, "数据库错误", 500),
    CACHE_ERROR(50003, "缓存错误", 500),
    FILE_STORAGE_ERROR(50004, "文件存储错误", 500),
    NETWORK_ERROR(50005, "网络错误", 502),
    TIMEOUT_ERROR(50006, "请求超时", 504),
    SERVICE_UNAVAILABLE(50007, "服务暂不可用", 503),
    RATE_LIMIT_EXCEEDED(50008, "请求过于频繁", 429),
    CIRCUIT_BREAKER_OPEN(50009, "服务熔断中，请稍后重试", 503),
    REQUEST_ENTITY_TOO_LARGE(50010, "请求体超限，拒绝处理", 413),

    // ==================== 配额/限流错误（6xxxx） ====================
    QUOTA_EXCEEDED(60001, "每日搜索配额已用尽", 429),
    QUOTA_WARNING(60002, "每日搜索配额即将用尽", 200),
    RATE_LIMITED(60003, "请求被限流，请稍后重试", 429),
    RATE_LIMITED_SLOWDOWN(60004, "请求过于频繁，请降低频率", 429),

    // ==================== 治理/审核错误（7xxxx） ====================
    REVIEW_REQUIRED(70001, "内容需要审核", 403),
    CONTENT_QUARANTINED(70002, "内容已被隔离", 403),
    CONTENT_REJECTED(70003, "内容审核未通过", 403),
    REASON_CODE_MISSING(70004, "审核原因码缺失", 400),
    RISK_LEVEL_TOO_HIGH(70005, "风险等级过高，禁止自动执行", 403),
    APPEAL_IN_PROGRESS(70006, "申诉处理中", 202),
    APPEAL_REJECTED(70007, "申诉被驳回", 403),

    // ==================== 搜索/Solution 错误（8xxxx） ====================
    SEARCH_RESULT_EMPTY(80001, "未找到匹配结果", 200),
    SEARCH_NO_PERMISSION(80002, "无搜索权限", 403),
    SOLUTION_NOT_VERIFIED(80003, "Solution 尚未通过验证", 400),
    SOLUTION_QUARANTINED(80004, "Solution 已被隔离", 403),
    SOLUTION_DEPRECATED(80005, "Solution 已废弃", 410),
    VERIFICATION_LEVEL_TOO_LOW(80006, "验证等级不足，无法执行", 403),

    // ==================== Trace/Case 错误（9xxxx） ====================
    TRACE_SUBMIT_FAILED(90001, "Trace 提交失败", 400),
    TRACE_USER_CONFIRMATION_PENDING(90002, "等待用户确认", 202),
    TRACE_EVIDENCE_MISSING(90003, "Trace 证据缺失", 400),
    CASE_PUBLISH_PENDING(90004, "Case 发布申请待审核", 202),
    CASE_SOURCE_MISSING(90005, "Case 源记录缺失", 400),
    CASE_DUPLICATE_SOURCE(90006, "Case 来源重复", 409),
    CASE_REDACTION_INCOMPLETE(90007, "Case 脱敏不完整", 400),

    // ==================== 未知错误 ====================
    UNKNOWN_ERROR(99999, "未知错误", 500);

    private final int code;
    private final String message;
    private final int httpStatus;

    ErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
