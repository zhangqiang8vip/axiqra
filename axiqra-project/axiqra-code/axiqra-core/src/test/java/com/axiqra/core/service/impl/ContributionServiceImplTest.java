package com.axiqra.core.service.impl;

import com.axiqra.common.domain.vo.ContributionRecordVO;
import com.axiqra.common.domain.vo.ContributionStatsVO;
import com.axiqra.common.domain.vo.UserContributionVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.ContributionLedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContributionServiceImpl 单元测试")
class ContributionServiceImplTest {

    @Mock
    private ContributionLedgerService contributionLedgerService;

    private ContributionServiceImpl contributionService;

    @BeforeEach
    void setUp() {
        contributionService = new ContributionServiceImpl(contributionLedgerService);
    }

    @Nested
    @DisplayName("recordContribution")
    class RecordContributionTests {

        @Test
        @DisplayName("userId 为空时应抛出异常")
        void shouldThrowWhenUserIdNull() {
            BizException ex = assertThrows(BizException.class,
                    () -> contributionService.recordContribution(null, 1L, "solution_create", 100L, 10));

            assertEquals(ErrorCode.UNAUTHORIZED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("contributionType 为空时应抛出异常")
        void shouldThrowWhenContributionTypeNull() {
            BizException ex = assertThrows(BizException.class,
                    () -> contributionService.recordContribution(1L, 1L, null, 100L, 10));

            assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("贡献类型"));
        }

        @Test
        @DisplayName("contributionType 为空字符串时应抛出异常")
        void shouldThrowWhenContributionTypeBlank() {
            BizException ex = assertThrows(BizException.class,
                    () -> contributionService.recordContribution(1L, 1L, "   ", 100L, 10));

            assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("有效请求应记录贡献")
        void shouldRecordContribution() {
            contributionService.recordContribution(1L, 1L, "solution_create", 100L, 10);

            verify(contributionLedgerService).recordContribution(
                    eq(1L),
                    argThat(request ->
                            request.contributionType().equals("solution_create") &&
                            request.targetId().equals(100L) &&
                            request.points() == 10
                    )
            );
        }

        @Test
        @DisplayName("负数积分应被替换为 0")
        void shouldReplaceNegativePointsWithZero() {
            contributionService.recordContribution(1L, 1L, "solution_create", 100L, -10);

            verify(contributionLedgerService).recordContribution(
                    eq(1L),
                    argThat(request -> request.points() == 0)
            );
        }
    }

    @Nested
    @DisplayName("getUserContribution")
    class GetUserContributionTests {

        @Test
        @DisplayName("userId 为空时应抛出异常")
        void shouldThrowWhenUserIdNull() {
            BizException ex = assertThrows(BizException.class,
                    () -> contributionService.getUserContribution(null, 1L));

            assertEquals(ErrorCode.UNAUTHORIZED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("应返回用户贡献信息")
        void shouldReturnUserContribution() {
            ContributionStatsVO mockStats = ContributionStatsVO.builder()
                    .userId(1L)
                    .totalPoints(100)
                    .monthlyPoints(50)
                    .weeklyPoints(20)
                    .rank(5)
                    .contributionCount(10)
                    .contributionByType(Map.of("solution_create", 50L, "trace_submit", 50L))
                    .generatedAt(System.currentTimeMillis())
                    .build();

            when(contributionLedgerService.getUserStats(1L)).thenReturn(mockStats);

            UserContributionVO result = contributionService.getUserContribution(1L, 1L);

            assertNotNull(result);
            assertEquals(1L, result.getUserId());
            assertEquals(100, result.getTotalPoints());
            assertEquals(50, result.getMonthlyPoints());
            assertEquals(20, result.getWeeklyPoints());
            assertEquals(5, result.getRank());
            assertEquals(10, result.getContributionCount());
        }
    }

    @Nested
    @DisplayName("getContributionRankings")
    class GetContributionRankingsTests {

        @Test
        @DisplayName("应返回排行榜")
        void shouldReturnRankings() {
            List<ContributionLedgerService.ContributionRankingItem> mockItems = List.of(
                    new ContributionLedgerService.ContributionRankingItem(1L, 100L, 5),
                    new ContributionLedgerService.ContributionRankingItem(2L, 80L, 4),
                    new ContributionLedgerService.ContributionRankingItem(3L, 60L, 3)
            );

            when(contributionLedgerService.getLeaderboardWithUsers(20)).thenReturn(mockItems);

            var result = contributionService.getContributionRankings(1L, 20);

            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals(1, result.get(0).getRank());
            assertEquals(1L, result.get(0).getUserId());
            assertEquals(100, result.get(0).getTotalPoints());
        }

        @Test
        @DisplayName("limit 为 0 时应使用默认值 20")
        void shouldUseDefaultLimitWhenZero() {
            when(contributionLedgerService.getLeaderboardWithUsers(20)).thenReturn(List.of());

            contributionService.getContributionRankings(1L, 0);

            verify(contributionLedgerService).getLeaderboardWithUsers(20);
        }
    }

    @Nested
    @DisplayName("getRecentContributions")
    class GetRecentContributionsTests {

        @Test
        @DisplayName("userId 为空时应抛出异常")
        void shouldThrowWhenUserIdNull() {
            BizException ex = assertThrows(BizException.class,
                    () -> contributionService.getRecentContributions(null, 10));

            assertEquals(ErrorCode.UNAUTHORIZED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("应返回最近的贡献记录")
        void shouldReturnRecentContributions() {
            List<ContributionRecordVO> mockRecords = List.of(
                    ContributionRecordVO.builder()
                            .userId(1L)
                            .contributionType("solution_create")
                            .points(10)
                            .contributedAt(Instant.now())
                            .build()
            );

            when(contributionLedgerService.getUserContributions(1L, 10)).thenReturn(mockRecords);

            var result = contributionService.getRecentContributions(1L, 10);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).getActorId());
        }

        @Test
        @DisplayName("limit 为 0 时应使用默认值 10")
        void shouldUseDefaultLimitWhenZero() {
            when(contributionLedgerService.getUserContributions(1L, 10)).thenReturn(List.of());

            contributionService.getRecentContributions(1L, 0);

            verify(contributionLedgerService).getUserContributions(1L, 10);
        }
    }
}
