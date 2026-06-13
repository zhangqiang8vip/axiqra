package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * 成员状态枚举
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Getter
public enum MemberStatus {

    ACTIVE("active", "正常"),
    SUSPENDED("suspended", "已停用"),
    PENDING("pending", "待确认");

    @EnumValue
    private final String code;
    private final String desc;

    MemberStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MemberStatus of(String code) {
        if (code == null) {
            return null;
        }
        for (MemberStatus s : values()) {
            if (s.code.equals(code.toLowerCase())) {
                return s;
            }
        }
        return null;
    }
}
