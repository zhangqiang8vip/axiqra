package com.axiqra.core.service.impl;

import com.axiqra.common.domain.entity.ReviewEntity;
import com.axiqra.common.domain.enums.ReviewQueue;
import com.axiqra.common.domain.enums.ReviewResult;
import com.axiqra.common.domain.vo.ReviewDetailVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.ReviewMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewServiceImpl 单元测试")
class ReviewServiceImplTest {

    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Test
    @DisplayName("getPendingReviews 应返回待审列表")
    void shouldReturnPendingReviews() {
        ReviewEntity entity = reviewEntity(1L, ReviewResult.PENDING);
        when(reviewMapper.selectPendingByQueue("human", "pending", 20))
                .thenReturn(List.of(entity));

        List<ReviewDetailVO> result = reviewService.getPendingReviews(1L, "human", 20);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("pending", result.get(0).getStatus());
    }

    @Test
    @DisplayName("approve 应更新状态为 APPROVED 并写入 reasonCode 和 notes")
    void shouldApprove() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.PENDING);
        when(reviewMapper.selectById(10L)).thenReturn(entity);
        when(reviewMapper.update(entity)).thenReturn(1);

        ReviewDetailVO result = reviewService.approve(1L, 10L, "CODE_OK", "LGTM");

        assertNotNull(result);
        verify(reviewMapper).update(entity);
        assertEquals(ReviewResult.APPROVED, entity.getStatus());
        assertEquals(1L, entity.getReviewerId());
        assertEquals("CODE_OK", entity.getReasonCode());
        assertEquals("LGTM", entity.getNotes());
    }

    @Test
    @DisplayName("reject 应更新状态为 REJECTED 并写入 reasonCode")
    void shouldReject() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.PENDING);
        when(reviewMapper.selectById(10L)).thenReturn(entity);
        when(reviewMapper.update(entity)).thenReturn(1);

        ReviewDetailVO result = reviewService.reject(1L, 10L, "RISK_HIGH", "R4 not allowed");

        assertNotNull(result);
        verify(reviewMapper).update(entity);
        assertEquals(ReviewResult.REJECTED, entity.getStatus());
        assertEquals("RISK_HIGH", entity.getReasonCode());
        assertEquals("R4 not allowed", entity.getNotes());
    }

    @Test
    @DisplayName("quarantine 应更新状态为 QUARANTINED 并写入 reasonCode 和 notes")
    void shouldQuarantine() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.PENDING);
        when(reviewMapper.selectById(10L)).thenReturn(entity);
        when(reviewMapper.update(entity)).thenReturn(1);

        ReviewDetailVO result = reviewService.quarantine(1L, 10L, "QUARANTINE", "malicious content");

        assertNotNull(result);
        verify(reviewMapper).update(entity);
        assertEquals(ReviewResult.QUARANTINED, entity.getStatus());
        assertEquals("QUARANTINE", entity.getReasonCode());
        assertEquals("malicious content", entity.getNotes());
    }

    @Test
    @DisplayName("appeal 应更新状态为 APPEAL_IN_PROGRESS")
    void shouldAppeal() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.REJECTED);
        when(reviewMapper.selectById(10L)).thenReturn(entity);
        when(reviewMapper.update(entity)).thenReturn(1);

        ReviewDetailVO result = reviewService.appeal(2L, 10L, "I disagree with rejection");

        assertNotNull(result);
        verify(reviewMapper).update(entity);
        assertEquals(ReviewResult.APPEAL_IN_PROGRESS, entity.getStatus());
        assertEquals("I disagree with rejection", entity.getAppealContent());
    }

    @Test
    @DisplayName("申诉时状态不是 REJECTED 或 QUARANTINED 应抛异常")
    void shouldThrowWhenAppealInvalidStatus() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.PENDING);
        when(reviewMapper.selectById(10L)).thenReturn(entity);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.appeal(2L, 10L, "valid content"));

        assertEquals(ErrorCode.STATUS_TRANSITION_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("申诉内容为空时应抛 PARAM_INVALID 异常")
    void shouldThrowWhenAppealContentBlank() {
        // blank 检查在 selectById 之前，不需要 mock mapper
        BizException ex = assertThrows(BizException.class,
                () -> reviewService.appeal(2L, 10L, ""));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("从 QUARANTINED 状态发起申诉应更新为 APPEAL_IN_PROGRESS")
    void shouldAppealFromQuarantined() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.QUARANTINED);
        when(reviewMapper.selectById(10L)).thenReturn(entity);
        when(reviewMapper.update(entity)).thenReturn(1);

        ReviewDetailVO result = reviewService.appeal(2L, 10L, "I disagree with quarantine");

        assertNotNull(result);
        verify(reviewMapper).update(entity);
        assertEquals(ReviewResult.APPEAL_IN_PROGRESS, entity.getStatus());
        assertEquals("I disagree with quarantine", entity.getAppealContent());
    }

    @Test
    @DisplayName("审核已完成时重复审核应抛异常")
    void shouldThrowWhenAlreadyFinal() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.APPROVED);
        when(reviewMapper.selectById(10L)).thenReturn(entity);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.approve(1L, 10L, "", ""));

        assertEquals(ErrorCode.STATUS_TRANSITION_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("审核任务不存在时抛异常")
    void shouldThrowWhenReviewNotFound() {
        when(reviewMapper.selectById(999L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.getReviewDetail(1L, 999L));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("getReviewDetail 应返回详情")
    void shouldReturnReviewDetail() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.PENDING);
        entity.setQueue(ReviewQueue.HUMAN);
        when(reviewMapper.selectById(10L)).thenReturn(entity);

        ReviewDetailVO result = reviewService.getReviewDetail(1L, 10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("pending", result.getStatus());
    }

    private ReviewEntity reviewEntity(Long id, ReviewResult status) {
        ReviewEntity entity = new ReviewEntity();
        entity.setId(id);
        entity.setObjectType("solution");
        entity.setObjectId(77L);
        entity.setQueue(ReviewQueue.HUMAN);
        entity.setStatus(status);
        entity.setRiskLevel(null);
        entity.setVersion(1L);
        return entity;
    }
}
