package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * Review 审核队列枚举
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Getter
public enum ReviewQueue {

    HUMAN("human", "人工审核队列"),
    AUTO("auto", "自动审核队列"),
    QUARANTINED("quarantined", "隔离审核队列");

    @EnumValue
    private final String code;
    private final String desc;

    ReviewQueue(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ReviewQueue of(String code) {
        if (code == null) return null;
        for (ReviewQueue q : values()) {
            if (q.code.equals(code)) return q;
        }
        return null;
    }
}
