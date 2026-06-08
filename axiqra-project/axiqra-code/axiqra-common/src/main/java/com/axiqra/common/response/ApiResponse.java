package com.axiqra.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 响应结构
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 响应码：0=成功，非0=失败 */
    private int code;

    /** 响应消息 */
    private String message;

    /** 响应数据 */
    private T data;

    /**
     * 请求追踪 ID（用于日志关联、审计记录、问题排查）。
     * 与 traceId 字段值相同，本字段为文档要求的规范命名。
     */
    private String requestId;

    /** @deprecated 请使用 requestId，本字段保留用于兼容 */
    @Deprecated
    private String traceId;

    /** 时间戳（ISO 8601） */
    private String timestamp;

    private ApiResponse() {
    }

    private ApiResponse(int code, String message, T data, String requestId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.requestId = requestId;
        this.traceId = requestId;
        this.timestamp = java.time.OffsetDateTime.now().toString();
    }

    /**
     * 设置 requestId。
     * TraceIdFilter 在请求入口生成 traceId 并放入 MDC，
     * TraceIdResponseAdvice 在响应前从 MDC 取出并调用本方法注入到响应体。
     * 成功响应需要包含 requestId 以满足审计要求。
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
        this.traceId = requestId;
    }

    // ===================== 成功响应 =====================

    public static <T> ApiResponse<T> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok("操作成功", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(0, message, data, null);
    }

    // ===================== 失败响应 =====================

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null, null);
    }

    public static <T> ApiResponse<T> fail(int code, String message, String requestId) {
        return new ApiResponse<>(code, message, null, requestId);
    }
}
