package com.axiqra.common.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Feedback 反馈提交请求
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Data
@NoArgsConstructor
public class FeedbackSubmitRequest {

    @JsonAlias("invocation_id")
    private Long invocationId;

    @JsonAlias({"invocationCode", "invocation_code"})
    private String invocationCode;

    @NotBlank(message = "反馈类型不能为空")
    @JsonAlias({"feedback_type", "resultType", "result_type"})
    private String feedbackType;

    @Size(max = 5000, message = "反馈内容不能超过 5000 字")
    @JsonAlias({"feedback_content", "content"})
    private String feedbackContent;

    @JsonAlias("evidence_refs")
    private List<String> evidenceRefs;

    @Size(max = 2000, message = "上下文说明不能超过 2000 字")
    @JsonAlias("context_delta")
    private String contextDelta;

    @Size(max = 1000, message = "边界说明不能超过 1000 字")
    @JsonAlias("boundary_notes")
    private String boundaryNotes;

    /**
     * 幂等键，用于防止重复提交
     * 客户端生成，建议格式: feedback_{timestamp}_{hash}
     */
    @JsonAlias("idempotency_key")
    private String idempotencyKey;
}
