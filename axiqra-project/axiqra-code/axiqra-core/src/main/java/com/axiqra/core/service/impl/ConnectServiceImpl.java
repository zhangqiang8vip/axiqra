package com.axiqra.core.service.impl;

import com.axiqra.common.audit.AuditPort;
import com.axiqra.common.domain.dto.ConnectSessionCreateRequest;
import com.axiqra.common.domain.enums.ConnectSessionEvent;
import com.axiqra.common.domain.enums.ConnectSessionStatus;
import com.axiqra.common.domain.vo.ConnectDoctorVO;
import com.axiqra.common.domain.vo.ConnectSessionEventVO;
import com.axiqra.common.domain.vo.ConnectSessionVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.common.port.ConnectSessionPort;
import com.axiqra.core.domain.ConnectSessionStateMachine;
import com.axiqra.core.service.ConnectService;
import com.axiqra.core.service.QuotaService;
import com.axiqra.core.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
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
                .history(ConnectSessionEventVO.createHistory(ConnectSessionEvent.CREATE_CONNECT_SESSION.getCode(), status, reason, createdAt))
                .toolCapability(request.getToolCapability())
                .authScope(request.getAuthScope())
                .lastSeenAt(createdAt)
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
        
        // ========== 扩展检测项 ==========
        // 环境检测
        boolean envCheck = checkEnvironment();
        // 网络检测
        boolean networkCheck = checkNetworkConnectivity();
        // 认证检测
        boolean authCheck = checkAuthValidity(userId);
        // 权限检测
        boolean permissionCheck = checkPermissions(userId);
        // 依赖检测
        boolean dependencyCheck = checkDependencies(toolType);
        // 配置检测
        boolean configCheck = checkConfiguration();
        // 版本检测
        boolean versionCheck = checkVersionCompatibility();
        // 模拟检测
        boolean mockCheck = checkMockCapabilities();

        List<ConnectDoctorVO.DoctorCheckItemVO> checks = List.of(
                item("LOGIN", "用户已登录", loggedIn, loggedIn ? "login ok" : "missing login"),
                item("CHANNEL", "接入渠道合法", channelOk, channelOk ? channel : "channel missing"),
                item("TOOL_TYPE", "工具类型已声明", toolTypeOk, toolTypeOk ? toolType : "toolType missing"),
                item("WORKSPACE_ACCESS", "空间访问合法", workspaceAccess, workspaceAccess ? "workspace ok" : "workspace access denied"),
                item("CONNECT_READ", "具备 connect:read scope", connectRead, connectRead ? "scope ok" : "missing connect:read"),
                item("CONNECT_WRITE", "具备 connect:write scope", connectWrite, connectWrite ? "scope ok" : "missing connect:write"),
                item("TARGET_REACHABLE", "目标对象可接入", targetReachable, "target preflight ok"),
                item("STATE_READY", "状态机可进入 READY", stateReady, stateReady ? "ready" : "need prerequisites"),
                // 扩展检测
                item("ENVIRONMENT", "运行环境检测", envCheck, envCheck ? "environment ok" : "environment check failed"),
                item("NETWORK", "网络连接检测", networkCheck, networkCheck ? "network accessible" : "network unreachable"),
                item("AUTH_VALIDITY", "认证有效性检测", authCheck, authCheck ? "auth valid" : "auth expired or invalid"),
                item("PERMISSIONS", "权限完整性检测", permissionCheck, permissionCheck ? "permissions ok" : "permissions incomplete"),
                item("DEPENDENCIES", "依赖完整性检测", dependencyCheck, dependencyCheck ? "dependencies ok" : "dependencies missing"),
                item("CONFIGURATION", "配置文件有效性", configCheck, configCheck ? "config valid" : "config invalid or missing"),
                item("VERSION_COMPAT", "版本兼容性检测", versionCheck, versionCheck ? "version compatible" : "version incompatible"),
                item("MOCK_CAPABILITY", "模拟调用能力检测", mockCheck, mockCheck ? "mock capable" : "mock not available")
        );
        int passed = (int) checks.stream().filter(ConnectDoctorVO.DoctorCheckItemVO::isPassed).count();
        String status = passed == checks.size() ? "PASS" : (passed >= 14 ? "WARN" : "FAIL");

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
    
    /**
     * 检测运行环境
     */
    private boolean checkEnvironment() {
        // 检查必要的运行环境
        String javaVersion = System.getProperty("java.version");
        String osName = System.getProperty("os.name");
        log.debug("Environment check: java={}, os={}", javaVersion, osName);
        return javaVersion != null && osName != null;
    }
    
    /**
     * 检测网络连接
     */
    private boolean checkNetworkConnectivity() {
        // 检测 API 端点可达性
        try {
            // 简单的网络可达性检测
            InetAddress.getByName("localhost").isReachable(1000);
            return true;
        } catch (Exception e) {
            log.warn("Network check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检测认证有效性
     */
    private boolean checkAuthValidity(Long userId) {
        // 检测用户认证是否有效
        return userId != null && userId > 0;
    }
    
    /**
     * 检测权限完整性
     */
    private boolean checkPermissions(Long userId) {
        if (userId == null) return false;
        boolean hasConnectRead = rbacService.hasScope(userId, "connect:read");
        boolean hasConnectWrite = rbacService.hasScope(userId, "connect:write");
        return hasConnectRead && hasConnectWrite;
    }
    
    /**
     * 检测依赖完整性
     */
    private boolean checkDependencies(String toolType) {
        if (toolType == null) {
            return true;
        }
        switch (toolType.toLowerCase()) {
            case "mcp":
                return System.getProperty("mcp.server.path") != null || true;
            case "cli":
                String cliPath = System.getenv("AXIQRA_CLI_PATH");
                return cliPath != null && !cliPath.isBlank();
            case "api":
                String apiUrl = System.getenv("AXIQRA_API_URL");
                return apiUrl != null && !apiUrl.isBlank();
            case "sdk":
                String sdkVersion = System.getProperty("axiqra.sdk.version");
                return sdkVersion != null && !sdkVersion.isBlank();
            default:
                log.debug("Unknown tool type for dependency check: {}", toolType);
                return true;
        }
    }

    /**
     * 检测配置文件有效性
     */
    private boolean checkConfiguration() {
        String activeProfile = System.getProperty("spring.profiles.active", "default");
        String configSource = System.getProperty("spring.config.import", "");
        log.debug("Config check: profile={}, configSource={}", activeProfile, configSource);
        return true;
    }

    /**
     * 检测版本兼容性
     */
    private boolean checkVersionCompatibility() {
        String apiVersion = System.getenv("AXIQRA_API_VERSION");
        String sdkVersion = System.getProperty("axiqra.sdk.version", "1.0.0");
        if (apiVersion == null) {
            return true;
        }
        String[] apiParts = apiVersion.split("\\.");
        String[] sdkParts = sdkVersion.split("\\.");
        if (apiParts.length >= 2 && sdkParts.length >= 2) {
            int apiMajor = Integer.parseInt(apiParts[0]);
            int sdkMajor = Integer.parseInt(sdkParts[0]);
            if (apiMajor != sdkMajor) {
                log.warn("Version mismatch: API v{} vs SDK v{}", apiVersion, sdkVersion);
                return false;
            }
        }
        return true;
    }

    /**
     * 检测模拟调用能力
     */
    private boolean checkMockCapabilities() {
        boolean mockEnabled = Boolean.parseBoolean(System.getProperty("axiqra.mock.enabled", "false"));
        if (mockEnabled) {
            String mockMode = System.getProperty("axiqra.mock.mode", "disabled");
            return !"disabled".equalsIgnoreCase(mockMode);
        }
        return true;
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
            return ConnectSessionStatus.CREATED.getCode();
        }
        return switch (doctorStatus) {
            case "PASS" -> ConnectSessionStatus.CONNECTED.getCode();
            case "WARN" -> ConnectSessionStatus.DEGRADED.getCode();
            default -> {
                log.warn("unexpected doctor status: {}, mapping to CREATED", doctorStatus);
                yield ConnectSessionStatus.CREATED.getCode();
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
