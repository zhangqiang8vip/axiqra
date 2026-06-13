package com.axiqra.core.service.impl;

import com.axiqra.common.audit.AuditPort;
import com.axiqra.common.domain.dto.ConnectSessionCreateRequest;
import com.axiqra.common.domain.vo.ConnectDoctorVO;
import com.axiqra.common.domain.vo.ConnectSessionEventVO;
import com.axiqra.common.domain.vo.ConnectSessionVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.common.port.ConnectSessionPort;
import com.axiqra.core.service.ConnectService;
import com.axiqra.core.service.QuotaService;
import com.axiqra.core.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectServiceImpl implements ConnectService {

    private final QuotaService quotaService;
    private final RbacService rbacService;
    private final AuditPort auditPort;
    private final ConnectSessionPort connectSessionPort;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConnectSessionVO createSession(Long userId, ConnectSessionCreateRequest request) {
        quotaService.consumeOrThrow(userId, "connect_session_daily");

        ConnectDoctorVO doctor = runDoctor(userId, request.getChannel(), request.getToolType(), request.getWorkspaceId());
        String status = doctorStatusToSessionStatus(doctor.getStatus());
        OffsetDateTime createdAt = OffsetDateTime.now();
        OffsetDateTime expiresAt = createdAt.plusHours(1);
        String reason = "doctor=" + doctor.getStatus();
        ConnectSessionVO session = ConnectSessionVO.builder()
                .sessionId(UUID.randomUUID().toString().replace("-", ""))
                .userId(userId)
                .channel(request.getChannel())
                .toolType(request.getToolType())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .workspaceId(request.getWorkspaceId())
                .status(status)
                .riskLevel(request.getRiskLevel() == null || request.getRiskLevel().isBlank() ? "R1" : request.getRiskLevel())
                .confirmationObtained(Boolean.TRUE.equals(request.getConfirmationObtained()))
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .doctor(doctor)
                .history(ConnectSessionEventVO.createHistory("CHECKED", status, reason, createdAt))
                .build();

        connectSessionPort.save(session);
        auditPort.logInvocation(new AuditPort.InvocationEvent(
                traceId(),
                request.getChannel(),
                request.getToolType(),
                userId,
                request.getTargetType(),
                request.getTargetId(),
                request.getWorkspaceId(),
                session.getRiskLevel(),
                session.getConfirmationObtained(),
                "success",
                0L,
                status
        ));
        return session;
    }

    @Override
    public ConnectDoctorVO runDoctor(Long userId, String channel, String toolType, Long workspaceId) {
        boolean loggedIn = userId != null && userId > 0;
        boolean channelOk = channel != null && !channel.isBlank();
        boolean toolTypeOk = toolType != null && !toolType.isBlank();
        boolean workspaceAccess = workspaceId == null || hasWorkspaceAccess(userId, workspaceId);
        boolean connectRead = userId != null && rbacService.hasScope(userId, "connect:read");
        boolean connectWrite = userId != null && rbacService.hasScope(userId, "connect:write");
        boolean targetReachable = true;
        boolean stateReady = loggedIn && channelOk && toolTypeOk && connectWrite;

        List<ConnectDoctorVO.DoctorCheckItemVO> checks = List.of(
                item("LOGIN", "用户已登录", loggedIn, loggedIn ? "login ok" : "missing login"),
                item("CHANNEL", "接入渠道合法", channelOk, channelOk ? channel : "channel missing"),
                item("TOOL_TYPE", "工具类型已声明", toolTypeOk, toolTypeOk ? toolType : "toolType missing"),
                item("WORKSPACE_ACCESS", "空间访问合法", workspaceAccess, workspaceAccess ? "workspace ok" : "workspace access denied"),
                item("CONNECT_READ", "具备 connect:read scope", connectRead, connectRead ? "scope ok" : "missing connect:read"),
                item("CONNECT_WRITE", "具备 connect:write scope", connectWrite, connectWrite ? "scope ok" : "missing connect:write"),
                item("TARGET_REACHABLE", "目标对象可接入", targetReachable, "target preflight ok"),
                item("STATE_READY", "状态机可进入 READY", stateReady, stateReady ? "ready" : "need prerequisites")
        );
        int passed = (int) checks.stream().filter(ConnectDoctorVO.DoctorCheckItemVO::isPassed).count();
        String status = passed == checks.size() ? "PASS" : (passed >= 6 ? "WARN" : "FAIL");

        if (!loggedIn) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未登录，无法创建 Connect 会话");
        }

        return ConnectDoctorVO.builder()
                .status(status)
                .passedChecks(passed)
                .totalChecks(checks.size())
                .checks(checks)
                .build();
    }

    @Override
    public ConnectSessionVO getSession(Long userId, String sessionId) {
        ConnectSessionVO session = connectSessionPort.get(sessionId)
                .orElseThrow(() -> new BizException(ErrorCode.CONNECT_SESSION_NOT_FOUND));
        if (!userId.equals(session.getUserId())) {
            throw new BizException(ErrorCode.CONNECT_SESSION_NOT_FOUND);
        }
        return session;
    }

    @Override
    public List<ConnectSessionVO> listSessions(Long userId) {
        return connectSessionPort.listByUser(userId);
    }

    private boolean hasWorkspaceAccess(Long userId, Long workspaceId) {
        if (workspaceId == null) {
            return true;
        }
        return rbacService.isMember(userId, workspaceId);
    }

    private String doctorStatusToSessionStatus(String doctorStatus) {
        if (doctorStatus == null) {
            log.warn("unknown doctor status: null, defaulting to BLOCKED");
            return "BLOCKED";
        }
        return switch (doctorStatus) {
            case "PASS" -> "READY";
            case "WARN" -> "DEGRADED";
            default -> {
                log.warn("unexpected doctor status: {}, mapping to BLOCKED", doctorStatus);
                yield "BLOCKED";
            }
        };
    }

    private ConnectDoctorVO.DoctorCheckItemVO item(String code, String description, boolean passed, String detail) {
        return ConnectDoctorVO.DoctorCheckItemVO.builder()
                .code(code)
                .description(description)
                .passed(passed)
                .detail(detail)
                .build();
    }

    private String traceId() {
        String traceId = MDC.get("traceId");
        return traceId == null || traceId.isBlank() ? "missing-trace-id" : traceId;
    }
}
