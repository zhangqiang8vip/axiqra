package com.axiqra.core.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Axiqra 业务指标埋点门面
 *
 * <p>统一封装 Counter / Timer，避免业务代码散落 Micrometer API。
 * 暴露给 /actuator/prometheus 抓取。
 *
 * <h2>S1 已接入的核心指标</h2>
 * <ul>
 *   <li>axiqra_search_requests_total — Search 调用总数（按 kind 标签区分 keyword / vector / hybrid）</li>
 *   <li>axiqra_search_duration_seconds — Search 耗时直方图</li>
 *   <li>axiqra_solution_invocations_total — Solution 拉起总数（按 result 标签区分 success / failed）</li>
 *   <li>axiqra_review_decisions_total — 审核决策总数（按 decision 标签区分 approved / rejected / quarantined）</li>
 *   <li>axiqra_feedback_submissions_total — 反馈提交总数（按 type 标签）</li>
 *   <li>axiqra_device_auth_codes_issued_total — 设备授权码签发数</li>
 *   <li>axiqra_device_auth_token_refreshes_total — 设备授权 token 刷新数（按 result 标签）</li>
 * </ul>
 *
 * @author Axiqra Team
 * @since 2026-06-27
 */
@Component
@RequiredArgsConstructor
public class AxiqraMetrics {

    private final MeterRegistry registry;

    public void recordSearch(String kind, Duration latency, boolean success) {
        Counter.builder("axiqra.search.requests")
                .description("Search Before Act 调用总数")
                .tag("kind", kind)
                .tag("result", success ? "success" : "failed")
                .register(registry)
                .increment();
        Timer.builder("axiqra.search.duration")
                .description("Search 耗时")
                .tag("kind", kind)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry)
                .record(latency);
    }

    public void recordSolutionInvocation(String result) {
        Counter.builder("axiqra.solution.invocations")
                .description("Solution 拉起总数")
                .tag("result", result)
                .register(registry)
                .increment();
    }

    public void recordReviewDecision(String decision) {
        Counter.builder("axiqra.review.decisions")
                .description("审核决策总数")
                .tag("decision", decision)
                .register(registry)
                .increment();
    }

    public void recordFeedbackSubmission(String type) {
        Counter.builder("axiqra.feedback.submissions")
                .description("反馈提交总数")
                .tag("type", type)
                .register(registry)
                .increment();
    }

    public void recordDeviceAuthCodeIssued() {
        Counter.builder("axiqra.device_auth.codes_issued")
                .description("设备授权码签发数")
                .register(registry)
                .increment();
    }

    public void recordDeviceAuthTokenRefresh(String result) {
        Counter.builder("axiqra.device_auth.token_refreshes")
                .description("设备授权 token 刷新数")
                .tag("result", result)
                .register(registry)
                .increment();
    }

    /** Trace 草稿创建（含主动 writeback 路径）。 */
    public void recordTraceDraftCreated(String riskLevel) {
        Counter.builder("axiqra.trace.drafts_created")
                .description("Trace 草稿创建总数")
                .tag("risk_level", riskLevel == null ? "unknown" : riskLevel)
                .register(registry)
                .increment();
    }

    /** Trace 提交（从 draft → submitted）。 */
    public void recordTraceSubmitted(String submitPath, boolean success) {
        Counter.builder("axiqra.trace.submissions")
                .description("Trace 提交总数")
                .tag("path", submitPath == null ? "unknown" : submitPath)
                .tag("result", success ? "success" : "failed")
                .register(registry)
                .increment();
    }

    /** Workspace 创建（personal / team / enterprise）。 */
    public void recordWorkspaceCreated(String workspaceType) {
        Counter.builder("axiqra.workspace.created")
                .description("Workspace 创建总数")
                .tag("type", workspaceType == null ? "unknown" : workspaceType)
                .register(registry)
                .increment();
    }

    /** ProjectCase 创建（含公开/团队分支）。 */
    public void recordProjectCaseCreated(String visibility) {
        Counter.builder("axiqra.project_case.created")
                .description("Project Case 创建总数")
                .tag("visibility", visibility == null ? "unknown" : visibility)
                .register(registry)
                .increment();
    }
}
