package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * Invocation 结果类型枚举
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Getter
public enum InvocationResult {

    WORKED("worked", "完全成功"),
    PARTIAL("partial", "部分成功"),
    FAILED("failed", "失败"),
    NOT_APPLICABLE("not_applicable", "不适用");

    @EnumValue
    private final String code;
    private final String desc;

    InvocationResult(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static InvocationResult of(String code) {
        if (code == null) return null;
        for (InvocationResult r : values()) {
            if (r.code.equals(code)) return r;
        }
        return null;
    }
}
