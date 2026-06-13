package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Feedback 详情 VO
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackDetailVO {

    private Long id;

    private Long invocationId;

    private Long userId;

    private String feedbackType;

    private String feedbackTypeDesc;

    private String feedbackContent;

    private List<String> evidenceRefs;

    private String contextDelta;

    private String boundaryNotes;

    private String status;

    private Instant gmtCreate;

    private Instant gmtModified;
}
