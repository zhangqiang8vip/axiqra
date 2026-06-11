package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Trace 证据引用视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraceEvidenceVO {

    private Long id;
    private String uri;
    private String hash;
    private String type;
    private Long sizeBytes;
}
