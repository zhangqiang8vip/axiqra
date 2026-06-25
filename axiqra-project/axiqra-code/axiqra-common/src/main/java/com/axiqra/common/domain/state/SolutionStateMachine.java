package com.axiqra.common.domain.state;

import com.axiqra.common.domain.enums.SolutionStatus;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;

import java.util.*;

/**
 * Solution 状态机
 *
 * 定义 Solution 状态之间的合法迁移规则
 * 对应 D08 Solution状态机文档
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
public class SolutionStateMachine {

    private static final Map<SolutionStatus, Set<SolutionStatus>> TRANSITIONS = new EnumMap<>(SolutionStatus.class);

    static {
        // Draft -> Candidate
        addTransition(SolutionStatus.DRAFT, SolutionStatus.CANDIDATE);

        // Candidate -> NeedsReview / Rejected
        addTransition(SolutionStatus.CANDIDATE, SolutionStatus.NEEDS_REVIEW);
        addTransition(SolutionStatus.CANDIDATE, SolutionStatus.REJECTED);

        // NeedsReview -> Reviewed / Quarantined
        addTransition(SolutionStatus.NEEDS_REVIEW, SolutionStatus.REVIEWED);
        addTransition(SolutionStatus.NEEDS_REVIEW, SolutionStatus.QUARANTINED);

        // Reviewed -> Verified / Deprecated
        addTransition(SolutionStatus.REVIEWED, SolutionStatus.VERIFIED);
        addTransition(SolutionStatus.REVIEWED, SolutionStatus.DEPRECATED);

        // Verified -> Stable / Deprecated / Quarantined
        addTransition(SolutionStatus.VERIFIED, SolutionStatus.STABLE);
        addTransition(SolutionStatus.VERIFIED, SolutionStatus.DEPRECATED);
        addTransition(SolutionStatus.VERIFIED, SolutionStatus.QUARANTINED);

        // Stable -> Canonical / Deprecated / Quarantined
        addTransition(SolutionStatus.STABLE, SolutionStatus.CANONICAL);
        addTransition(SolutionStatus.STABLE, SolutionStatus.DEPRECATED);
        addTransition(SolutionStatus.STABLE, SolutionStatus.QUARANTINED);

        // Canonical -> Deprecated / Quarantined
        addTransition(SolutionStatus.CANONICAL, SolutionStatus.DEPRECATED);
        addTransition(SolutionStatus.CANONICAL, SolutionStatus.QUARANTINED);

        // Quarantined -> NeedsReview
        addTransition(SolutionStatus.QUARANTINED, SolutionStatus.NEEDS_REVIEW);

        // Deprecated -> Archived
        addTransition(SolutionStatus.DEPRECATED, SolutionStatus.ARCHIVED);
    }

    private static void addTransition(SolutionStatus from, SolutionStatus to) {
        TRANSITIONS.computeIfAbsent(from, k -> new HashSet<>()).add(to);
    }

    /**
     * 检查状态迁移是否合法
     */
    public static boolean canTransition(SolutionStatus from, SolutionStatus to) {
        if (from == null || to == null) {
            return false;
        }
        Set<SolutionStatus> allowedTargets = TRANSITIONS.get(from);
        return allowedTargets != null && allowedTargets.contains(to);
    }

    /**
     * 获取从指定状态可以迁移到的所有目标状态
     */
    public static Set<SolutionStatus> getAllowedTransitions(SolutionStatus from) {
        if (from == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(TRANSITIONS.getOrDefault(from, Collections.emptySet()));
    }

    /**
     * 验证状态迁移，非法时抛出异常
     */
    public static void validateTransition(SolutionStatus from, SolutionStatus to) {
        if (!canTransition(from, to)) {
            throw new BizException(
                    ErrorCode.STATUS_TRANSITION_INVALID,
                    String.format("Solution 状态从 %s 不能迁移到 %s", from, to)
            );
        }
    }

    /**
     * 获取状态的中文描述
     */
    public static String getDescription(SolutionStatus status) {
        if (status == null) {
            return "未知状态";
        }
        return status.getDesc();
    }

    /**
     * 获取状态迁移事件名称
     */
    public static String getTransitionEvent(SolutionStatus from, SolutionStatus to) {
        if (!canTransition(from, to)) {
            return "invalid_transition";
        }
        return switch (to) {
            case CANDIDATE -> "submit_candidate";
            case NEEDS_REVIEW -> "request_review";
            case REVIEWED -> "approve_review";
            case VERIFIED -> "attach_evidence";
            case STABLE -> "promote_stable";
            case CANONICAL -> "promote_canonical";
            case DEPRECATED -> "deprecate";
            case QUARANTINED -> "quarantine";
            case ARCHIVED -> "archive";
            case REJECTED -> "reject";
            default -> "unknown_event";
        };
    }

    /**
     * 获取状态机的 Mermaid 图表
     */
    public static String toMermaidDiagram() {
        StringBuilder sb = new StringBuilder();
        sb.append("stateDiagram-v2\n");
        sb.append("    [*] --> DRAFT\n");

        for (Map.Entry<SolutionStatus, Set<SolutionStatus>> entry : TRANSITIONS.entrySet()) {
            for (SolutionStatus to : entry.getValue()) {
                sb.append("    ")
                  .append(entry.getKey().name())
                  .append(" --> ")
                  .append(to.name())
                  .append("\n");
            }
        }

        return sb.toString();
    }
}
