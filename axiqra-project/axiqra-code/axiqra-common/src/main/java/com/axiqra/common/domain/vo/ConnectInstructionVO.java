package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话式接入指令 VO
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectInstructionVO {

    /** 指令类型: skill_pack / mcp_config / cli_script / api_config / full_package */
    private String instructionType;

    /** 接入方式 */
    private String accessMethod;

    /** 主要内容/脚本/配置 */
    private String content;

    /** 安装说明 */
    private String installInstructions;

    /** 使用说明 */
    private String usageInstructions;

    /** 兼容声明 */
    private String compatibilityNote;

    /** 回退路径说明 */
    private String fallbackInstructions;

    /** 生成时间戳 */
    private Long generatedAt;

    /** 版本信息 */
    private String version;
}
