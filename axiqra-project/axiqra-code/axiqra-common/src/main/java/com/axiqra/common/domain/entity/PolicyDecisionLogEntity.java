package com.axiqra.common.domain.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 策略决策日志表 axiqra_policy_decision_log
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_policy_decision_log")
public class PolicyDecisionLogEntity extends BaseEntity {

    private String requestId;
    private String subjectJson;
    private String objectJson;
    private String action;
    private String decision;
    private String policyCode;
    private String policyVersion;
    private String reasonCode;
    private Long tenantId;
}
