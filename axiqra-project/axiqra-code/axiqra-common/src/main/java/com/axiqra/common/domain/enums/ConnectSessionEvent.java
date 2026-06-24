package com.axiqra.common.domain.enums;

/**
 * Connect 会话事件枚举
 * 
 * 对应 D09 文档第 9 节定义的事件：
 * 入口动作：create_connect_session, copy_instruction, run_install_or_config, doctor_check, doctor_passed, doctor_failed, token_rotated, revoke, timeout, repair, retry
 */
public enum ConnectSessionEvent {
    
    /** 创建会话 */
    CREATE_CONNECT_SESSION("create_connect_session", "创建接入会话"),
    
    /** 复制指令 */
    COPY_INSTRUCTION("copy_instruction", "复制接入指令"),
    
    /** 工具启动 */
    RUN_INSTALL_OR_CONFIG("run_install_or_config", "运行安装或配置"),
    
    /** 开始诊断 */
    DOCTOR_CHECK("doctor_check", "开始诊断检查"),
    
    /** 诊断通过 */
    DOCTOR_PASSED("doctor_passed", "诊断通过"),
    
    /** 诊断失败 */
    DOCTOR_FAILED("doctor_failed", "诊断失败"),
    
    /** Token 轮换 */
    TOKEN_ROTATED("token_rotated", "Token 已轮换"),
    
    /** 撤回授权 */
    REVOKE("revoke", "撤回授权"),
    
    /** 会话过期 */
    TIMEOUT("timeout", "会话超时"),
    
    /** 修复问题 */
    REPAIR("repair", "修复问题"),
    
    /** 重试 */
    RETRY("retry", "重试接入");
    
    private final String code;
    private final String description;
    
    ConnectSessionEvent(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static ConnectSessionEvent of(String code) {
        if (code == null) {
            return null;
        }
        for (ConnectSessionEvent event : values()) {
            if (event.code.equals(code)) {
                return event;
            }
        }
        return null;
    }
}
