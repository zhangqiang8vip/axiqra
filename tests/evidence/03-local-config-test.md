# Cursor Axiqra MCP 本地配置测试

## 测试时间
2026-06-25 17:52 UTC+8

## MCP Server Stub 配置

已创建本地 MCP Server Stub:
- 路径: `temp/axiqra-mcp-blackbox/cursor/axiqra-mcp-server-stub.mjs`
- 功能: 模拟 Axiqra MCP Server 的 7 个工具

## Cursor MCP 配置

已更新 `C:\Users\Administrator\.cursor\mcp.json`:

```json
{
  "mcpServers": {
    "axiqra": {
      "command": "node",
      "args": [
        "C:/Users/Administrator/.cursor/projects/e-ProjectMyNew-axiqra-tests/temp/axiqra-mcp-blackbox/cursor/axiqra-mcp-server-stub.mjs"
      ],
      "env": {
        "AXIQRA_API_KEY": "test-api-key-for-local-testing",
        "AXIQRA_API_URL": "http://localhost:8080/api",
        "AXIQRA_WORKSPACE_ID": "test-workspace"
      }
    }
  }
}
```

## Stub 测试结果

| 测试 | 结果 | 输出 |
|------|------|------|
| 初始化 | ✅ 通过 | 返回 protocolVersion, capabilities |
| 工具列表 | ✅ 通过 | 返回 7 个工具定义 |
| search_before_act | ✅ 通过 | 返回模拟搜索结果 |
| submit_trace | ✅ 通过 | 返回 trace_id |
| submit_feedback | ✅ 通过 | 返回 invocation_id |
| doctor | ✅ 通过 | 返回诊断结果 |

## 下一步

**请重启 Cursor 以加载 Axiqra MCP Server**

重启后请检查:
1. Cursor 设置 → MCP → Axiqra 状态为 "Running"
2. Axiqra 工具出现在 MCP 工具列表中
3. 可以调用三个核心工具:
   - `axiqra.search_before_act`
   - `axiqra.submit_trace`
   - `axiqra.submit_feedback`

## 验证命令

重启 Cursor 后，可以执行以下测试任务:

```
请帮我分析一个模拟后端问题：登录接口偶发 500。请先看看有没有历史经验可以参考，再给我排查方案，完成后记录一下这次分析过程和结果。
```

预期行为:
1. Agent 调用 `axiqra.search_before_act`
2. Agent 基于返回结果分析
3. Agent 调用 `axiqra.submit_trace` 提交轨迹
4. Agent 调用 `axiqra.submit_feedback` 提交反馈
