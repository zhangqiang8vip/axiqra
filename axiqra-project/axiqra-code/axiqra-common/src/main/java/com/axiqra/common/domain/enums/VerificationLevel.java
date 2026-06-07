package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * Solution 验证等级枚举（L0~L5）
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Getter
public enum VerificationLevel {

    L0(0, "未验证", "仅有理论依据"),
    L1(1, "个人验证", "个人测试通过"),
    L2(2, "团队验证", "团队审查通过"),
    L3(3, "公开验证", "多人反馈正向"),
    L4(4, "生产验证", "生产环境验证"),
    L5(5, "权威验证", "行业专家认证");

    @EnumValue
    private final int level;
    private final String desc;
    private final String meaning;

    VerificationLevel(int level, String desc, String meaning) {
        this.level = level;
        this.desc = desc;
        this.meaning = meaning;
    }

    public static VerificationLevel of(int level) {
        for (VerificationLevel v : values()) {
            if (v.level == level) return v;
        }
        return null;
    }

    /** 是否高于给定等级 */
    public boolean isHigherThan(int otherLevel) {
        return this.level > otherLevel;
    }
}
