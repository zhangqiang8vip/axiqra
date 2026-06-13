package com.axiqra.common.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "Invocation ID 不能为空")
    private Long invocationId;

    @NotBlank(message = "反馈类型不能为空")
    private String feedbackType;

    @Size(max = 5000, message = "反馈内容不能超过 5000 字")
    private String feedbackContent;

    private List<String> evidenceRefs;

    @Size(max = 2000, message = "上下文说明不能超过 2000 字")
    private String contextDelta;

    @Size(max = 1000, message = "边界说明不能超过 1000 字")
    private String boundaryNotes;
}
