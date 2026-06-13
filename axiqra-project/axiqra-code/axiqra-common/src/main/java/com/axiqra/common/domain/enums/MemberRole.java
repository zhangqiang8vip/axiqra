package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

import java.util.Locale;

/**
 * 成员角色枚举
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Getter
public enum MemberRole {

    OWNER("owner", "所有者"),
    ADMIN("admin", "管理员"),
    MEMBER("member", "成员"),
    VIEWER("viewer", "访客");

    @EnumValue
    private final String code;
    private final String desc;

    MemberRole(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MemberRole of(String code) {
        if (code == null) return null;
        for (MemberRole r : values()) {
            if (r.code.equals(code.toLowerCase(Locale.ROOT))) return r;
        }
        return null;
    }

    /** 是否有管理权限（owner 或 admin 可管理成员） */
    public boolean hasAdminPrivilege() {
        return this == OWNER || this == ADMIN;
    }
}
