package com.axiqra.core.service.impl;

import com.axiqra.common.domain.vo.ConnectInstructionVO;
import com.axiqra.core.service.ConnectInstructionService;
import com.axiqra.core.service.ConnectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 对话式接入指令服务实现
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectInstructionServiceImpl implements ConnectInstructionService {

    private final ConnectService connectService;

    @Value("${axiqra.api.base-url:https://api.axiqra.com}")
    private String apiBaseUrl;

    private static final String VERSION = "1.0.0";

    @Override
    public ConnectInstructionVO generateSkillPack(String sessionId, Long workspaceId) {
        String content = buildSkillPackContent(sessionId, workspaceId);
        return ConnectInstructionVO.builder()
                .instructionType("skill_pack")
                .accessMethod("cursor_skill")
                .content(content)
                .installInstructions(buildSkillPackInstallInstructions())
                .usageInstructions(buildSkillPackUsageInstructions())
                .compatibilityNote("Compatible with Cursor AI IDE v0.35+")
                .fallbackInstructions(buildSkillPackFallback())
                .generatedAt(System.currentTimeMillis())
                .version(VERSION)
                .build();
    }

    @Override
    public ConnectInstructionVO generateMcpConfig(String sessionId, Long workspaceId) {
        String content = buildMcpConfigContent(sessionId, workspaceId);
        return ConnectInstructionVO.builder()
                .instructionType("mcp_config")
                .accessMethod("mcp")
                .content(content)
                .installInstructions(buildMcpInstallInstructions())
                .usageInstructions(buildMcpUsageInstructions())
                .compatibilityNote("Compatible with Claude Desktop, Cursor, and other MCP clients")
                .fallbackInstructions(buildMcpFallback())
                .generatedAt(System.currentTimeMillis())
                .version(VERSION)
                .build();
    }

    @Override
    public ConnectInstructionVO generateCliScript(String sessionId, Long workspaceId) {
        String content = buildCliScriptContent(sessionId, workspaceId);
        return ConnectInstructionVO.builder()
                .instructionType("cli_script")
                .accessMethod("cli")
                .content(content)
                .installInstructions(buildCliInstallInstructions())
                .usageInstructions(buildCliUsageInstructions())
                .compatibilityNote("Compatible with macOS, Linux, and Windows (WSL)")
                .fallbackInstructions(buildCliFallback())
                .generatedAt(System.currentTimeMillis())
                .version(VERSION)
                .build();
    }

    @Override
    public ConnectInstructionVO generateApiConfig(String sessionId, Long workspaceId) {
        String content = buildApiConfigContent(sessionId, workspaceId);
        return ConnectInstructionVO.builder()
                .instructionType("api_config")
                .accessMethod("rest_api")
                .content(content)
                .installInstructions(buildApiInstallInstructions())
                .usageInstructions(buildApiUsageInstructions())
                .compatibilityNote("Compatible with any HTTP client supporting REST API")
                .fallbackInstructions(buildApiFallback())
                .generatedAt(System.currentTimeMillis())
                .version(VERSION)
                .build();
    }

    @Override
    public ConnectInstructionVO generateFullPackage(String sessionId, Long workspaceId) {
        StringBuilder content = new StringBuilder();
        content.append("# Axiqra Full Integration Package\n\n");
        content.append("## Version: ").append(VERSION).append("\n\n");
        content.append("## Contents\n\n");
        content.append("1. MCP Configuration\n");
        content.append("2. CLI Installation Script\n");
        content.append("3. REST API Configuration\n");
        content.append("4. Skill Pack (for Cursor)\n\n");
        content.append("## Session ID\n").append(sessionId).append("\n");
        content.append("## Workspace ID\n").append(workspaceId).append("\n\n");
        content.append("---\n\n");
        content.append("### MCP Configuration\n\n");
        content.append(buildMcpConfigContent(sessionId, workspaceId));
        content.append("\n\n---\n\n");
        content.append("### CLI Installation\n\n");
        content.append(buildCliScriptContent(sessionId, workspaceId));
        content.append("\n\n---\n\n");
        content.append("### API Configuration\n\n");
        content.append(buildApiConfigContent(sessionId, workspaceId));

        return ConnectInstructionVO.builder()
                .instructionType("full_package")
                .accessMethod("all")
                .content(content.toString())
                .installInstructions(buildFullPackageInstallInstructions())
                .usageInstructions(buildFullPackageUsageInstructions())
                .compatibilityNote("All integration methods included")
                .fallbackInstructions(buildFullPackageFallback())
                .generatedAt(System.currentTimeMillis())
                .version(VERSION)
                .build();
    }

    // ========== Skill Pack 生成 ==========

    private String buildSkillPackContent(String sessionId, Long workspaceId) {
        return "# Axiqra Integration Skill Pack\n" +
               "version: 1.0.0\n" +
               "session_id: " + sessionId + "\n" +
               "workspace_id: " + (workspaceId != null ? workspaceId : "default") + "\n\n" +
               "## Capabilities\n" +
               "- Search Solutions\n" +
               "- Get Case Details\n" +
               "- Submit Engineering Traces\n" +
               "- Report Feedback\n\n" +
               "## Usage\n" +
               "Ask Axiqra to search for solutions using natural language.";
    }

    private String buildSkillPackInstallInstructions() {
        return "1. Copy the skill pack content above\n" +
               "2. Create a new file: ~/.cursor/skills/axiqra.yaml\n" +
               "3. Paste the content into the file\n" +
               "4. Restart Cursor IDE";
    }

    private String buildSkillPackUsageInstructions() {
        return "Use Axiqra skills in your conversations:\n" +
               "- \"Search for OAuth2 login solutions\"\n" +
               "- \"Find database migration examples\"\n" +
               "- \"Get help with CI/CD setup\"";
    }

    private String buildSkillPackFallback() {
        return "If Skill Pack installation fails, use MCP configuration as fallback.";
    }

    // ========== MCP 配置生成 ==========

    private String buildMcpConfigContent(String sessionId, Long workspaceId) {
        String wsId = workspaceId != null ? String.valueOf(workspaceId) : "default";
        return "{\n" +
               "  \"mcpServers\": {\n" +
               "    \"axiqra\": {\n" +
               "      \"command\": \"npx\",\n" +
               "      \"args\": [\"-y\", \"@axiqra/mcp-client\"],\n" +
               "      \"env\": {\n" +
               "        \"AXIQRA_SESSION_ID\": \"" + sessionId + "\",\n" +
               "        \"AXIQRA_WORKSPACE_ID\": \"" + wsId + "\",\n" +
               "        \"AXIQRA_API_URL\": \"" + apiBaseUrl + "/api/v1\",\n" +
               "        \"AXIQRA_API_KEY\": \"${AXIQRA_API_KEY}\"\n" +
               "      }\n" +
               "    }\n" +
               "  }\n" +
               "}";
    }

    private String buildMcpInstallInstructions() {
        return "1. Install Node.js 18+ if not already installed\n" +
               "2. Install the Axiqra MCP client:\n" +
               "   npm install -g @axiqra/mcp-client\n" +
               "3. Add the MCP configuration to your MCP settings\n" +
               "4. Set your API key as environment variable AXIQRA_API_KEY";
    }

    private String buildMcpUsageInstructions() {
        return "Tools available via MCP:\n" +
               "- axiqra_search: Search for solutions\n" +
               "- axiqra_get_case: Get case details\n" +
               "- axiqra_submit_trace: Submit engineering trace\n" +
               "- axiqra_report_feedback: Report solution feedback";
    }

    private String buildMcpFallback() {
        return "If MCP installation fails, use CLI or REST API as fallback.";
    }

    // ========== CLI 脚本生成 ==========

    private String buildCliScriptContent(String sessionId, Long workspaceId) {
        String wsId = workspaceId != null ? String.valueOf(workspaceId) : "default";
        return "#!/bin/bash\n" +
               "# Axiqra CLI Installation Script\n" +
               "# Version: " + VERSION + "\n\n" +
               "set -e\n\n" +
               "# Configuration\n" +
               "SESSION_ID=\"" + sessionId + "\"\n" +
               "WORKSPACE_ID=\"" + wsId + "\"\n" +
               "API_URL=\"" + apiBaseUrl + "/api/v1\"\n\n" +
               "echo \"Installing Axiqra CLI...\"\n\n" +
               "# Download and install CLI\n" +
               "if command -v npm &> /dev/null; then\n" +
               "    npm install -g @axiqra/cli\n" +
               "elif command -v pip &> /dev/null; then\n" +
               "    pip install axiqra-cli\n" +
               "else\n" +
               "    echo \"Error: npm or pip required for installation\"\n" +
               "    exit 1\n" +
               "fi\n\n" +
               "# Configure CLI\n" +
               "axiqra config set session-id \"$SESSION_ID\"\n" +
               "axiqra config set workspace-id \"$WORKSPACE_ID\"\n" +
               "axiqra config set api-url \"$API_URL\"\n\n" +
               "echo \"Installation complete!\"\n" +
               "echo \"Run 'axiqra search <query>' to get started.\"";
    }

    private String buildCliInstallInstructions() {
        return "1. Ensure npm or pip is installed\n" +
               "2. Run the installation script above\n" +
               "3. Set AXIQRA_API_KEY environment variable\n" +
               "4. Run 'axiqra doctor' to verify installation";
    }

    private String buildCliUsageInstructions() {
        return "CLI Commands:\n" +
               "- axiqra search <query>   : Search solutions\n" +
               "- axiqra case <id>        : Get case details\n" +
               "- axiqra trace submit     : Submit trace\n" +
               "- axiqra feedback report  : Report feedback\n" +
               "- axiqra doctor           : Run diagnostics";
    }

    private String buildCliFallback() {
        return "If CLI installation fails, use REST API directly.";
    }

    // ========== API 配置生成 ==========

    private String buildApiConfigContent(String sessionId, Long workspaceId) {
        String wsId = workspaceId != null ? String.valueOf(workspaceId) : "default";
        return "{\n" +
               "  \"api_endpoint\": \"" + apiBaseUrl + "/api/v1/search\",\n" +
               "  \"session_id\": \"" + sessionId + "\",\n" +
               "  \"workspace_id\": \"" + wsId + "\",\n" +
               "  \"headers\": {\n" +
               "    \"Authorization\": \"Bearer ${AXIQRA_API_KEY}\",\n" +
               "    \"Content-Type\": \"application/json\",\n" +
               "    \"X-Axiqra-Session-Id\": \"" + sessionId + "\"\n" +
               "  },\n" +
               "  \"example_request\": {\n" +
               "    \"query\": \"OAuth2 login implementation\",\n" +
               "    \"workspace_id\": \"" + wsId + "\",\n" +
               "    \"limit\": 10\n" +
               "  }\n" +
               "}";
    }

    private String buildApiInstallInstructions() {
        return "1. Set AXIQRA_API_KEY environment variable\n" +
               "2. Use the API endpoint provided above\n" +
               "3. Include session ID in headers for session tracking";
    }

    private String buildApiUsageInstructions() {
        return "API Endpoints:\n" +
               "- POST /api/v1/search          : Search solutions\n" +
               "- GET  /api/v1/cases/{id}      : Get case details\n" +
               "- POST /api/v1/traces           : Submit trace\n" +
               "- POST /api/v1/feedback         : Report feedback";
    }

    private String buildApiFallback() {
        return "If API access is restricted, use CLI as fallback.";
    }

    // ========== 完整包 ==========

    private String buildFullPackageInstallInstructions() {
        return "1. Choose your preferred integration method:\n" +
               "   - MCP (recommended for Claude/Cursor)\n" +
               "   - CLI (recommended for terminal usage)\n" +
               "   - REST API (for custom integrations)\n" +
               "2. Follow the installation instructions for your chosen method\n" +
               "3. Run 'axiqra doctor' or MCP diagnostics to verify";
    }

    private String buildFullPackageUsageInstructions() {
        return "Start by running diagnostics:\n" +
               "- CLI: axiqra doctor\n" +
               "- MCP: Use health check tool\n\n" +
               "Then search for solutions:\n" +
               "- CLI: axiqra search \"your query\"\n" +
               "- MCP: Use axiqra_search tool\n" +
               "- API: POST to /api/v1/search";
    }

    private String buildFullPackageFallback() {
        return "If all methods fail, use the web interface at " + apiBaseUrl;
    }
}
