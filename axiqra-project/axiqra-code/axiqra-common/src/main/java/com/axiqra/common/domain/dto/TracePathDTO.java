package com.axiqra.common.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Engineering Trace 路径数据
 *
 * 用于记录完整的工程轨迹信息
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TracePathDTO {

    /** 正向路径 - 成功解决问题的步骤 */
    private ForwardPath forwardPath;

    /** 决策路径 - 关键决策点 */
    private List<DecisionStep> decisionPath;

    /** 回滚路径 - 失败时的回滚步骤 */
    private RollbackPath rollbackPath;

    /** 演化提示 - 方案的演进方向 */
    private String evolutionHint;

    /** 反向路径 - 从结果反向推导 */
    private ReversePath reversePath;

    /**
     * 正向路径
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ForwardPath {
        /** 初始问题描述 */
        private String initialProblem;

        /** 解决步骤列表 */
        private List<String> steps;

        /** 最终结果 */
        private String result;

        /** 耗时 (秒) */
        private Integer durationSeconds;

        /** 是否成功 */
        private Boolean success;
    }

    /**
     * 决策步骤
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DecisionStep {
        /** 决策点描述 */
        private String description;

        /** 可选方案 */
        private List<String> options;

        /** 最终选择 */
        private String chosenOption;

        /** 选择理由 */
        private String reason;

        /** 决策时间戳 */
        private Long timestamp;
    }

    /**
     * 回滚路径
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RollbackPath {
        /** 失败步骤描述 */
        private String failureStep;

        /** 失败原因 */
        private String failureReason;

        /** 回滚步骤列表 */
        private List<String> rollbackSteps;

        /** 回滚后状态 */
        private String stateAfterRollback;

        /** 是否完全回滚成功 */
        private Boolean success;
    }

    /**
     * 反向路径
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReversePath {
        /** 最终结果 */
        private String finalResult;

        /** 逆向分析步骤 */
        private List<String> reverseSteps;

        /** 关键发现 */
        private List<String> keyFindings;

        /** 教训总结 */
        private List<String> lessonsLearned;
    }
}
