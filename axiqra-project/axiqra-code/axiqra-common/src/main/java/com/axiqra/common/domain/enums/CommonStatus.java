package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * 通用状态枚举（用于 is_deleted / active/inactive 等三态状态）
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Getter
public enum CommonStatus {

    ACTIVE(0, "有效"),
    INACTIVE(1, "无效"),
    DELETED(2, "已删除");

    @EnumValue
    private final int code;
    private final String desc;

    CommonStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CommonStatus of(Integer code) {
        if (code == null) return null;
        for (CommonStatus s : values()) {
            if (s.code == code) return s;
        }
        return null;
    }
}
