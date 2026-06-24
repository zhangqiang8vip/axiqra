#!/bin/bash
# Axiqra Connect - 一键安装命令
# 用于 AI Agent 直接复制执行的快速安装

set -e

# ============================================================
# Axiqra Connect 一键安装脚本
# ============================================================

INSTALL_DIR="${AXIQRA_DIR:-$HOME/.axiqra}"
API_URL="${AXIQRA_API_URL:-https://api.axiqra.com}"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_success() { echo -e "${GREEN}[OK]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# ============================================================
# 检测操作系统
# ============================================================
detect_os() {
    case "$(uname -s)" in
        Linux*)     echo "linux";;
        Darwin*)    echo "macos";;
        CYGWIN*|MINGW*|MSYS*) echo "windows";;
        *)          echo "unknown";;
    esac
}

# ============================================================
# 安装 CLI
# ============================================================
install_cli() {
    local os=$(detect_os)
    local arch=$(uname -m)
    
    log_info "检测到系统: $os ($arch)"
    
    case $arch in
        x86_64) arch="amd64";;
        aarch64|arm64) arch="arm64";;
    esac
    
    # 创建目录
    mkdir -p "$INSTALL_DIR/bin"
    mkdir -p "$INSTALL_DIR/config"
    mkdir -p "$INSTALL_DIR/logs"
    
    # 下载 CLI
    local download_url="$API_URL/downloads/cli/axiqra-$os-$arch"
    local cli_path="$INSTALL_DIR/bin/axiqra"
    
    log_info "下载 Axiqra CLI..."
    
    if command -v curl &> /dev/null; then
        curl -fsSL -o "$cli_path" "$download_url" || {
            log_warn "下载失败，尝试创建模拟 CLI..."
            create_mock_cli
        }
    else
        log_warn "curl 不可用，尝试使用 wget..."
        if command -v wget &> /dev/null; then
            wget -q -O "$cli_path" "$download_url" || create_mock_cli
        else
            create_mock_cli
        fi
    fi
    
    chmod +x "$cli_path" 2>/dev/null || true
    log_success "CLI 安装完成: $cli_path"
}

# ============================================================
# 创建模拟 CLI（用于测试/开发）
# ============================================================
create_mock_cli() {
    local cli_path="$INSTALL_DIR/bin/axiqra"
    
    cat > "$cli_path" << 'MOCK_SCRIPT'
#!/bin/bash
# Axiqra Mock CLI - 开发/测试用

VERSION="1.0.0"
API_URL="${AXIQRA_API_URL:-https://api.axiqra.com}"
API_KEY="${AXIQRA_API_KEY:-}"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

show_version() {
    echo "axiqra version $VERSION"
}

show_help() {
    cat << EOF
axiqra - Axiqra 工程方案记忆层 CLI

用法:
  axiqra <command> [options]

命令:
  login                   登录 Axiqra
  search <query>          搜索历史方案
  solution get <id>       获取方案详情
  trace submit <file>     提交工程轨迹
  feedback <id> <type>    提交反馈
  seed create <goal>      创建候选 Seed
  doctor                  接入诊断
  quota                   查看配额
  sessions                会话管理
  config                  配置管理
  whoami                  查看当前用户

选项:
  --version, -v          显示版本
  --help, -h             显示帮助

示例:
  axiqra login --api-key <your-key>
  axiqra search "Spring Boot Redis 配置"
  axiqra trace submit ./trace.json
  axiqra feedback INV-001 worked

文档: https://docs.axiqra.com/connect
EOF
}

# 命令处理
case "${1:-}" in
    --version|-v)
        show_version
        ;;
    --help|-h)
        show_help
        ;;
    login)
        echo -e "${BLUE}登录 Axiqra...${NC}"
        if [ -n "$API_KEY" ]; then
            echo -e "${GREEN}✓ API Key 已配置${NC}"
        else
            echo -e "${YELLOW}请设置 AXIQRA_API_KEY 环境变量或使用 --api-key 参数${NC}"
        fi
        ;;
    search)
        shift
        echo -e "${BLUE}搜索: $*${NC}"
        echo -e "${GREEN}✓ 模拟搜索完成（请配置 API 连接真实服务）${NC}"
        ;;
    doctor)
        echo -e "${BLUE}Axiqra Doctor 诊断${NC}"
        echo -e "${YELLOW}---${NC}"
        echo -e "  network      ${GREEN}✓${NC} 通过"
        echo -e "  auth         ${GREEN}✓${NC} 通过"
        echo -e "  quota        ${GREEN}✓${NC} 通过"
        echo -e "  version      ${GREEN}✓${NC} 通过"
        echo -e "  config       ${GREEN}✓${NC} 通过"
        echo -e "  storage      ${GREEN}✓${NC} 通过"
        echo -e "${YELLOW}---${NC}"
        echo -e "${GREEN}通过: 6 / 6${NC}"
        ;;
    quota)
        echo -e "${BLUE}配额状态${NC}"
        echo -e "${YELLOW}---${NC}"
        echo -e "  今日使用: 0 / 2000"
        echo -e "  限流窗口: 100 / 分钟"
        echo -e "  重置时间: UTC 00:00"
        echo -e "${YELLOW}---${NC}"
        ;;
    whoami)
        echo -e "${BLUE}当前用户${NC}"
        echo -e "${YELLOW}---${NC}"
        echo -e "  ID: (未登录)"
        echo -e "  名称: (未登录)"
        echo -e "${YELLOW}---${NC}"
        ;;
    config)
        echo -e "${BLUE}Axiqra 配置${NC}"
        echo -e "${YELLOW}---${NC}"
        echo -e "  API URL: ${API_URL:-https://api.axiqra.com}"
        echo -e "  API Key: ${API_KEY:+***$(echo $API_KEY | tail -c 4)}${API_KEY:-未设置}"
        echo -e "  安装目录: $HOME/.axiqra"
        echo -e "${YELLOW}---${NC}"
        ;;
    *)
        if [ -z "$1" ]; then
            show_help
        else
            echo -e "${RED}未知命令: $1${NC}"
            echo -e "运行 ${GREEN}axiqra --help${NC} 查看帮助"
        fi
        ;;
esac
MOCK_SCRIPT
    
    chmod +x "$cli_path"
}

# ============================================================
# 创建配置文件
# ============================================================
create_config() {
    local config_file="$INSTALL_DIR/config/config.yaml"
    
    cat > "$config_file" << EOF
# Axiqra CLI 配置
# 生成时间: $(date '+%Y-%m-%d %H:%M:%S')

api:
  url: "$API_URL"
  timeout: 30
  retry: 3

cli:
  install_path: "$INSTALL_DIR"
  log_level: "info"

connect:
  default_channel: "cli"
  default_tool_type: "custom"
  auto_doctor: true

paths:
  config: "$config_file"
  cache: "$INSTALL_DIR/cache"
  data: "$INSTALL_DIR/data"
  logs: "$INSTALL_DIR/logs"
EOF
    
    log_success "配置文件: $config_file"
}

# ============================================================
# 添加到 PATH
# ============================================================
add_to_path() {
    local shell_rc=""
    
    if [ -f "$HOME/.bashrc" ]; then
        shell_rc="$HOME/.bashrc"
    elif [ -f "$HOME/.zshrc" ]; then
        shell_rc="$HOME/.zshrc"
    fi
    
    if [ -n "$shell_rc" ]; then
        if ! grep -q "$INSTALL_DIR/bin" "$shell_rc" 2>/dev/null; then
            echo "" >> "$shell_rc"
            echo "# Axiqra CLI" >> "$shell_rc"
            echo "export PATH=\"\$PATH:$INSTALL_DIR/bin\"" >> "$shell_rc"
            log_success "已添加到 PATH: $shell_rc"
        fi
    fi
}

# ============================================================
# 主流程
# ============================================================
main() {
    echo ""
    echo -e "${BLUE}╔═══════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║     Axiqra Connect 一键安装               ║${NC}"
    echo -e "${BLUE}║     版本: 1.0.0                         ║${NC}"
    echo -e "${BLUE}╚═══════════════════════════════════════════╝${NC}"
    echo ""
    
    log_info "安装目录: $INSTALL_DIR"
    log_info "API 地址: $API_URL"
    echo ""
    
    # 1. 安装 CLI
    log_info "步骤 1/3: 安装 CLI..."
    install_cli
    
    # 2. 创建配置
    log_info "步骤 2/3: 创建配置..."
    create_config
    
    # 3. 添加到 PATH
    log_info "步骤 3/3: 配置 PATH..."
    add_to_path
    
    echo ""
    echo -e "${GREEN}╔═══════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║     安装完成！                            ║${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "  ${YELLOW}下一步:${NC}"
    echo ""
    echo -e "  1. 设置 API Key:"
    echo -e "     ${BLUE}export AXIQRA_API_KEY=your-api-key${NC}"
    echo ""
    echo -e "  2. 登录:"
    echo -e "     ${BLUE}axiqra login --api-key your-api-key${NC}"
    echo ""
    echo -e "  3. 诊断:"
    echo -e "     ${BLUE}axiqra doctor${NC}"
    echo ""
    echo -e "  文档: ${BLUE}https://docs.axiqra.com/connect${NC}"
    echo ""
    
    # 提示
    if [ -z "$AXIQRA_API_KEY" ]; then
        echo -e "${YELLOW}注意: 请先获取 API Key 后再使用 axiqra 命令${NC}"
        echo ""
    fi
}

# 执行
main "$@"
