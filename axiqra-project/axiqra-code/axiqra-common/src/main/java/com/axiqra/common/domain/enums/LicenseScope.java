package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * 授权范围枚举（license_scope 字段）
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Getter
public enum LicenseScope {

    PROPRIETARY("proprietary", "专有"),
    OPEN_SOURCE("open_source", "开源"),
    CREATIVE_COMMONS("creative_commons", "知识共享"),
    PUBLIC_DOMAIN("public_domain", "公共领域");

    @EnumValue
    private final String code;
    private final String desc;

    LicenseScope(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static LicenseScope of(String code) {
        if (code == null) return null;
        for (LicenseScope l : values()) {
            if (l.code.equals(code)) return l;
        }
        return null;
    }

    /** 是否允许公开发布 */
    public boolean allowsPublicRelease() {
        return this == OPEN_SOURCE || this == CREATIVE_COMMONS || this == PUBLIC_DOMAIN;
    }
}
