package com.axiqra.common.domain.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TraceCreateRequest 校验测试")
class TraceCreateRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("evidences 为空时应校验失败")
    void shouldRejectMissingEvidences() {
        TraceCreateRequest request = validRequestBuilder()
                .evidences(null)
                .build();

        Set<ConstraintViolation<TraceCreateRequest>> violations = validator.validate(request);

        assertTrue(hasViolation(violations, "evidences"));
    }

    @Test
    @DisplayName("evidence sizeBytes 为空时应校验失败")
    void shouldRejectNullEvidenceSizeBytes() {
        TraceCreateRequest request = validRequestBuilder()
                .evidences(List.of(TraceCreateRequest.TraceEvidenceItem.builder()
                        .uri("minio://bucket/e1")
                        .hash("abc")
                        .type("artifact")
                        .sizeBytes(null)
                        .build()))
                .build();

        Set<ConstraintViolation<TraceCreateRequest>> violations = validator.validate(request);

        assertTrue(hasViolation(violations, "evidences[0].sizeBytes"));
    }

    @Test
    @DisplayName("完整请求应通过校验")
    void shouldAcceptValidRequest() {
        TraceCreateRequest request = validRequestBuilder().build();

        Set<ConstraintViolation<TraceCreateRequest>> violations = validator.validate(request);

        assertFalse(hasViolation(violations, "evidences"));
        assertFalse(hasViolation(violations, "evidences[0].sizeBytes"));
    }

    private TraceCreateRequest.TraceCreateRequestBuilder validRequestBuilder() {
        return TraceCreateRequest.builder()
                .workspaceId(100L)
                .taskGoal("补齐 trace")
                .outcome("完成")
                .riskLevel("R2")
                .evidences(List.of(TraceCreateRequest.TraceEvidenceItem.builder()
                        .uri("minio://bucket/e1")
                        .hash("abc")
                        .type("artifact")
                        .sizeBytes(12L)
                        .build()));
    }

    private boolean hasViolation(Set<? extends ConstraintViolation<?>> violations, String propertyPath) {
        return violations.stream().anyMatch(v -> propertyPath.equals(v.getPropertyPath().toString()));
    }
}
