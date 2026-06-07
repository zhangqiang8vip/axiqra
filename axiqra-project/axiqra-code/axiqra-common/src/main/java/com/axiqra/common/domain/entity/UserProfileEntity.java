package com.axiqra.common.domain.entity;

import com.mybatisflex.annotation.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 用户配置文件表 axiqra_user_profile（S1 MVP 预留，扩展 user 表字段）
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_user_profile")
public class UserProfileEntity extends BaseEntity {

    @NotNull
    private Long userId;
    private String avatarUrl;
    private String bio;
    private String timezone;
    private String language;
    private String defaultWorkspaceType;
}
