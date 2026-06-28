package com.axiqra.common.domain.entity;

import com.axiqra.common.domain.enums.LicenseScope;
import com.axiqra.common.domain.enums.SolutionStatus;
import com.axiqra.common.domain.enums.VerificationLevel;
import com.axiqra.common.domain.enums.VisibilityScope;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

/**
 * Solution 表 axiqra_solution
 * 
 * 对应 D06/D12 文档规定的 Solution 字段
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_solution")
public class SolutionEntity extends BaseEntity {

    private Long authorId;
    private Long workspaceId;
    @Nullable
    private Long projectId;
    private String solutionCode;
    private String title;
    @Nullable
    private String domain;
    @Nullable
    private String techStack;
    private VerificationLevel verificationLevel;
    private Integer riskLevel;
    private SolutionStatus status;
    private VisibilityScope visibilityScope;
    @Nullable
    private LicenseScope licenseScope;
    @Nullable
    @Column("tenant_id")
    private Long tenantId;
    @Nullable
    private Long sourceCaseId;
    @Column("is_deleted")
    private boolean isDeleted = false;
    
    // ========== D06 §15 / D12 §3/§11 规定的补充字段 ==========
    
    /** 错误签名，用于精确匹配报错信息 */
    @Nullable
    @Column("error_signature")
    private String errorSignature;
    
    /** 运行环境 (OS、运行时、数据库、云环境等) */
    @Nullable
    @Column("environment")
    private String environment;
    
    /** 问题类型: build / deploy / performance / security / permission 等 */
    @Nullable
    @Column("problem_type")
    private String problemType;
    
    /** 证据数量 */
    @Nullable
    @Column("evidence_count")
    private Integer evidenceCount;
    
    /** 失败路径描述 */
    @Nullable
    @Column("failure_paths")
    private String failurePaths;
    
    /** 适用条件描述 */
    @Nullable
    @Column("applicability")
    private String applicability;
    
    /** 不适用边界描述 */
    @Nullable
    @Column("inapplicability")
    private String inapplicability;

    // ========== D08 §15 Solution 演化关系字段 ==========

    /** 父 Solution ID：merge / fork / split 操作的来源 Solution */
    @Nullable
    @Column("parent_solution_id")
    private Long parentSolutionId;

    /** 继承 Solution ID：本 Solution 被 merge 到 / 被 superseded 到 */
    @Nullable
    @Column("successor_solution_id")
    private Long successorSolutionId;

    /** Fork 来源 Solution ID：本 Solution 是从哪个 Solution fork 出来的 */
    @Nullable
    @Column("fork_of_solution_id")
    private Long forkOfSolutionId;

    /** 演化类型：merge / fork / supersede / split / rollback / null */
    @Nullable
    @Column("evolution_kind")
    private String evolutionKind;
}
