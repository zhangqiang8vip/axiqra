# Cursor 接入 Axiqra

本目录包含 Cursor IDE 接入 Axiqra 所需的配置和说明。

## 前置条件

1. 安装 Axiqra CLI: `npm install -g axiqra-cli`
2. 登录 Axiqra: `axiqra login`
3. Cursor 版本支持 MCP

## 安装步骤

### 方式一：复制 .cursorrules（推荐）

1. 复制 `.cursorrules` 文件到项目根目录
2. 复制 `CLAUDE.md` 到项目根目录
3. Cursor 会自动识别并加载规则

### 方式二：导入到 Cursor 设置

1. 打开 Cursor 设置 (Cmd/Ctrl + ,)
2. 进入 Rules 页面
3. 将 `.cursorrules` 的内容粘贴到规则编辑器

## MCP 配置

在 Cursor 中配置 Axiqra MCP Server：

1. 打开 Cursor 设置
2. 进入 MCP 页面
3. 添加新服务器：

```json
{
  "mcpServers": {
    "axiqra": {
      "command": "npx",
      "args": ["-y", "@axiqra/mcp-server"],
      "env": {
        "AXIQRA_API_KEY": "your-api-key"
      }
    }
  }
}
```

或者使用 CLI：

```bash
axiqra connect cursor
```

## 验证安装

在 Cursor 中执行以下命令验证：

```
Search for how to handle Redis connection pool exhaustion in Spring Boot
```

应该能看到 Axiqra 返回的搜索结果。

## 权限说明

使用 Axiqra 需要以下权限：

- `search:read` - 搜索历史方案
- `solution:read` - 读取方案详情
- `trace:write` - 提交工程轨迹
- `feedback:write` - 提交反馈

## 常见问题

### Q: MCP 工具没有响应

检查：
1. API Key 是否正确配置
2. 网络是否可达
3. 运行 `axiqra doctor` 诊断

### Q: 搜索结果为空

可能原因：
1. 工作空间没有历史数据
2. 搜索词不够精确
3. 没有匹配的技术栈

### Q: 无法提交轨迹

检查：
1. 是否已登录 (`axiqra whoami`)
2. Token 是否过期
3. 轨迹格式是否正确

## 更多信息

- [Axiqra CLI 文档](../cli/README.md)
- [Axiqra MCP Server 文档](../mcp/README.md)
- [D09 接入协议文档](../../../docs/_docs/09-AI工具接入、对话式自动接入、MCP、API、CLI与插件协议.md)
