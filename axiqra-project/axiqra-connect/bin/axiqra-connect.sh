#!/bin/bash
# Axiqra Connect CLI - macOS/Linux 一键安装脚本

set -e

# ============================================================
# 配置
# ============================================================
INSTALL_PATH="${HOME}/.axiqra"
API_URL="https://api.axiqra.com"
LOG_FILE="${INSTALL_PATH}/logs/install.log"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ============================================================
# 工具函数
# ============================================================
log() {
    local level=$1
    shift
    local message="[$(date '+%Y-%m-%d %H:%M:%S')] [${level}] $*"
    echo -e "${message}"
    mkdir -p "$(dirname ${LOG_FILE})"
    echo "${message}" >> "${LOG_FILE}"
}

info() { log "INFO" "$@"; }
warn() { log "WARN" "$@" >&2; }
error() { log "ERROR" "$@" >&2; }
success() { echo -e "${GREEN}✓ $*${NC}"; }
step() { echo -e "\n${BLUE}==> ${*}${NC}"; }

# ============================================================
# 检查系统要求
# ============================================================
check_requirements() {
    step "检查系统要求..."

    # 检查 curl
    if ! command -v curl &> /dev/null; then
        error "curl 未安装"
        exit 1
    fi

    # 检查网络
    if curl -sf --head "${API_URL}" > /dev/null 2>&1; then
        info "API 服务器可达"
    else
        warn "无法连接到 ${API_URL}"
    fi

    success "系统检查完成"
}

# ============================================================
# 创建目录
# ============================================================
init_directories() {
    step "初始化目录..."

    mkdir -p "${INSTALL_PATH}"/{bin,config,logs,cache,data}
    info "安装目录: ${INSTALL_PATH}"

    success "目录初始化完成"
}

# ============================================================
# 下载 CLI
# ============================================================
install_cli() {
    step "下载 Axiqra CLI..."

    local os=$(uname -s | tr '[:upper:]' '[:lower:]')
    local arch=$(uname -m)
    local ext="tar.gz"

    # 处理架构
    case ${arch} in
        x86_64) arch="amd64" ;;
        aarch64|arm64) arch="arm64" ;;
    esac

    local download_url="${API_URL}/downloads/cli/axiqra-${os}-${arch}.${ext}"
    local archive="${INSTALL_PATH}/cache/axiqra.${ext}"
    local bin_dir="${INSTALL_PATH}/bin"

    info "下载: ${download_url}"

    curl -fsSL -o "${archive}" "${download_url}"

    # 解压
    tar -xzf "${archive}" -C "${bin_dir}"

    # 查找可执行文件
    local cli_bin
    cli_bin=$(find "${bin_dir}" -name "axiqra" -type f 2>/dev/null | head -1)

    if [ -z "${cli_bin}" ]; then
        error "CLI 解压失败"
        exit 1
    fi

    chmod +x "${cli_bin}"

    # 添加到 PATH
    local shell_rc="${HOME}/.bashrc"
    if [ -f "${HOME}/.zshrc" ]; then
        shell_rc="${HOME}/.zshrc"
    fi

    if ! grep -q "${bin_dir}" "${shell_rc}" 2>/dev/null; then
        echo "export PATH=\"\${PATH}:${bin_dir}\"" >> "${shell_rc}"
        info "已添加到 PATH: ${bin_dir}"
    fi

    success "CLI 安装完成: ${cli_bin}"
}

# ============================================================
# 配置
# ============================================================
configure() {
    step "配置 Axiqra..."

    local config_file="${INSTALL_PATH}/config/config.yaml"

    cat > "${config_file}" << EOF
# Axiqra CLI 配置
# 生成时间: $(date '+%Y-%m-%d %H:%M:%S')

api:
  url: "${API_URL}"
  timeout: 30
  retry: 3

cli:
  install_path: "${INSTALL_PATH}"
  log_level: "info"

connect:
  default_channel: "cli"
  default_tool_type: "custom"
  auto_doctor: true

doctor:
  checks:
    - network
    - auth
    - quota
    - version
    - config
    - storage

paths:
  config: "${config_file}"
  cache: "${INSTALL_PATH}/cache"
  data: "${INSTALL_PATH}/data"
  logs: "${INSTALL_PATH}/logs"
EOF

    # 设置环境变量
    export AXIQRA_API_URL="${API_URL}"
    export AXIQRA_CONFIG_PATH="${config_file}"

    info "配置文件: ${config_file}"
    success "配置完成"
}

# ============================================================
# Doctor 检测
# ============================================================
run_doctor() {
    step "执行 Doctor 检测..."

    local cli="${INSTALL_PATH}/bin/axiqra"

    if [ ! -f "${cli}" ]; then
        warn "CLI 未找到，跳过 Doctor 检测"
        return 0
    fi

    # 8 项检查
    local checks=("network" "auth" "quota" "version" "config" "storage")

    for check in "${checks[@]}"; do
        echo -n "  检测: ${check}... "
        if ${cli} doctor --check ${check} &>/dev/null; then
            echo -e "${GREEN}✓${NC}"
        else
            echo -e "${YELLOW}✗${NC}"
        fi
    done

    success "Doctor 检测完成"
}

# ============================================================
# 显示完成信息
# ============================================================
show_completion() {
    local cli_bin="${INSTALL_PATH}/bin/axiqra"

    echo ""
    echo -e "${GREEN}═══════════════════════════════════════════${NC}"
    echo -e "${GREEN}  Axiqra Connect 安装完成！${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════${NC}"
    echo ""
    echo -e "  ${YELLOW}下一步:${NC}"
    echo ""
    echo -e "  1. 登录 Axiqra:"
    echo -e "     ${BLUE}${cli_bin} login${NC}"
    echo ""
    echo -e "  2. 初始化接入:"
    echo -e "     ${BLUE}${cli_bin} connect init${NC}"
    echo ""
    echo -e "  3. Doctor 检测:"
    echo -e "     ${BLUE}${cli_bin} doctor${NC}"
    echo ""
    echo -e "  文档: https://docs.axiqra.com/connect"
    echo ""
    echo -e "${GREEN}═══════════════════════════════════════════${NC}"

    # 注意
    echo ""
    warn "注意: 请重新打开终端窗口使 PATH 生效"
    warn "或手动执行: export PATH=\"\${PATH}:${INSTALL_PATH}/bin\""
}

# ============================================================
# 主流程
# ============================================================
main() {
    echo -e "${BLUE}═══════════════════════════════════════════${NC}"
    echo -e "${BLUE}  Axiqra Connect 一键安装${NC}"
    echo -e "${BLUE}  版本: 1.0.0${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════${NC}"
    echo ""
    info "安装路径: ${INSTALL_PATH}"
    info "API 地址: ${API_URL}"

    # 1. 检查
    check_requirements

    # 2. 目录
    init_directories

    # 3. CLI
    install_cli

    # 4. 配置
    configure

    # 5. Doctor（可选）
    if [ "${1}" != "--skip-doctor" ]; then
        run_doctor
    fi

    # 6. 完成
    show_completion
}

# 执行
main "$@"
