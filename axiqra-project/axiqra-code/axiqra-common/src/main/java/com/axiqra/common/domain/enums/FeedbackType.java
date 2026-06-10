package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * Feedback 反馈类型枚举
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Getter
public enum FeedbackType {

    WORKED("worked", "方案有效"),
    PARTIAL("partial", "部分有效"),
    FAILED("failed", "方案无效"),
    NOT_APPLICABLE("not_applicable", "不适用");

    @EnumValue
    private final String code;
    private final String desc;

    FeedbackType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static FeedbackType of(String code) {
        if (code == null) return null;
        for (FeedbackType f : values()) {
            if (f.code.equals(code)) return f;
        }
        return null;
    }
}
