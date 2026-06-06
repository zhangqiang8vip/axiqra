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

    /** traceId（用于问题追踪） */
    private String traceId;

    /** 时间戳（ISO 8601） */
    private String timestamp;

    private ApiResponse() {
    }

    private ApiResponse(int code, String message, T data, String traceId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = traceId;
        this.timestamp = java.time.OffsetDateTime.now().toString();
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

    public static <T> ApiResponse<T> fail(int code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, traceId);
    }
}
