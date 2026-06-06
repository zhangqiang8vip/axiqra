package com.axiqra.common.exception;

import lombok.Getter;

/**
 * 统一错误码（5 位数字）
 * <p>
 * 格式：XYYYY
 * X = 错误大类（1~9）
 * YYYY = 错误序号（0001~9999）
 * <p>
 * 大类分配：
 * 0 - 成功（无错误码）
 * 1 - 参数错误（40001~19999）
 * 2 - 认证授权错误（20001~29999）
 * 3 - 资源不存在（30001~39999）
 * 4 - 业务逻辑错误（40001~49999）
 * 5 - 系统错误（50001~59999）
 * 6 - 配额/限流错误（60001~69999）
 * 7 - 治理/审核错误（70001~79999）
 * 8 - 搜索/Solution 错误（80001~89999）
 * 9 - Trace/Case 错误（90001~99999）
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Getter
public enum ErrorCode {

    // ==================== 成功 ====================
    SUCCESS(0, "操作成功"),

    // ==================== 参数错误（1xxxx） ====================
    PARAM_INVALID(10001, "参数无效"),
    PARAM_MISSING(10002, "缺少必需参数"),
    PARAM_TYPE_MISMATCH(10003, "参数类型不匹配"),
    PARAM_VALIDATION_FAILED(10004, "参数校验失败"),

    // ==================== 认证授权错误（2xxxx） ====================
    UNAUTHORIZED(20001, "未登录或登录已过期"),
    TOKEN_INVALID(20002, "Token 无效"),
    TOKEN_EXPIRED(20003, "Token 已过期"),
    FORBIDDEN(20004, "无权访问该资源"),
    PERMISSION_DENIED(20005, "权限不足"),
    API_SIGNATURE_INVALID(20006, "API 签名无效"),
    API_SIGNATURE_EXPIRED(20007, "API 签名已过期"),
    API_NONCE_REUSED(20008, "Nonce 已使用（重放攻击）"),

    // ==================== 资源不存在（3xxxx） ====================
    RESOURCE_NOT_FOUND(30001, "资源不存在"),
    USER_NOT_FOUND(30002, "用户不存在"),
    WORKSPACE_NOT_FOUND(30003, "工作空间不存在"),
    SOLUTION_NOT_FOUND(30004, "Solution 不存在"),
    TRACE_NOT_FOUND(30005, "Trace 不存在"),
    PROJECT_CASE_NOT_FOUND(30006, "Project Case 不存在"),
    PUBLIC_CASE_NOT_FOUND(30007, "Public Case 不存在"),
    INVOCATION_NOT_FOUND(30008, "Invocation 不存在"),
    CONNECT_SESSION_NOT_FOUND(30009, "接入会话不存在"),

    // ==================== 业务逻辑错误（4xxxx） ====================
    DUPLICATE_ENTRY(40001, "记录已存在"),
    IDEMPOTENCY_KEY_CONFLICT(40002, "幂等键冲突，请勿重复提交"),
    STATUS_TRANSITION_INVALID(40003, "状态转换无效"),
    WORKSPACE_TYPE_MISMATCH(40004, "工作空间类型不匹配"),
    LICENSE_SCOPE_MISSING(40005, "授权范围缺失，无法发布"),
    REDACTION_REQUIRED(40006, "脱敏未完成，无法发布"),
    USER_CONFIRMATION_REQUIRED(40007, "需要用户确认"),
    INVOCATION_ALREADY_EXISTS(40008, "Invocation 记录已存在"),
    AUTHORIZATION_REVOKED(40009, "授权已撤回"),

    // ==================== 系统错误（5xxxx） ====================
    SYSTEM_ERROR(50001, "系统内部错误"),
    DATABASE_ERROR(50002, "数据库错误"),
    CACHE_ERROR(50003, "缓存错误"),
    FILE_STORAGE_ERROR(50004, "文件存储错误"),
    NETWORK_ERROR(50005, "网络错误"),
    TIMEOUT_ERROR(50006, "请求超时"),
    SERVICE_UNAVAILABLE(50007, "服务暂不可用"),
    RATE_LIMIT_EXCEEDED(50008, "请求过于频繁"),
    CIRCUIT_BREAKER_OPEN(50009, "服务熔断中，请稍后重试"),

    // ==================== 配额/限流错误（6xxxx） ====================
    QUOTA_EXCEEDED(60001, "每日搜索配额已用尽"),
    QUOTA_WARNING(60002, "每日搜索配额即将用尽"),
    RATE_LIMITED(60003, "请求被限流，请稍后重试"),
    RATE_LIMITED_SLOWDOWN(60004, "请求过于频繁，请降低频率"),

    // ==================== 治理/审核错误（7xxxx） ====================
    REVIEW_REQUIRED(70001, "内容需要审核"),
    CONTENT_QUARANTINED(70002, "内容已被隔离"),
    CONTENT_REJECTED(70003, "内容审核未通过"),
    REASON_CODE_MISSING(70004, "审核原因码缺失"),
    RISK_LEVEL_TOO_HIGH(70005, "风险等级过高，禁止自动执行"),
    APPEAL_IN_PROGRESS(70006, "申诉处理中"),
    APPEAL_REJECTED(70007, "申诉被驳回"),

    // ==================== 搜索/Solution 错误（8xxxx） ====================
    SEARCH_RESULT_EMPTY(80001, "未找到匹配结果"),
    SEARCH_NO_PERMISSION(80002, "无搜索权限"),
    SOLUTION_NOT_VERIFIED(80003, "Solution 尚未通过验证"),
    SOLUTION_QUARANTINED(80004, "Solution 已被隔离"),
    SOLUTION_DEPRECATED(80005, "Solution 已废弃"),
    VERIFICATION_LEVEL_TOO_LOW(80006, "验证等级不足，无法执行"),

    // ==================== Trace/Case 错误（9xxxx） ====================
    TRACE_SUBMIT_FAILED(90001, "Trace 提交失败"),
    TRACE_USER_CONFIRMATION_PENDING(90002, "等待用户确认"),
    TRACE_EVIDENCE_MISSING(90003, "Trace 证据缺失"),
    CASE_PUBLISH_PENDING(90004, "Case 发布申请待审核"),
    CASE_SOURCE_MISSING(90005, "Case 源记录缺失"),
    CASE_DUPLICATE_SOURCE(90006, "Case 来源重复"),
    CASE_REDACTION_INCOMPLETE(90007, "Case 脱敏不完整"),

    // ==================== 未知错误 ====================
    UNKNOWN_ERROR(99999, "未知错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
