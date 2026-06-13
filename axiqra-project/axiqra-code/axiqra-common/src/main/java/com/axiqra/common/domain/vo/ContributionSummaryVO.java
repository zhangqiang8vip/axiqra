package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 贡献汇总 VO
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributionSummaryVO {

    private Long actorId;

    private Integer totalPoints;

    private Integer traceCount;

    private Integer solutionCount;

    private Integer caseCount;

    private List<ContributionRecord> recentRecords;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContributionRecord {

        private Long id;

        private String eventType;

        private String objectType;

        private Long objectId;

        private Integer points;

        private String status;

        private String evidenceRefs;

        private Instant gmtCreate;
    }
}
