package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 贡献记录 VO
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributionRecordVO {

    /** 贡献记录 ID */
    private String contributionId;

    /** 用户 ID */
    private Long userId;

    /** 用户名（显示用） */
    private String username;

    /** 贡献类型 */
    private String contributionType;

    /** 目标类型 */
    private String targetType;

    /** 目标 ID */
    private Long targetId;

    /** 贡献积分 */
    private int points;

    /** 贡献描述 */
    private String description;

    /** 贡献数量 */
    private int contributionCount;

    /** 贡献时间 */
    private Instant contributedAt;

    /** 相关联的内容标题 */
    private String targetTitle;
}
