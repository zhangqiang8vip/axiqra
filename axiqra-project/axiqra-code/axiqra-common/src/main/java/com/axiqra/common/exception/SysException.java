package com.axiqra.common.exception;

/**
 * 系统异常
 * <p>
 * 用于数据库故障、网络超时、文件不存在等系统级错误。
 * 系统异常会被全局异常处理器记录完整堆栈。
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
public class SysException extends RuntimeException {

    private final int code;

    public SysException(String message) {
        super(message);
        this.code = 50000;
    }

    public SysException(int code, String message) {
        super(message);
        this.code = code;
    }

    public SysException(String message, Throwable cause) {
        super(message, cause);
        this.code = 50000;
    }

    public SysException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
