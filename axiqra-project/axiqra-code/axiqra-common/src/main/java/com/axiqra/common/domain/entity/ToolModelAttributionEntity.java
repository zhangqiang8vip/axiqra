package com.axiqra.common.domain.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

import java.time.Instant;

/**
 * 工具模型归因表 axiqra_tool_model_attribution
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_tool_model_attribution")
public class ToolModelAttributionEntity extends BaseEntity {

    private Long solutionId;
    @Nullable
    private Long solutionVersionId;
    private String attributionSource;
    private String toolType;
    private String toolName;
    @Nullable
    private String toolVendor;
    private String toolVersion;
    private String clientChannel;
    private String reportedModelProvider;
    private String reportedModelName;
    @Nullable
    private String reportedModelVersion;
    private String reportedModelSource;
    private String reportedModelConfidence;
    @Nullable
    private Instant modelReportedAt;
    @Nullable
    private String missingReason;
    private String requestId;
}
