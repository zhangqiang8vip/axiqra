package com.axiqra.common.domain.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

/**
 * Trace 证据引用表 axiqra_trace_evidence_ref
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_trace_evidence_ref")
public class TraceEvidenceRefEntity extends BaseEntity {

    private Long traceId;
    private String uri;
    @Nullable
    private String hash;
    private String type;
    @Nullable
    private Long sizeBytes;
}
