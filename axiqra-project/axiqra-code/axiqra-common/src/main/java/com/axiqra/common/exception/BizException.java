package com.axiqra.common.exception;

/**
 * 业务异常
 *
 * <p>用于业务层校验失败、无权限、资源不存在等业务错误。
 * 使用 ErrorCode 中的错误码。
 *
 * <p>每个异常可携带语义化的 HTTP 状态码，默认返回 409 CONFLICT。
 * 如需覆盖，由 ErrorCode 中的 httpStatus 决定。
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final Integer httpStatus;

    public BizException(int code) {
        super();
        this.code = code;
        this.httpStatus = null;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = null;
    }

    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = null;
    }

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }

    public BizException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public int getCode() {
        return code;
    }
}
