# Axiqra Connect CLI

Axiqra 命令行工具，用于 AI Agent 通过 CLI 方式接入 Axiqra。

## 安装

### Windows

```powershell
# 一键安装
powershell -ExecutionPolicy Bypass -Command "irm https://install.axiqra.com/connect.ps1 | iex"
```

### macOS / Linux

```bash
# 一键安装
curl -fsSL https://install.axiqra.com/connect.sh | bash
```

### npm 安装

```bash
npm install -g @axiqra/cli
```

## 登录

```bash
# 交互式登录
axiqra login

# API Key 登录
axiqra login --api-key <your-api-key>

# 查看登录状态
axiqra whoami
```

## 初始化

```bash
# 初始化接入
axiqra connect init

# 指定渠道和工具类型
axiqra connect init --channel cli --tool-type claude_code

# 指定工作空间
axiqra connect init --workspace ws-xxx
```

## Doctor 检测

```bash
# 全部检查
axiqra doctor

# 单项检查
axiqra doctor --check network
axiqra doctor --check auth
axiqra doctor --check quota
axiqra doctor --check version
axiqra doctor --check config
axiqra doctor --check storage

# 详细输出
axiqra doctor --verbose
```

## 搜索

```bash
# 基本搜索
axiqra search "Spring Boot Redis 配置"

# 指定标签
axiqra search --tag java "连接池"

# 指定验证等级
axiqra search --level L3 "微服务架构"

# 最大结果数
axiqra search --max 10 "K8s 部署"
```

## Solution

```bash
# 获取 Solution 详情
axiqra solution get SOL-xxx

# 指定视图模式
axiqra solution get SOL-xxx --view execution
axiqra solution get SOL-xxx --view full
axiqra solution get SOL-xxx --view metadata
```

## 轨迹

```bash
# 提交轨迹
axiqra trace submit ./trace.json

# 提交并标记
axiqra trace submit ./trace.json --tag bug-fix --tag production

# 查看待提交轨迹
axiqra trace list --status pending

# 查看轨迹详情
axiqra trace get TRACE-xxx

# 删除轨迹
axiqra trace delete TRACE-xxx
```

## 反馈

```bash
# 方案有效
axiqra feedback INV-xxx worked

# 方案无效
axiqra feedback INV-xxx failed --reason "版本不匹配"

# 部分有效
axiqra feedback INV-xxx partial --reason "配置需要调整"

# 不适用
axiqra feedback INV-xxx not_applicable --reason "场景不匹配"
```

## Seed

```bash
# 创建候选 Seed
axiqra seed create "K8s 有状态服务部署"

# 查看 Seed 列表
axiqra seed list

# 查看 Seed 详情
axiqra seed get SEED-xxx
```

## 会话

```bash
# 查看会话列表
axiqra sessions list

# 查看会话详情
axiqra sessions get SESSION-xxx

# 删除会话
axiqra sessions delete SESSION-xxx
```

## 配额

```bash
# 查看配额状态
axiqra quota

# 查看详细配额信息
axiqra quota --detail
```

## 配置

```bash
# 查看所有配置
axiqra config list

# 设置配置项
axiqra config set api-url https://api.axiqra.com
axiqra config set api-key <your-key>
axiqra config set log-level debug

# 获取配置项
axiqra config get api-url

# 重置配置
axiqra config reset
```

## 全局选项

```bash
# 显示帮助
axiqra --help
axiqra <command> --help

# 显示版本
axiqra --version

# 指定配置文件
axiqra --config ~/.axiqra/config.yaml <command>

# 详细输出
axiqra --verbose <command>

# 输出格式
axiqra --output json <command>
axiqra --output yaml <command>
```

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `AXIQRA_API_URL` | API 地址 | `https://api.axiqra.com` |
| `AXIQRA_API_KEY` | API Key | - |
| `AXIQRA_WORKSPACE_ID` | 工作空间 ID | - |
| `AXIQRA_CONFIG_PATH` | 配置文件路径 | `~/.axiqra/config.yaml` |
| `AXIQRA_LOG_LEVEL` | 日志级别 | `info` |

## 退出码

| 退出码 | 说明 |
|--------|------|
| `0` | 成功 |
| `1` | 一般错误 |
| `2` | 配置错误 |
| `3` | 认证错误 |
| `4` | 配额超限 |
| `5` | 限流 |

## 配置文件

默认配置文件: `~/.axiqra/config/config.yaml`

```yaml
api:
  url: https://api.axiqra.com
  timeout: 30
  retry: 3

cli:
  install_path: ~/.axiqra
  log_level: info

connect:
  default_channel: cli
  default_tool_type: custom
  auto_doctor: true

paths:
  config: ~/.axiqra/config/config.yaml
  cache: ~/.axiqra/cache
  data: ~/.axiqra/data
  logs: ~/.axiqra/logs
```

## 故障排除

### Doctor 检测失败

```bash
# 查看详细日志
axiqra doctor --verbose

# 检查网络
axiqra doctor --check network

# 检查认证
axiqra doctor --check auth
```

### API 请求失败

```bash
# 检查 API 地址
axiqra config get api-url

# 测试 API 连接
curl -I https://api.axiqra.com
```

### 配额超限

```bash
# 查看配额
axiqra quota

# 每日配额 2000 次
# UTC 0:00 重置
```

## 相关链接

- [Axiqra 文档](https://docs.axiqra.com)
- [MCP 协议](https://modelcontextprotocol.io)
- [GitHub](https://github.com/axiqra/axiqra)
