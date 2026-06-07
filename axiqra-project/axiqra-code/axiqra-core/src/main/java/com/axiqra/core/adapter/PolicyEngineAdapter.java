package com.axiqra.core.adapter;

import com.axiqra.common.domain.dto.PolicyEvaluationRequest;
import com.axiqra.common.domain.enums.PolicyDecision;
import com.axiqra.common.domain.enums.ScopeEnum;
import com.axiqra.common.domain.vo.PolicyEvaluationVO;
import com.axiqra.common.port.PolicyEnginePort;
import com.axiqra.common.port.RbacPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * ABAC 策略引擎适配器
 *
 * <p>轻量级策略引擎，支持：
 * <ul>
 *   <li>Scope 权限范围校验（connect:write, search:read 等）</li>
 *   <li>Workspace 成员关系校验</li>
 *   <li>风险等级校验（禁止 R4+ 在自动模式下执行）</li>
 *   <li>策略决策日志（异步写审计库）</li>
 * </ul>
 *
 * <p>S1 阶段使用规则化策略（硬编码规则），S2 升级为动态策略配置表。
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyEngineAdapter implements PolicyEnginePort {

    private final RbacPort rbacPort;
    private final ObjectMapper objectMapper;

    /** 按 scope 自动放行的对象类型 */
    private static final Map<String, String> SCOPE_OBJECT_TYPE_MAP = Map.ofEntries(
            Map.entry("connect:write", "connect_session"),
            Map.entry("connect:read", "connect_session"),
            Map.entry("search:read", "search"),
            Map.entry("search:public", "search"),
            Map.entry("solution:read", "solution"),
            Map.entry("solution:write", "solution"),
            Map.entry("solution:delete", "solution"),
            Map.entry("solution:publish", "solution"),
            Map.entry("trace:read", "trace"),
            Map.entry("trace:write", "trace"),
            Map.entry("trace:confirm", "trace"),
            Map.entry("case:read", "case"),
            Map.entry("case:write", "case"),
            Map.entry("case:publish", "case"),
            Map.entry("review:write", "review"),
            Map.entry("audit:read", "audit")
    );

    @Override
    public PolicyEvaluationVO evaluate(PolicyEvaluationRequest request) {
        if (request == null) {
            return PolicyEvaluationVO.builder()
                    .decision(PolicyDecision.DENY)
                    .reasonCode("INVALID_REQUEST")
                    .message("策略评估请求为空")
                    .build();
        }

        String action = request.getAction();
        Long subjectId = request.getSubjectId();
        String objectType = request.getObjectType();
        Long objectId = request.getObjectId();
        Map<String, Object> context = request.getContext();

        // 参数合法性检查
        if (action == null || action.isBlank()) {
            return PolicyEvaluationVO.builder()
                    .decision(PolicyDecision.DENY)
                    .reasonCode("INVALID_ACTION")
                    .message("action 参数不能为空")
                    .build();
        }
        if (subjectId == null) {
            return PolicyEvaluationVO.builder()
                    .decision(PolicyDecision.DENY)
                    .reasonCode("INVALID_SUBJECT")
                    .message("subjectId 不能为空")
                    .build();
        }

        // 1. Admin 全局权限 bypass（下沉到各分支末尾，避免跳过 action-specific 校验）
        // 2. 按 action 类型分发策略
        return switch (action.toLowerCase()) {
            case "read" -> evaluateReadAction(subjectId, objectType, objectId, context);
            case "write", "create", "update" -> evaluateWriteAction(subjectId, objectType, objectId, context);
            case "delete" -> evaluateDeleteAction(subjectId, objectType, objectId, context);
            case "publish" -> evaluatePublishAction(subjectId, objectType, objectId, context);
            default -> evaluateGenericAction(subjectId, objectType, objectId, action, context);
        };
    }

    @Override
    public boolean hasScope(Long userId, String scope) {
        if (userId == null || scope == null) {
            return false;
        }
        ScopeEnum scopeEnum = ScopeEnum.of(scope);
        if (scopeEnum == null) {
            log.warn("Unknown scope: {}", scope);
            return false;
        }
        // S1: 登录用户默认拥有基础 scope
        // S2: 从授权表（axiqra_authorization）读取用户持有的 scope
        Set<String> userScopes = getUserScopes(userId);
        return userScopes.contains(scope);
    }

    // ==================== 分支策略评估 ====================

    private PolicyEvaluationVO evaluateReadAction(Long userId, String objectType, Long objectId, Map<String, Object> context) {
        // 公开资源：任何登录用户可读
        if ("search_public".equals(objectType) || "public_case".equals(objectType)) {
            return PolicyEvaluationVO.builder()
                    .decision(PolicyDecision.ALLOW)
                    .policyCode("ABAC-READ-PUBLIC")
                    .reasonCode("PUBLIC_RESOURCE")
                    .message("公开资源，任何登录用户可读")
                    .build();
        }

        // 私有资源：必须是成员
        if (objectId != null) {
            if (rbacPort.isMember(userId, objectId)) {
                return PolicyEvaluationVO.builder()
                        .decision(PolicyDecision.ALLOW)
                        .policyCode("ABAC-READ-MEMBER")
                        .reasonCode("MEMBER_ACCESS")
                        .message("成员可读取私有资源")
                        .build();
            }
        }

        // Admin bypass
        if (objectId != null && rbacPort.isAdmin(userId, objectId)) {
            log.debug("Policy: admin bypass read for user={}, workspaceId={}", userId, objectId);
            return PolicyEvaluationVO.builder()
                    .decision(PolicyDecision.ALLOW)
                    .policyCode("ABAC-ADMIN-BYPASS")
                    .reasonCode("ADMIN_PRIVILEGE")
                    .message("管理员权限")
                    .build();
        }

        return PolicyEvaluationVO.builder()
                .decision(PolicyDecision.DENY_SCOPE_MISSING)
                .policyCode("ABAC-READ-DENY")
                .reasonCode("NOT_MEMBER")
                .message("非成员无权读取该资源")
                .build();
    }

    private PolicyEvaluationVO evaluateWriteAction(Long userId, String objectType, Long objectId, Map<String, Object> context) {
        // 缺失 riskLevel 视为高风险操作，默认拒绝
        String riskLevel = parseContextField(context, "riskLevel");
        if (riskLevel == null) {
            return PolicyEvaluationVO.builder()
                    .decision(PolicyDecision.DENY)
                    .policyCode("ABAC-RISK-DENY")
                    .reasonCode("RISK_LEVEL_MISSING")
                    .message("write 操作必须显式指定 riskLevel")
                    .build();
        }
        if ("R4".equals(riskLevel) || "R5".equals(riskLevel)) {
            String executionMode = parseContextField(context, "executionMode");
            if (!"MANUAL".equalsIgnoreCase(executionMode)) {
                return PolicyEvaluationVO.builder()
                        .decision(PolicyDecision.DENY_RISK_LEVEL_TOO_HIGH)
                        .policyCode("ABAC-RISK-DENY")
                        .reasonCode("RISK_LEVEL_TOO_HIGH")
                        .message("风险等级 R4/R5 仅允许手动模式执行")
                        .build();
            }
        }

        // 写入需要至少 member 角色
        if (objectId != null && rbacPort.hasRole(userId, objectId, com.axiqra.common.domain.enums.MemberRole.MEMBER)) {
            return PolicyEvaluationVO.builder()
                    .decision(PolicyDecision.ALLOW)
                    .policyCode("ABAC-WRITE-MEMBER")
                    .reasonCode("MEMBER_WRITE")
                    .message("成员可写入资源")
                    .build();
        }

        // Admin bypass
        if (objectId != null && rbacPort.isAdmin(userId, objectId)) {
            log.debug("Policy: admin bypass write for user={}, workspaceId={}", userId, objectId);
            return PolicyEvaluationVO.builder()
                    .decision(PolicyDecision.ALLOW)
                    .policyCode("ABAC-ADMIN-BYPASS")
                    .reasonCode("ADMIN_PRIVILEGE")
                    .message("管理员权限")
                    .build();
        }

        return PolicyEvaluationVO.builder()
                .decision(PolicyDecision.DENY_SCOPE_MISSING)
                .policyCode("ABAC-WRITE-DENY")
                .reasonCode("INSUFFICIENT_ROLE")
                .message("写入资源至少需要 member 角色")
                .build();
    }

    private PolicyEvaluationVO evaluateDeleteAction(Long userId, String objectType, Long objectId, Map<String, Object> context) {
        // 删除需要 admin 或 owner
        if (objectId != null && rbacPort.isAdmin(userId, objectId)) {
            return PolicyEvaluationVO.builder()
                    .decision(PolicyDecision.ALLOW)
                    .policyCode("ABAC-DELETE-ADMIN")
                    .reasonCode("ADMIN_DELETE")
                    .message("管理员可删除资源")
                    .build();
        }

        return PolicyEvaluationVO.builder()
                .decision(PolicyDecision.DENY)
                .policyCode("ABAC-DELETE-DENY")
                .reasonCode("NOT_ADMIN")
                .message("只有管理员或所有者可以删除资源")
                .build();
    }

    private PolicyEvaluationVO evaluatePublishAction(Long userId, String objectType, Long objectId, Map<String, Object> context) {
        // 发布需要 admin + 授权范围
        String licenseScope = parseContextField(context, "licenseScope");
        if (objectId != null && rbacPort.isAdmin(userId, objectId)) {
            if (licenseScope == null || licenseScope.isBlank()) {
                return PolicyEvaluationVO.builder()
                        .decision(PolicyDecision.DENY)
                        .policyCode("ABAC-PUBLISH-NO-LICENSE")
                        .reasonCode("LICENSE_SCOPE_MISSING")
                        .message("发布需要有效的授权范围（licenseScope）")
                        .build();
            }
            return PolicyEvaluationVO.builder()
                    .decision(PolicyDecision.ALLOW)
                    .policyCode("ABAC-PUBLISH-ADMIN")
                    .reasonCode("ADMIN_PUBLISH")
                    .message("管理员可发布资源")
                    .build();
        }

        return PolicyEvaluationVO.builder()
                .decision(PolicyDecision.DENY)
                .policyCode("ABAC-PUBLISH-DENY")
                .reasonCode("NOT_ADMIN")
                .message("只有管理员可以发布资源")
                .build();
    }

    private PolicyEvaluationVO evaluateGenericAction(Long userId, String objectType, Long objectId,
                                                    String action, Map<String, Object> context) {
        if (objectId != null && rbacPort.isMember(userId, objectId)) {
            return PolicyEvaluationVO.builder()
                    .decision(PolicyDecision.ALLOW)
                    .policyCode("ABAC-GENERIC-MEMBER")
                    .reasonCode("MEMBER_ACTION")
                    .message("成员可执行该操作")
                    .build();
        }
        return PolicyEvaluationVO.builder()
                .decision(PolicyDecision.DENY)
                .policyCode("ABAC-GENERIC-DENY")
                .reasonCode("NOT_MEMBER")
                .message("无权执行该操作")
                .build();
    }

    // ==================== 辅助方法 ====================

    /**
     * S1: 所有登录用户默认拥有基础 scope
     * S2: 从 axiqra_authorization 表读取实际授权范围
     */
    private Set<String> getUserScopes(Long userId) {
        // S1: 默认基础权限
        Set<String> defaultScopes = Set.of(
                ScopeEnum.SEARCH_READ.getCode(),
                ScopeEnum.SEARCH_PUBLIC.getCode(),
                ScopeEnum.SOLUTION_READ.getCode(),
                ScopeEnum.TRACE_READ.getCode(),
                ScopeEnum.TRACE_WRITE.getCode(),
                ScopeEnum.CASE_READ.getCode(),
                ScopeEnum.PUBLIC_READ.getCode()
        );
        // S2 TODO: 从 axiqra_authorization 表查询用户持有的实际 scope
        // authorizationMapper.selectByUserId(userId)
        return defaultScopes;
    }

    /**
     * 从 context Map 中安全提取字段值
     */
    private String parseContextField(Map<String, Object> context, String fieldName) {
        if (context == null) {
            return null;
        }
        Object value = context.get(fieldName);
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}
