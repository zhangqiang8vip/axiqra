package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * Engineering Trace 索引状态枚举
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Getter
public enum IndexStatus {

    NOT_INDEXED("not_indexed", "未索引"),
    INDEXING("indexing", "索引中"),
    INDEXED("indexed", "已索引"),
    INDEX_FAILED("index_failed", "索引失败");

    @EnumValue
    private final String code;
    private final String desc;

    IndexStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static IndexStatus of(String code) {
        if (code == null) return null;
        for (IndexStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
