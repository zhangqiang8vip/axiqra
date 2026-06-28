package com.axiqra.core.service.impl;

import com.axiqra.common.domain.entity.ReviewEntity;
import com.axiqra.common.domain.enums.ReviewQueue;
import com.axiqra.common.domain.enums.ReviewResult;
import com.axiqra.common.domain.vo.ReviewDetailVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.ReviewMapper;
import com.axiqra.core.observability.AxiqraMetrics;
import com.axiqra.core.service.SolutionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReviewServiceImpl 单元测试")
class ReviewServiceImplTest {

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private SolutionService solutionService;

    @Mock
    private AxiqraMetrics metrics;

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
        entity.setObjectType("solution");
        entity.setObjectId(100L);
        when(reviewMapper.selectById(10L)).thenReturn(entity);
        when(reviewMapper.update(entity)).thenReturn(1);
        doNothing().when(solutionService).transitionAfterReviewApproved(100L, 1L);

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
        entity.setObjectType("solution");
        entity.setObjectId(100L);
        when(reviewMapper.selectById(10L)).thenReturn(entity);
        when(reviewMapper.update(entity)).thenReturn(1);
        doNothing().when(solutionService).transitionAfterReviewRejected(100L, 1L, "RISK_HIGH");

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
        entity.setObjectType("solution");
        entity.setObjectId(100L);
        when(reviewMapper.selectById(10L)).thenReturn(entity);
        when(reviewMapper.update(entity)).thenReturn(1);
        doNothing().when(solutionService).transitionToQuarantined(100L, 1L, "QUARANTINE");

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

    // ========== 边界用例测试 ==========

    @Test
    @DisplayName("approve 非 solution 类型不应调用 SolutionService")
    void shouldNotCallSolutionServiceForNonSolutionApprove() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.PENDING);
        entity.setObjectType("trace");
        entity.setObjectId(100L);
        when(reviewMapper.selectById(10L)).thenReturn(entity);
        when(reviewMapper.update(entity)).thenReturn(1);

        ReviewDetailVO result = reviewService.approve(1L, 10L, "CODE_OK", "LGTM");

        assertNotNull(result);
        assertEquals(ReviewResult.APPROVED, entity.getStatus());
    }

    @Test
    @DisplayName("reject 非 solution 类型不应调用 SolutionService")
    void shouldNotCallSolutionServiceForNonSolutionReject() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.PENDING);
        entity.setObjectType("case");
        entity.setObjectId(100L);
        when(reviewMapper.selectById(10L)).thenReturn(entity);
        when(reviewMapper.update(entity)).thenReturn(1);

        ReviewDetailVO result = reviewService.reject(1L, 10L, "RISK_HIGH", "R4 not allowed");

        assertNotNull(result);
        assertEquals(ReviewResult.REJECTED, entity.getStatus());
    }

    @Test
    @DisplayName("quarantine 非 solution 类型不应调用 SolutionService")
    void shouldNotCallSolutionServiceForNonSolutionQuarantine() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.PENDING);
        entity.setObjectType("project_case");
        entity.setObjectId(100L);
        when(reviewMapper.selectById(10L)).thenReturn(entity);
        when(reviewMapper.update(entity)).thenReturn(1);

        ReviewDetailVO result = reviewService.quarantine(1L, 10L, "QUARANTINE", "malicious content");

        assertNotNull(result);
        assertEquals(ReviewResult.QUARANTINED, entity.getStatus());
    }

    @Test
    @DisplayName("getReviewDetail 应返回审核详情")
    void shouldReturnReviewDetail() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.PENDING);
        entity.setReasonCode("INITIAL");
        entity.setNotes("Initial review");
        when(reviewMapper.selectById(10L)).thenReturn(entity);

        ReviewDetailVO result = reviewService.getReviewDetail(1L, 10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("pending", result.getStatus());
        assertEquals("INITIAL", result.getReasonCode());
    }

    @Test
    @DisplayName("getReviewDetail 审核不存在应抛异常")
    void shouldThrowWhenReviewNotFound() {
        when(reviewMapper.selectById(999L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.getReviewDetail(1L, 999L));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("getReviewDetail 审核已删除应抛异常")
    void shouldThrowWhenReviewDeleted() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.PENDING);
        entity.setDeleted(true);
        when(reviewMapper.selectById(10L)).thenReturn(entity);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.getReviewDetail(1L, 10L));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("approve 已完成的审核应抛异常")
    void shouldRejectApproveAlreadyFinalReview() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.APPROVED);
        when(reviewMapper.selectById(10L)).thenReturn(entity);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.approve(1L, 10L, "CODE_OK", "LGTM"));

        assertEquals(ErrorCode.STATUS_TRANSITION_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("reject 已完成的审核应抛异常")
    void shouldRejectRejectAlreadyFinalReview() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.REJECTED);
        when(reviewMapper.selectById(10L)).thenReturn(entity);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.reject(1L, 10L, "RISK_HIGH", "R4 not allowed"));

        assertEquals(ErrorCode.STATUS_TRANSITION_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("quarantine 已完成的审核应抛异常")
    void shouldRejectQuarantineAlreadyFinalReview() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.QUARANTINED);
        when(reviewMapper.selectById(10L)).thenReturn(entity);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.quarantine(1L, 10L, "QUARANTINE", "malicious"));

        assertEquals(ErrorCode.STATUS_TRANSITION_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("approve 审核不存在应抛异常")
    void shouldThrowWhenApproveReviewNotFound() {
        when(reviewMapper.selectById(999L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.approve(1L, 999L, "CODE_OK", "LGTM"));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("reject 审核不存在应抛异常")
    void shouldThrowWhenRejectReviewNotFound() {
        when(reviewMapper.selectById(999L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.reject(1L, 999L, "RISK_HIGH", "R4 not allowed"));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("quarantine 审核不存在应抛异常")
    void shouldThrowWhenQuarantineReviewNotFound() {
        when(reviewMapper.selectById(999L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.quarantine(1L, 999L, "QUARANTINE", "malicious"));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("approve 审核已删除应抛异常")
    void shouldThrowWhenApproveReviewDeleted() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.PENDING);
        entity.setDeleted(true);
        when(reviewMapper.selectById(10L)).thenReturn(entity);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.approve(1L, 10L, "CODE_OK", "LGTM"));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("reject 审核已删除应抛异常")
    void shouldThrowWhenRejectReviewDeleted() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.PENDING);
        entity.setDeleted(true);
        when(reviewMapper.selectById(10L)).thenReturn(entity);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.reject(1L, 10L, "RISK_HIGH", "R4 not allowed"));

        // 已删除的 Review 等价于不存在 → RESOURCE_NOT_FOUND (30001)
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("quarantine 审核已删除应抛异常")
    void shouldThrowWhenQuarantineReviewDeleted() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.PENDING);
        entity.setDeleted(true);
        when(reviewMapper.selectById(10L)).thenReturn(entity);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.quarantine(1L, 10L, "QUARANTINE", "malicious"));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("approve 乐观锁冲突应抛异常")
    void shouldThrowOnApproveOptimisticLockConflict() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.PENDING);
        when(reviewMapper.selectById(10L)).thenReturn(entity);
        when(reviewMapper.update(entity)).thenReturn(0);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.approve(1L, 10L, "CODE_OK", "LGTM"));

        assertEquals(ErrorCode.STATUS_TRANSITION_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("reject 乐观锁冲突应抛异常")
    void shouldThrowOnRejectOptimisticLockConflict() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.PENDING);
        when(reviewMapper.selectById(10L)).thenReturn(entity);
        when(reviewMapper.update(entity)).thenReturn(0);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.reject(1L, 10L, "RISK_HIGH", "R4 not allowed"));

        assertEquals(ErrorCode.STATUS_TRANSITION_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("quarantine 乐观锁冲突应抛异常")
    void shouldThrowOnQuarantineOptimisticLockConflict() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.PENDING);
        when(reviewMapper.selectById(10L)).thenReturn(entity);
        when(reviewMapper.update(entity)).thenReturn(0);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.quarantine(1L, 10L, "QUARANTINE", "malicious"));

        assertEquals(ErrorCode.STATUS_TRANSITION_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("appeal 审核不存在应抛异常")
    void shouldThrowWhenAppealReviewNotFound() {
        when(reviewMapper.selectById(999L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.appeal(2L, 999L, "I disagree"));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("appeal 审核已删除应抛异常")
    void shouldThrowWhenAppealReviewDeleted() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.REJECTED);
        entity.setDeleted(true);
        when(reviewMapper.selectById(10L)).thenReturn(entity);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.appeal(2L, 10L, "I disagree"));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("appeal 乐观锁冲突应抛异常")
    void shouldThrowOnAppealOptimisticLockConflict() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.REJECTED);
        when(reviewMapper.selectById(10L)).thenReturn(entity);
        when(reviewMapper.update(entity)).thenReturn(0);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.appeal(2L, 10L, "I disagree"));

        assertEquals(ErrorCode.STATUS_TRANSITION_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("appeal 内容为空白应抛异常")
    void shouldRejectAppealWithBlankContent() {
        BizException ex = assertThrows(BizException.class,
                () -> reviewService.appeal(2L, 10L, "   "));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("approve null objectId 不应调用 SolutionService")
    void shouldNotCallSolutionServiceWhenObjectIdIsNull() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.PENDING);
        entity.setObjectType("solution");
        entity.setObjectId(null);
        when(reviewMapper.selectById(10L)).thenReturn(entity);
        when(reviewMapper.update(entity)).thenReturn(1);

        ReviewDetailVO result = reviewService.approve(1L, 10L, "CODE_OK", "LGTM");

        assertNotNull(result);
        assertEquals(ReviewResult.APPROVED, entity.getStatus());
    }

    @Test
    @DisplayName("reject null objectId 不应调用 SolutionService")
    void shouldNotCallSolutionServiceWhenRejectObjectIdIsNull() {
        ReviewEntity entity = reviewEntity(10L, ReviewResult.PENDING);
        entity.setObjectType("solution");
        entity.setObjectId(null);
        when(reviewMapper.selectById(10L)).thenReturn(entity);
        when(reviewMapper.update(entity)).thenReturn(1);

        ReviewDetailVO result = reviewService.reject(1L, 10L, "RISK_HIGH", "R4 not allowed");

        assertNotNull(result);
        assertEquals(ReviewResult.REJECTED, entity.getStatus());
    }

    private ReviewEntity reviewEntity(Long id, ReviewResult status) {
        ReviewEntity entity = new ReviewEntity();
        entity.setId(id);
        entity.setStatus(status);
        entity.setQueue(ReviewQueue.HUMAN);
        entity.setObjectType("solution");
        entity.setObjectId(100L);
        entity.setDeleted(false);
        return entity;
    }
}
