package com.axiqra.common.domain.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

/**
 * Contribution Ledger 贡献账本表 axiqra_contribution_ledger
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_contribution_ledger")
public class ContributionLedgerEntity extends BaseEntity {

    private Long actorId;
    private String eventType;
    private String objectType;
    private Long objectId;
    private Integer points;
    @Nullable
    private String evidenceRefs;
    private String status;
    @Column("is_deleted")
    private boolean isDeleted = false;
    @Nullable
    @Column("tenant_id")
    private Long tenantId;
}
