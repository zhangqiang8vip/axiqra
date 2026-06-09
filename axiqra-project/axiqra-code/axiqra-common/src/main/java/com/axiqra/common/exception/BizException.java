package com.axiqra.common.exception;

/**
 * 业务异常
 *
 * <p>用于业务层校验失败、无权限、资源不存在等业务错误。
 * 使用 ErrorCode 中的错误码。
 *
 * <p>每个异常可携带语义化的 HTTP 状态码。如未指定 {@code httpStatus}，由
 * {@link GlobalExceptionHandler#handleBizException} 默认返回 409 CONFLICT。
 * 如需覆盖，由 ErrorCode 中的 {@code httpStatus} 决定。
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final Integer httpStatus;
    private final Integer retryAfterSeconds;

    public BizException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage(), null, null);
    }

    public BizException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, null);
    }

    public BizException(ErrorCode errorCode, String message, Integer retryAfterSeconds) {
        this(errorCode, message, null, retryAfterSeconds);
    }

    public BizException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, cause, null);
    }

    public BizException(ErrorCode errorCode, String message, Throwable cause, Integer retryAfterSeconds) {
        super(message, cause);
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public int getCode() {
        return code;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
