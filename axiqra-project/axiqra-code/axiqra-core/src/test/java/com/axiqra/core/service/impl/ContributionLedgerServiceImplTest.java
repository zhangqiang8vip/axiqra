package com.axiqra.core.service.impl;

import com.axiqra.common.audit.AuditPort;
import com.axiqra.common.domain.enums.ContributionType;
import com.axiqra.common.domain.vo.ContributionRecordVO;
import com.axiqra.common.domain.vo.ContributionStatsVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.ContributionLedgerService.ContributionRecordRequest;
import com.axiqra.core.service.ContributionLedgerService.ContributionRankingItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContributionLedgerServiceImpl 单元测试")
class ContributionLedgerServiceImplTest {

    @Mock
    private AuditPort auditPort;

    private ContributionLedgerServiceImpl contributionLedgerService;

    @BeforeEach
    void setUp() {
        contributionLedgerService = new ContributionLedgerServiceImpl(auditPort);
    }

    @Nested
    @DisplayName("recordContribution")
    class RecordContributionTests {

        @Test
        @DisplayName("userId 为空时应抛出异常")
        void shouldThrowWhenUserIdNull() {
            ContributionRecordRequest request = new ContributionRecordRequest(
                    "solution_create", null, null, 0, null
            );

            BizException ex = assertThrows(BizException.class,
                    () -> contributionLedgerService.recordContribution(null, request));

            assertEquals(ErrorCode.UNAUTHORIZED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("贡献类型为空时应抛出异常")
        void shouldThrowWhenContributionTypeNull() {
            ContributionRecordRequest request = new ContributionRecordRequest(
                    null, 1L, "solution", 0, null
            );

            BizException ex = assertThrows(BizException.class,
                    () -> contributionLedgerService.recordContribution(1L, request));

            assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("无效的贡献类型时应抛出异常")
        void shouldThrowWhenInvalidContributionType() {
            ContributionRecordRequest request = new ContributionRecordRequest(
                    "invalid_type", 1L, "solution", 0, null
            );

            BizException ex = assertThrows(BizException.class,
                    () -> contributionLedgerService.recordContribution(1L, request));

            assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("无效的贡献类型"));
        }

        @Test
        @DisplayName("有效请求应记录贡献并返回记录")
        void shouldRecordContribution() {
            ContributionRecordRequest request = new ContributionRecordRequest(
                    "solution_create",
                    100L,
                    "solution",
                    10,
                    "创建了一个新方案"
            );

            ContributionRecordVO result = contributionLedgerService.recordContribution(1L, request);

            assertNotNull(result);
            assertNotNull(result.getContributionId());
            assertEquals(1L, result.getUserId());
            assertEquals(ContributionType.SOLUTION_CREATE.getCode(), result.getContributionType());
            assertEquals(100L, result.getTargetId());
            assertEquals("solution", result.getTargetType());
            assertEquals(10, result.getPoints());
            assertEquals("创建了一个新方案", result.getDescription());
            assertNotNull(result.getContributedAt());

            verify(auditPort).log(any(AuditPort.AuditEvent.class));
        }

        @Test
        @DisplayName("未指定积分时应使用默认积分")
        void shouldUseDefaultPointsWhenNotSpecified() {
            ContributionRecordRequest request = new ContributionRecordRequest(
                    "solution_create",
                    100L,
                    "solution",
                    0,
                    null
            );

            ContributionRecordVO result = contributionLedgerService.recordContribution(1L, request);

            assertEquals(ContributionType.SOLUTION_CREATE.getBasePoints(), result.getPoints());
        }

        @Test
        @DisplayName("记录贡献时应触发审计日志")
        void shouldLogAuditEvent() {
            ContributionRecordRequest request = new ContributionRecordRequest(
                    "trace_submit",
                    200L,
                    "trace",
                    5,
                    "提交了一条轨迹"
            );

            contributionLedgerService.recordContribution(1L, request);

            ArgumentCaptor<AuditPort.AuditEvent> captor = ArgumentCaptor.forClass(AuditPort.AuditEvent.class);
            verify(auditPort).log(captor.capture());

            AuditPort.AuditEvent event = captor.getValue();
            assertEquals("contribution.record", event.action());
            assertEquals("user", event.actorType());
            assertEquals("success", event.result());
        }
    }

    @Nested
    @DisplayName("getUserContributions")
    class GetUserContributionsTests {

        @Test
        @DisplayName("userId 为空时应返回空列表")
        void shouldReturnEmptyListWhenUserIdNull() {
            List<ContributionRecordVO> result = contributionLedgerService.getUserContributions(null, 10);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("用户无贡献时应返回空列表")
        void shouldReturnEmptyListWhenNoContributions() {
            List<ContributionRecordVO> result = contributionLedgerService.getUserContributions(999L, 10);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("应返回用户的贡献记录并按时间倒序")
        void shouldReturnUserContributionsSorted() throws InterruptedException {
            // 记录多个贡献
            contributionLedgerService.recordContribution(1L,
                    new ContributionRecordRequest("solution_create", 1L, "solution", 10, null));
            Thread.sleep(10);
            contributionLedgerService.recordContribution(1L,
                    new ContributionRecordRequest("trace_submit", 2L, "trace", 5, null));
            Thread.sleep(10);
            contributionLedgerService.recordContribution(1L,
                    new ContributionRecordRequest("review_approve", 3L, "review", 3, null));

            List<ContributionRecordVO> result = contributionLedgerService.getUserContributions(1L, 10);

            assertEquals(3, result.size());
            // 最新的应该在最前面
            assertEquals("review_approve", result.get(0).getContributionType());
            assertEquals("trace_submit", result.get(1).getContributionType());
            assertEquals("solution_create", result.get(2).getContributionType());
        }

        @Test
        @DisplayName("应限制返回数量")
        void shouldLimitResults() {
            for (int i = 0; i < 10; i++) {
                contributionLedgerService.recordContribution(1L,
                        new ContributionRecordRequest("solution_create", (long) i, "solution", 1, null));
            }

            List<ContributionRecordVO> result = contributionLedgerService.getUserContributions(1L, 5);

            assertEquals(5, result.size());
        }
    }

    @Nested
    @DisplayName("getUserStats")
    class GetUserStatsTests {

        @Test
        @DisplayName("userId 为空时应抛出异常")
        void shouldThrowWhenUserIdNull() {
            BizException ex = assertThrows(BizException.class,
                    () -> contributionLedgerService.getUserStats(null));

            assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("userId"));
        }

        @Test
        @DisplayName("无贡献用户应返回零统计")
        void shouldReturnZeroStatsForNoContributions() {
            ContributionStatsVO stats = contributionLedgerService.getUserStats(999L);

            assertNotNull(stats);
            assertEquals(999L, stats.getUserId());
            assertEquals(0, stats.getTotalPoints());
            assertEquals(0, stats.getWeeklyPoints());
            assertEquals(0, stats.getMonthlyPoints());
            assertEquals(0, stats.getContributionCount());
            assertEquals(0, stats.getRank());
        }

        @Test
        @DisplayName("应正确计算用户统计数据")
        void shouldCalculateUserStats() {
            contributionLedgerService.recordContribution(1L,
                    new ContributionRecordRequest("solution_create", 1L, "solution", 10, null));
            contributionLedgerService.recordContribution(1L,
                    new ContributionRecordRequest("trace_submit", 2L, "trace", 5, null));

            ContributionStatsVO stats = contributionLedgerService.getUserStats(1L);

            assertEquals(1L, stats.getUserId());
            assertEquals(15, stats.getTotalPoints());
            assertEquals(15, stats.getWeeklyPoints());
            assertEquals(15, stats.getMonthlyPoints());
            assertEquals(2, stats.getContributionCount());
        }
    }

    @Nested
    @DisplayName("getLeaderboard")
    class GetLeaderboardTests {

        @Test
        @DisplayName("无贡献时应返回空列表")
        void shouldReturnEmptyListWhenNoContributions() {
            List<ContributionRecordVO> result = contributionLedgerService.getLeaderboard(10);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("应按积分降序返回排行榜")
        void shouldReturnLeaderboardSortedByPoints() {
            contributionLedgerService.recordContribution(1L,
                    new ContributionRecordRequest("solution_create", 1L, "solution", 10, null));
            contributionLedgerService.recordContribution(2L,
                    new ContributionRecordRequest("solution_create", 2L, "solution", 50, null));
            contributionLedgerService.recordContribution(3L,
                    new ContributionRecordRequest("trace_submit", 3L, "trace", 25, null));

            List<ContributionRecordVO> result = contributionLedgerService.getLeaderboard(10);

            assertEquals(3, result.size());
            assertEquals(2L, result.get(0).getUserId()); // 50 分
            assertEquals(3L, result.get(1).getUserId()); // 25 分
            assertEquals(1L, result.get(2).getUserId()); // 10 分
        }

        @Test
        @DisplayName("应限制排行榜数量")
        void shouldLimitLeaderboardSize() {
            for (long i = 1; i <= 20; i++) {
                contributionLedgerService.recordContribution(i,
                        new ContributionRecordRequest("solution_create", i, "solution", (int) i, null));
            }

            List<ContributionRecordVO> result = contributionLedgerService.getLeaderboard(5);

            assertEquals(5, result.size());
        }
    }

    @Nested
    @DisplayName("getContribution")
    class GetContributionTests {

        @Test
        @DisplayName("contributionId 为空时应抛出异常")
        void shouldThrowWhenContributionIdNull() {
            BizException ex = assertThrows(BizException.class,
                    () -> contributionLedgerService.getContribution(null));

            assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("contributionId 为空字符串时应抛出异常")
        void shouldThrowWhenContributionIdBlank() {
            BizException ex = assertThrows(BizException.class,
                    () -> contributionLedgerService.getContribution("   "));

            assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("不存在的贡献记录应返回 null")
        void shouldReturnNullWhenNotFound() {
            ContributionRecordVO result = contributionLedgerService.getContribution("non-existent-id");

            assertNull(result);
        }

        @Test
        @DisplayName("应返回存在的贡献记录")
        void shouldReturnExistingContribution() {
            ContributionRecordVO created = contributionLedgerService.recordContribution(1L,
                    new ContributionRecordRequest("solution_create", 1L, "solution", 10, "测试贡献"));

            ContributionRecordVO result = contributionLedgerService.getContribution(created.getContributionId());

            assertNotNull(result);
            assertEquals(created.getContributionId(), result.getContributionId());
            assertEquals(1L, result.getUserId());
        }
    }

    @Nested
    @DisplayName("边界用例")
    class BoundaryTests {

        @Test
        @DisplayName("应正确处理负数积分")
        void shouldHandleNegativePoints() {
            ContributionRecordRequest request = new ContributionRecordRequest(
                    "solution_create",
                    1L,
                    "solution",
                    -100,
                    null
            );

            ContributionRecordVO result = contributionLedgerService.recordContribution(1L, request);

            // 负数积分会被替换为默认值
            assertTrue(result.getPoints() > 0);
        }

        @Test
        @DisplayName("应正确处理空描述")
        void shouldHandleEmptyDescription() {
            ContributionRecordRequest request = new ContributionRecordRequest(
                    "solution_create",
                    1L,
                    "solution",
                    10,
                    ""
            );

            ContributionRecordVO result = contributionLedgerService.recordContribution(1L, request);

            assertNotNull(result.getDescription());
        }

        @Test
        @DisplayName("应正确处理 limit 为 0 的情况")
        void shouldHandleZeroLimit() {
            contributionLedgerService.recordContribution(1L,
                    new ContributionRecordRequest("solution_create", 1L, "solution", 10, null));

            List<ContributionRecordVO> result = contributionLedgerService.getUserContributions(1L, 0);

            // limit 为 0 时应返回所有记录
            assertTrue(result.size() >= 1);
        }
    }
}
