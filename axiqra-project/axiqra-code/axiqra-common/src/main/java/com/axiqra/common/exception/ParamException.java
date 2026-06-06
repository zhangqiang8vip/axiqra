package com.axiqra.common.exception;

/**
 * 参数校验异常
 * <p>
 * 用于请求参数校验失败（@Validated 注解触发的 ConstraintViolationException）。
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
public class ParamException extends RuntimeException {

    private final int code;

    public ParamException(String message) {
        super(message);
        this.code = 40000;
    }

    public ParamException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
