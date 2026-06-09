package com.axiqra.core.service;

import com.axiqra.common.domain.dto.ConnectSessionCreateRequest;
import com.axiqra.common.domain.vo.ConnectDoctorVO;
import com.axiqra.common.domain.vo.ConnectSessionVO;
import com.axiqra.common.exception.BizException;

import java.util.List;

public interface ConnectService {

    /**
     * 创建 Connect 会话。
     *
     * @throws BizException 当配额耗尽时抛出 {@code ErrorCode.QUOTA_EXCEEDED}，当请求触发限流时抛出 {@code ErrorCode.RATE_LIMITED}
     */
    ConnectSessionVO createSession(Long userId, ConnectSessionCreateRequest request);

    /**
     * 执行 Connect 诊断检查，用于验证当前用户、渠道、工具类型和空间是否满足接入前置条件。
     *
     * @param userId 当前用户 ID，需为非 null 且大于 0
     * @param channel 接入渠道标识，当前实现要求非空字符串，例如 {@code cli}/{@code web}/{@code api}
     * @param toolType 工具类型标识，当前实现要求非空字符串，例如 {@code mcp}/{@code connector}/{@code auth}
     * @param workspaceId 目标空间 ID，可为 null；非 null 时会校验用户是否具备空间访问权限
     * @return 诊断结果，包含总检查项、通过数和各检查项明细
     * @throws BizException 当 userId 无效导致未登录时抛出 {@code ErrorCode.UNAUTHORIZED}
     */
    ConnectDoctorVO runDoctor(Long userId, String channel, String toolType, Long workspaceId);

    /**
     * 查询会话详情。
     * <p>
     * 当底层 {@link com.axiqra.common.port.ConnectSessionPort#get(String)} 未找到会话，或会话存在但不属于当前用户时，
     * 当前实现统一抛出 {@code ErrorCode.CONNECT_SESSION_NOT_FOUND}，而不是返回 null。
     */
    ConnectSessionVO getSession(Long userId, String sessionId);

    List<ConnectSessionVO> listSessions(Long userId);
}
