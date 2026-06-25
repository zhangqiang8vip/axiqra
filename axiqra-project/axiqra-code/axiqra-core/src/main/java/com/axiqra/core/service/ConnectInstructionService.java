package com.axiqra.core.service;

import com.axiqra.common.domain.vo.ConnectInstructionVO;

/**
 * 对话式接入指令服务接口
 *
 * 生成可被 AI Agent 安装的接入指令和配置
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
public interface ConnectInstructionService {

    /**
     * 生成 Skill Pack 格式的接入指令
     *
     * @param sessionId 会话 ID
     * @param workspaceId 工作空间 ID
     * @return Skill Pack 格式的指令
     */
    ConnectInstructionVO generateSkillPack(String sessionId, Long workspaceId);

    /**
     * 生成 MCP 配置
     *
     * @param sessionId 会话 ID
     * @param workspaceId 工作空间 ID
     * @return MCP JSON 配置
     */
    ConnectInstructionVO generateMcpConfig(String sessionId, Long workspaceId);

    /**
     * 生成 CLI 安装脚本
     *
     * @param sessionId 会话 ID
     * @param workspaceId 工作空间 ID
     * @return CLI 安装脚本
     */
    ConnectInstructionVO generateCliScript(String sessionId, Long workspaceId);

    /**
     * 生成 API 接入配置
     *
     * @param sessionId 会话 ID
     * @param workspaceId 工作空间 ID
     * @return API 配置信息
     */
    ConnectInstructionVO generateApiConfig(String sessionId, Long workspaceId);

    /**
     * 生成完整的对话式接入指令包
     *
     * @param sessionId 会话 ID
     * @param workspaceId 工作空间 ID
     * @return 完整的接入指令包
     */
    ConnectInstructionVO generateFullPackage(String sessionId, Long workspaceId);
}
