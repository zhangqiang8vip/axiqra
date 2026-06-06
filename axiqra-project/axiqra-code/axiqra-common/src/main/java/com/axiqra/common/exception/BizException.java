package com.axiqra.common.exception;

/**
 * 业务异常
 * <p>
 * 用于业务层校验失败、无权限、资源不存在等业务错误。
 * 使用 ErrorCode 中的错误码。
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code) {
        super();
        this.code = code;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
