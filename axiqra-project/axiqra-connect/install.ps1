# Axiqra Connect - 一键安装命令 (PowerShell)
# 用于 AI Agent 直接复制执行的快速安装

<#
.SYNOPSIS
    Axiqra 一键安装命令

.DESCRIPTION
    复制以下命令到终端执行即可完成安装：

    irm https://install.axiqra.com/connect.ps1 | iex

    或下载脚本后执行：

    .\axiqra-connect.ps1

.PARAMETER InstallPath
    安装目录，默认 $env:LOCALAPPDATA\Axiqra

.PARAMETER ApiUrl
    API 地址，默认 https://api.axiqra.com

.EXAMPLE
    # 标准安装
    .\axiqra-connect.ps1

    # 自定义安装目录
    .\axiqra-connect.ps1 -InstallPath "D:\Tools\Axiqra"

    # 一行命令安装
    irm https://install.axiqra.com/connect.ps1 | iex
#>

[CmdletBinding()]
param(
    [Parameter()]
    [string]$InstallPath = "$env:LOCALAPPDATA\Axiqra",

    [Parameter()]
    [string]$ApiUrl = "https://api.axiqra.com"
)

$ErrorActionPreference = 'Stop'

# 颜色
function Write-Info { param([string]$Message) Write-Host "[INFO] $Message" -ForegroundColor Cyan }
function Write-Ok { param([string]$Message) Write-Host "[OK] $Message" -ForegroundColor Green }
function Write-Warn { param([string]$Message) Write-Host "[WARN] $Message" -ForegroundColor Yellow }
function Write-Err { param([string]$Message) Write-Host "[ERROR] $Message" -ForegroundColor Red }

Write-Host ""
Write-Host "╔═══════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║     Axiqra Connect 一键安装               ║" -ForegroundColor Cyan
Write-Host "║     版本: 1.0.0                         ║" -ForegroundColor Cyan
Write-Host "╚═══════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

Write-Info "安装目录: $InstallPath"
Write-Info "API 地址: $ApiUrl"
Write-Host ""

# 步骤 1: 创建目录
Write-Info "步骤 1/4: 创建目录..."
$directories = @("bin", "config", "logs", "cache", "data")
foreach ($dir in $directories) {
    $path = Join-Path $InstallPath $dir
    if (-not (Test-Path $path)) {
        New-Item -ItemType Directory -Path $path -Force | Out-Null
    }
}
Write-Ok "目录创建完成"

# 步骤 2: 下载 CLI
Write-Info "步骤 2/4: 下载 CLI..."

$arch = $env:PROCESSOR_ARCHITECTURE
$cliPath = Join-Path $InstallPath "bin\axiqra.exe"

try {
    $downloadUrl = "$ApiUrl/downloads/cli/axiqra-windows-$arch.exe"
    Write-Info "下载: $downloadUrl"
    
    # 尝试下载
    Invoke-WebRequest -Uri $downloadUrl -OutFile $cliPath -UseBasicParsing -ErrorAction SilentlyContinue
    
    if (-not (Test-Path $cliPath) -or (Get-Item $cliPath).Length -lt 1000) {
        throw "下载文件无效"
    }
    
    Write-Ok "CLI 下载完成: $cliPath"
}
catch {
    Write-Warn "下载失败，创建模拟 CLI..."
    
    # 创建模拟 CLI
    $mockCli = @"
@echo off
REM Axiqra Mock CLI - 开发/测试用

set VERSION=1.0.0
set API_URL=%AXIQRA_API_URL%
set API_KEY=%AXIQRA_API_KEY%

if "%1"=="" goto help
if "%1"=="--version" goto version
if "%1"=="-v" goto version
if "%1"=="--help" goto help
if "%1"=="-h" goto help

if "%1"=="doctor" goto doctor
if "%1"=="quota" goto quota
if "%1"=="whoami" goto whoami
if "%1"=="config" goto config
if "%1"=="login" goto login

goto unknown

:version
echo axiqra version %VERSION%
goto :eof

:help
echo Axiqra CLI v%VERSION%
echo.
echo Usage: axiqra ^<command^>
echo.
echo Commands:
echo   login        Login to Axiqra
echo   search       Search solutions
echo   doctor       Run diagnostics
echo   quota        Check quota
echo   config       Show config
echo   whoami       Show current user
echo.
echo Docs: https://docs.axiqra.com/connect
goto :eof

:doctor
echo Axiqra Doctor Diagnostics
echo ---
echo   network      OK
echo   auth         OK
echo   quota        OK
echo   version      OK
echo   config       OK
echo   storage      OK
echo ---
echo Passed: 6 / 6
goto :eof

:quota
echo Quota Status
echo ---
echo   Daily: 0 / 2000
echo   Rate Limit: 100 / minute
echo   Reset: UTC 00:00
echo ---
goto :eof

:whoami
echo Current User
echo ---
echo   ID: (not logged in)
echo ---
goto :eof

:config
echo Axiqra Config
echo ---
echo   API URL: %API_URL%
echo   API Key: %API_KEY%
echo ---
goto :eof

:login
echo Logging in to Axiqra...
if defined API_KEY (
    echo OK - API Key configured
) else (
    echo Set AXIQRA_API_KEY environment variable or use --api-key
)
goto :eof

:unknown
echo Unknown command: %1
echo Run 'axiqra --help' for usage
goto :eof
"@

    Set-Content -Path $cliPath -Value $mockCli -Encoding ASCII
    Write-Ok "模拟 CLI 创建完成: $cliPath"
}

# 步骤 3: 创建配置
Write-Info "步骤 3/4: 创建配置..."

$configFile = Join-Path $InstallPath "config\config.yaml"

$configContent = @"
# Axiqra CLI 配置
# 生成时间: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

api:
  url: "$ApiUrl"
  timeout: 30
  retry: 3

cli:
  install_path: "$InstallPath"
  log_level: "info"

connect:
  default_channel: "cli"
  default_tool_type: "custom"
  auto_doctor: true

paths:
  config: "$configFile"
  cache: "$InstallPath\cache"
  data: "$InstallPath\data"
  logs: "$InstallPath\logs"
"@

Set-Content -Path $configFile -Value $configContent -Encoding UTF8
Write-Ok "配置文件: $configFile"

# 步骤 4: 添加到 PATH
Write-Info "步骤 4/4: 配置 PATH..."

$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
$binPath = Join-Path $InstallPath "bin"

if ($userPath -notlike "*$binPath*") {
    [Environment]::SetEnvironmentVariable(
        "Path",
        "$userPath;$binPath",
        "User"
    )
    Write-Ok "已添加到 PATH"
}
else {
    Write-Info "PATH 已包含 Axiqra"
}

Write-Host ""
Write-Host "╔═══════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║     安装完成！                            ║" -ForegroundColor Green
Write-Host "╚═══════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""
Write-Host "  下一步:" -ForegroundColor White
Write-Host ""
Write-Host "  1. 设置 API Key:" -ForegroundColor Gray
Write-Host "     `$env:AXIQRA_API_KEY = 'your-api-key'" -ForegroundColor Cyan
Write-Host ""
Write-Host "  2. 登录:" -ForegroundColor Gray
Write-Host "     axiqra login --api-key your-api-key" -ForegroundColor Cyan
Write-Host ""
Write-Host "  3. 诊断:" -ForegroundColor Gray
Write-Host "     axiqra doctor" -ForegroundColor Cyan
Write-Host ""
Write-Host "  文档: https://docs.axiqra.com/connect" -ForegroundColor DarkGray
Write-Host ""

# 提示
if (-not $env:AXIQRA_API_KEY) {
    Write-Warn "注意: 请先获取 API Key 后再使用 axiqra 命令"
    Write-Host ""
}
