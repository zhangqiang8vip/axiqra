package com.axiqra.common.domain.enums;

import java.util.Set;

/**
 * Connect 会话状态枚举
 * 
 * 对应 D09 文档第 9 节定义的 11 个状态：
 * <ul>
 *   <li>created - 用户在平台创建接入会话</li>
 *   <li>instruction_copied - 用户复制接入指令</li>
 *   <li>tool_started - AI 工具开始执行接入</li>
 *   <li>doctor_running - doctor 检测中</li>
 *   <li>connected - 接入成功</li>
 *   <li>degraded - 部分能力不可用</li>
 *   <li>failed - 接入失败</li>
 *   <li>revoked - 授权撤回</li>
 *   <li>expired - 会话过期</li>
 * </ul>
 * 
 * 入口动作：create_connect_session, copy_instruction, run_install_or_config, doctor_check
 * 出口动作：doctor_passed, doctor_failed, token_rotated, revoked, timeout, repair, retry
 */
public enum ConnectSessionStatus {
    
    /** 用户在平台创建接入会话 */
    CREATED("created", "已创建"),
    
    /** 用户已复制接入指令 */
    INSTRUCTION_COPIED("instruction_copied", "指令已复制"),
    
    /** AI 工具开始执行接入 */
    TOOL_STARTED("tool_started", "工具已启动"),
    
    /** doctor 检测中 */
    DOCTOR_RUNNING("doctor_running", "诊断中"),
    
    /** 接入成功 */
    CONNECTED("connected", "已连接"),
    
    /** 部分能力不可用（降级） */
    DEGRADED("degraded", "部分能力不可用"),
    
    /** 接入失败 */
    FAILED("failed", "接入失败"),
    
    /** 授权已撤回 */
    REVOKED("revoked", "已撤回"),
    
    /** 会话已过期 */
    EXPIRED("expired", "已过期");
    
    private final String code;
    private final String description;
    
    ConnectSessionStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static ConnectSessionStatus of(String code) {
        if (code == null) {
            return null;
        }
        for (ConnectSessionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * 状态流转规则定义
     * 
     * @return 允许的下一状态集合
     */
    public Set<ConnectSessionStatus> getAllowedTransitions() {
        return switch (this) {
            case CREATED -> Set.of(INSTRUCTION_COPIED, EXPIRED);
            case INSTRUCTION_COPIED -> Set.of(TOOL_STARTED, EXPIRED);
            case TOOL_STARTED -> Set.of(DOCTOR_RUNNING, FAILED, EXPIRED);
            case DOCTOR_RUNNING -> Set.of(CONNECTED, DEGRADED, FAILED, EXPIRED);
            case CONNECTED -> Set.of(CONNECTED, DEGRADED, REVOKED, EXPIRED);
            case DEGRADED -> Set.of(CONNECTED, DEGRADED, REVOKED, EXPIRED);
            case FAILED -> Set.of(TOOL_STARTED, CREATED);
            case REVOKED -> Set.of();
            case EXPIRED -> Set.of(CREATED);
        };
    }
    
    /**
     * 检查是否可以流转到目标状态
     * 
     * @param target 目标状态
     * @return 是否允许流转
     */
    public boolean canTransitionTo(ConnectSessionStatus target) {
        return getAllowedTransitions().contains(target);
    }
    
    /**
     * 是否是终态（不可继续流转）
     * 
     * @return 是否终态
     */
    public boolean isTerminal() {
        return this == REVOKED || this == EXPIRED;
    }
    
    /**
     * 是否是成功状态
     * 
     * @return 是否成功
     */
    public boolean isSuccess() {
        return this == CONNECTED || this == DEGRADED;
    }
    
    /**
     * 是否需要用户重新授权
     * 
     * @return 是否需要重新授权
     */
    public boolean needsReauthorization() {
        return this == REVOKED || this == EXPIRED;
    }
}
