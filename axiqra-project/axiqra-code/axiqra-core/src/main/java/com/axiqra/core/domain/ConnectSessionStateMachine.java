package com.axiqra.core.domain;

import com.axiqra.common.domain.enums.ConnectSessionEvent;
import com.axiqra.common.domain.enums.ConnectSessionStatus;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;

/**
 * Connect 会话状态机
 * 
 * 处理状态流转逻辑，根据事件驱动状态变更。
 */
@Slf4j
public class ConnectSessionStateMachine {
    
    /**
     * 处理状态流转
     * 
     * @param currentStatus 当前状态
     * @param event 触发事件
     * @return 下一状态
     * @throws BizException 如果状态流转不合法
     */
    public static ConnectSessionStatus transition(ConnectSessionStatus currentStatus, ConnectSessionEvent event) {
        if (currentStatus == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "当前状态不能为空");
        }
        if (event == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "事件不能为空");
        }
        
        ConnectSessionStatus nextStatus = calculateNextStatus(currentStatus, event);
        
        if (nextStatus == null) {
            log.warn("非法状态流转: currentStatus={}, event={}", currentStatus, event);
            throw new BizException(ErrorCode.PARAM_INVALID, 
                    String.format("状态 %s 不支持事件 %s", currentStatus.getDescription(), event.getDescription()));
        }
        
        log.info("状态流转: {} --[{}]--> {}", currentStatus.getCode(), event.getCode(), nextStatus.getCode());
        return nextStatus;
    }
    
    /**
     * 计算下一状态（不抛异常）
     * 
     * @param currentStatus 当前状态
     * @param event 触发事件
     * @return 下一状态，如果不允许流转则返回 null
     */
    public static ConnectSessionStatus calculateNextStatus(ConnectSessionStatus currentStatus, ConnectSessionEvent event) {
        return switch (currentStatus) {
            case CREATED -> switch (event) {
                case COPY_INSTRUCTION -> ConnectSessionStatus.INSTRUCTION_COPIED;
                case TIMEOUT -> ConnectSessionStatus.EXPIRED;
                default -> null;
            };
            case INSTRUCTION_COPIED -> switch (event) {
                case RUN_INSTALL_OR_CONFIG -> ConnectSessionStatus.TOOL_STARTED;
                case TIMEOUT -> ConnectSessionStatus.EXPIRED;
                default -> null;
            };
            case TOOL_STARTED -> switch (event) {
                case DOCTOR_CHECK -> ConnectSessionStatus.DOCTOR_RUNNING;
                case TIMEOUT -> ConnectSessionStatus.EXPIRED;
                default -> null;
            };
            case DOCTOR_RUNNING -> switch (event) {
                case DOCTOR_PASSED -> ConnectSessionStatus.CONNECTED;
                case DOCTOR_FAILED -> ConnectSessionStatus.FAILED;
                case TIMEOUT -> ConnectSessionStatus.EXPIRED;
                default -> null;
            };
            case CONNECTED -> switch (event) {
                case TOKEN_ROTATED -> ConnectSessionStatus.CONNECTED;
                case REVOKE -> ConnectSessionStatus.REVOKED;
                case TIMEOUT -> ConnectSessionStatus.EXPIRED;
                case DOCTOR_CHECK -> ConnectSessionStatus.DOCTOR_RUNNING;
                default -> null;
            };
            case DEGRADED -> switch (event) {
                case DOCTOR_PASSED -> ConnectSessionStatus.CONNECTED;
                case REPAIR -> ConnectSessionStatus.CONNECTED;
                case REVOKE -> ConnectSessionStatus.REVOKED;
                case TIMEOUT -> ConnectSessionStatus.EXPIRED;
                default -> null;
            };
            case FAILED -> switch (event) {
                case RETRY -> ConnectSessionStatus.TOOL_STARTED;
                case CREATE_CONNECT_SESSION -> ConnectSessionStatus.CREATED;
                default -> null;
            };
            case REVOKED, EXPIRED -> null;
        };
    }
    
    /**
     * 检查会话是否过期
     * 
     * @param status 当前状态
     * @param expiresAt 过期时间
     * @return 是否过期
     */
    public static boolean isExpired(ConnectSessionStatus status, OffsetDateTime expiresAt) {
        if (status == null || expiresAt == null) {
            return false;
        }
        return OffsetDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * 检查是否需要自动过期
     * 
     * @param currentStatus 当前状态
     * @param expiresAt 过期时间
     * @return 是否应该触发过期事件
     */
    public static boolean shouldAutoExpire(ConnectSessionStatus currentStatus, OffsetDateTime expiresAt) {
        if (currentStatus.isTerminal()) {
            return false;
        }
        return isExpired(currentStatus, expiresAt);
    }
    
    /**
     * 获取状态描述
     * 
     * @param status 状态
     * @return 用户友好的描述
     */
    public static String getStatusDescription(ConnectSessionStatus status) {
        if (status == null) {
            return "未知状态";
        }
        return switch (status) {
            case CREATED -> "会话已创建，请复制接入指令";
            case INSTRUCTION_COPIED -> "指令已复制，请在 AI 工具中粘贴";
            case TOOL_STARTED -> "AI 工具已开始安装配置";
            case DOCTOR_RUNNING -> "正在检测接入状态...";
            case CONNECTED -> "接入成功，可以开始使用";
            case DEGRADED -> "部分能力不可用，建议检查配置";
            case FAILED -> "接入失败，请重试或检查配置";
            case REVOKED -> "授权已撤回，请重新授权";
            case EXPIRED -> "会话已过期，请重新创建";
        };
    }
    
    /**
     * 获取下一步建议
     * 
     * @param status 当前状态
     * @return 建议的下一步操作
     */
    public static String getNextHint(ConnectSessionStatus status) {
        if (status == null) {
            return "请创建新的接入会话";
        }
        return switch (status) {
            case CREATED -> "请复制接入指令到剪贴板";
            case INSTRUCTION_COPIED -> "请在 AI 工具中粘贴并执行接入指令";
            case TOOL_STARTED -> "等待工具完成安装和配置...";
            case DOCTOR_RUNNING -> "请等待诊断完成";
            case CONNECTED -> "可以开始在 AI 工具中使用 Axiqra";
            case DEGRADED -> "建议运行 axiqra doctor 检查问题";
            case FAILED -> "建议运行 axiqra doctor 排查问题，或创建新会话";
            case REVOKED -> "请重新运行 axiqra login 授权";
            case EXPIRED -> "请重新运行 axiqra connect 创建会话";
        };
    }
}
