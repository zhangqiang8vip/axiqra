# Axiqra Connect - Windows 一键安装脚本
# Axiqra Connect - One-Click Installation Script for Windows

<#
.SYNOPSIS
    Axiqra 一键接入安装脚本

.DESCRIPTION
    本脚本自动化完成以下步骤：
    1. 检查系统要求
    2. 下载 Axiqra CLI
    3. 配置环境变量
    4. 执行 doctor 检测
    5. 验证安装结果

.NOTES
    作者: Axiqra Team
    版本: 1.0.0
    日期: 2026-06-24

.EXAMPLE
    # 标准安装
    .\axiqra-connect.ps1

    # 自定义安装目录
    .\axiqra-connect.ps1 -InstallPath "D:\Tools\Axiqra"

    # 跳过 doctor 检测
    .\axiqra-connect.ps1 -SkipDoctor

    # 仅下载 CLI
    .\axiqra-connect.ps1 -DownloadOnly
#>

[CmdletBinding()]
param(
    [Parameter()]
    [string]$InstallPath = "$env:LOCALAPPDATA\Axiqra",

    [Parameter()]
    [string]$ApiUrl = "https://api.axiqra.com",

    [Parameter()]
    [switch]$SkipDoctor,

    [Parameter()]
    [switch]$DownloadOnly,

    [Parameter()]
    [switch]$Verbose
)

# ============================================================
# 颜色定义
# ============================================================
$colors = @{
    Success = @{
        ForegroundColor = 'Green'
        BackgroundColor = 'Black'
    }
    Error = @{
        ForegroundColor = 'Red'
        BackgroundColor = 'Black'
    }
    Warning = @{
        ForegroundColor = 'Yellow'
        BackgroundColor = 'Black'
    }
    Info = @{
        ForegroundColor = 'Cyan'
        BackgroundColor = 'Black'
    }
    Step = @{
        ForegroundColor = 'Magenta'
        BackgroundColor = 'Black'
    }
}

function Write-Success { param([string]$Message) Write-Host $Message @colors.Success }
function Write-Err { param([string]$Message) Write-Host $Message @colors.Error }
function Write-Warn { param([string]$Message) Write-Host $Message @colors.Warning }
function Write-Info { param([string]$Message) Write-Host $Message @colors.Info }
function Write-Step { param([string]$Message) Write-Host "`n==> $Message" @colors.Step }

# ============================================================
# 日志函数
# ============================================================
function Write-Log {
    param(
        [string]$Level,
        [string]$Message
    )
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $logMessage = "[$timestamp] [$Level] $Message"

    if ($Verbose) {
        Write-Host $logMessage -ForegroundColor DarkGray
    }

    # 写入日志文件
    $logDir = Join-Path $InstallPath "logs"
    if (-not (Test-Path $logDir)) {
        New-Item -ItemType Directory -Path $logDir -Force | Out-Null
    }
    $logFile = Join-Path $logDir "install.log"
    Add-Content -Path $logFile -Value $logMessage
}

# ============================================================
# 检查系统要求
# ============================================================
function Test-SystemRequirements {
    Write-Step "检查系统要求..."

    $errors = @()

    # 检查 PowerShell 版本
    if ($PSVersionTable.PSVersion.Major -lt 5) {
        $errors += "PowerShell 5.0 或更高版本 (当前: $($PSVersionTable.PSVersion))"
    }

    # 检查网络连接
    try {
        $null = Invoke-WebRequest -Uri $ApiUrl -Method Head -TimeoutSec 10 -UseBasicParsing
        Write-Log -Level "INFO" -Message "API 服务器可达: $ApiUrl"
    }
    catch {
        Write-Warn "无法连接到 $ApiUrl"
        Write-Log -Level "WARN" -Message "API 连接失败: $_"
    }

    # 检查写入权限
    try {
        $testFile = Join-Path $InstallPath ".write_test"
        "test" | Out-File -FilePath $testFile -Force
        Remove-Item $testFile -Force
        Write-Log -Level "INFO" -Message "安装目录可写: $InstallPath"
    }
    catch {
        $errors += "无法写入安装目录: $InstallPath"
    }

    # 检查 curl
    $curlVersion = $null
    try {
        $curlVersion = (curl --version 2>$null | Select-Object -First 1)
        Write-Log -Level "INFO" -Message "curl 可用: $curlVersion"
    }
    catch {
        Write-Warn "curl 未找到，将使用 Invoke-WebRequest"
    }

    # 检查 git
    $gitVersion = $null
    try {
        $gitVersion = (git --version 2>$null)
        Write-Log -Level "INFO" -Message "git 可用: $gitVersion"
    }
    catch {
        Write-Warn "git 未找到"
    }

    if ($errors.Count -gt 0) {
        Write-Err "系统检查失败:"
        $errors | ForEach-Object { Write-Err "  - $_" }
        return $false
    }

    Write-Success "系统检查通过"
    return $true
}

# ============================================================
# 创建安装目录
# ============================================================
function Initialize-InstallDirectory {
    Write-Step "初始化安装目录..."

    if (-not (Test-Path $InstallPath)) {
        New-Item -ItemType Directory -Path $InstallPath -Force | Out-Null
        Write-Log -Level "INFO" -Message "创建安装目录: $InstallPath"
    }

    # 创建子目录
    $subDirs = @("bin", "config", "logs", "cache", "data")
    foreach ($dir in $subDirs) {
        $path = Join-Path $InstallPath $dir
        if (-not (Test-Path $path)) {
            New-Item -ItemType Directory -Path $path -Force | Out-Null
        }
    }

    Write-Success "安装目录初始化完成"
}

# ============================================================
# 下载 CLI
# ============================================================
function Install-AxiqraCLI {
    Write-Step "下载 Axiqra CLI..."

    $binDir = Join-Path $InstallPath "bin"

    # 检测系统架构
    $arch = $env:PROCESSOR_ARCHITECTURE
    $os = "windows"
    $ext = "zip"

    Write-Info "系统: Windows $arch"

    # 下载地址（示例，实际地址需要配置）
    $downloadBase = "$ApiUrl/downloads/cli"
    $cliZip = "axiqra-cli-$os-$arch.$ext"
    $downloadUrl = "$downloadBase/$cliZip"
    $localZip = Join-Path $InstallPath "cache\$cliZip"

    try {
        Write-Info "下载 CLI: $downloadUrl"
        Write-Log -Level "INFO" -Message "开始下载: $downloadUrl"

        # 使用 curl 或 Invoke-WebRequest
        if (Get-Command curl -ErrorAction SilentlyContinue) {
            curl -fsSL -o $localZip $downloadUrl
        }
        else {
            Invoke-WebRequest -Uri $downloadUrl -OutFile $localZip -UseBasicParsing
        }

        Write-Log -Level "INFO" -Message "下载完成: $localZip"

        # 解压
        Write-Info "解压 CLI..."
        $cliExe = Join-Path $binDir "axiqra.exe"

        # 如果是 zip 文件
        if ($ext -eq "zip") {
            Expand-Archive -Path $localZip -DestinationPath $binDir -Force
            # 查找解压后的可执行文件
            $extractedFiles = Get-ChildItem -Path $binDir -Recurse -Filter "axiqra*.exe" -ErrorAction SilentlyContinue
            if ($extractedFiles) {
                Copy-Item $extractedFiles[0].FullName -Destination $cliExe -Force
            }
        }

        # 添加到 PATH（当前用户）
        $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
        if ($userPath -notlike "*$InstallPath\bin*") {
            [Environment]::SetEnvironmentVariable(
                "Path",
                "$userPath;$InstallPath\bin",
                "User"
            )
            Write-Info "已添加 $InstallPath\bin 到 PATH"
            Write-Log -Level "INFO" -Message "添加 PATH: $InstallPath\bin"
        }

        Write-Success "CLI 安装完成: $cliExe"
        return $cliExe
    }
    catch {
        Write-Err "CLI 下载失败: $_"
        Write-Log -Level "ERROR" -Message "下载失败: $_"
        return $null
    }
}

# ============================================================
# 配置环境变量
# ============================================================
function Set-AxiqraConfig {
    Write-Step "配置 Axiqra..."

    $configDir = Join-Path $InstallPath "config"
    $configFile = Join-Path $configDir "config.yaml"

    # 创建配置文件
    $config = @"
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

doctor:
  checks:
    - network
    - auth
    - quota
    - version
    - config
    - storage
    - proxy
    - firewall

paths:
  config: "$configFile"
  cache: "$InstallPath\cache"
  data: "$InstallPath\data"
  logs: "$InstallPath\logs"
"@

    Set-Content -Path $configFile -Value $config -Encoding UTF8
    Write-Log -Level "INFO" -Message "配置文件: $configFile"

    # 设置环境变量
    [Environment]::SetEnvironmentVariable("AXIQRA_API_URL", $ApiUrl, "User")
    [Environment]::SetEnvironmentVariable("AXIQRA_CONFIG_PATH", $configFile, "User")

    Write-Success "配置完成"
}

# ============================================================
# Doctor 检测
# ============================================================
function Invoke-AxiqraDoctor {
    Write-Step "执行 Doctor 检测..."

    $cliExe = Join-Path $InstallPath "bin\axiqra.exe"

    if (-not (Test-Path $cliExe)) {
        Write-Warn "CLI 未找到，跳过 Doctor 检测"
        return $true
    }

    try {
        Write-Info "运行 Axiqra Doctor 检测..."

        # 8 项检查
        $checks = @(
            @{ Name = "network"; Description = "网络连接" },
            @{ Name = "auth"; Description = "认证状态" },
            @{ Name = "quota"; Description = "配额状态" },
            @{ Name = "version"; Description = "CLI 版本" },
            @{ Name = "config"; Description = "配置文件" },
            @{ Name = "storage"; Description = "存储权限" },
            @{ Name = "proxy"; Description = "代理设置" },
            @{ Name = "firewall"; Description = "防火墙" }
        )

        $results = @()

        foreach ($check in $checks) {
            Write-Info "  检测: $($check.Description)..."

            try {
                $result = & $cliExe doctor --check $check.Name 2>&1

                if ($LASTEXITCODE -eq 0) {
                    Write-Success "    ✓ $($check.Description): 通过"
                    $results += @{ Name = $check.Name; Status = "PASS" }
                    Write-Log -Level "INFO" -Message "$($check.Name): PASS"
                }
                else {
                    Write-Warn "    ✗ $($check.Description): 失败"
                    $results += @{ Name = $check.Name; Status = "FAIL" }
                    Write-Log -Level "WARN" -Message "$($check.Name): FAIL - $result"
                }
            }
            catch {
                Write-Warn "    ? $($check.Description): 跳过"
                $results += @{ Name = $check.Name; Status = "SKIP" }
                Write-Log -Level "INFO" -Message "$($check.Name): SKIP"
            }
        }

        # 显示摘要
        $passed = ($results | Where-Object { $_.Status -eq "PASS" }).Count
        $total = $results.Count

        Write-Host ""
        Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan
        Write-Host "  Doctor 检测摘要" -ForegroundColor Cyan
        Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan
        Write-Host "  通过: $passed / $total" -ForegroundColor $(if ($passed -eq $total) { "Green" } else { "Yellow" })
        Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan

        return ($passed -eq $total)
    }
    catch {
        Write-Warn "Doctor 检测执行失败: $_"
        Write-Log -Level "WARN" -Message "Doctor 检测异常: $_"
        return $true  # 不阻塞安装
    }
}

# ============================================================
# 测试安装
# ============================================================
function Test-Installation {
    Write-Step "验证安装结果..."

    $cliExe = Join-Path $InstallPath "bin\axiqra.exe"

    if (-not (Test-Path $cliExe)) {
        Write-Err "CLI 可执行文件未找到: $cliExe"
        return $false
    }

    try {
        # 测试 CLI 版本
        Write-Info "测试 CLI 版本..."
        $version = & $cliExe --version 2>&1
        Write-Host "  版本: $version" -ForegroundColor Gray

        # 测试帮助
        Write-Info "测试 CLI 帮助..."
        $help = & $cliExe --help 2>&1 | Select-Object -First 5
        $help | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }

        Write-Success "CLI 验证通过"
        return $true
    }
    catch {
        Write-Err "CLI 验证失败: $_"
        return $false
    }
}

# ============================================================
# 显示完成信息
# ============================================================
function Show-CompletionMessage {
    param(
        [bool]$Success,
        [string]$CliPath
    )

    Write-Host ""
    Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host ""

    if ($Success) {
        Write-Host "  ✓ Axiqra Connect 安装完成！" -ForegroundColor Green
    }
    else {
        Write-Host "  ⚠ Axiqra Connect 安装完成（部分功能可能受限）" -ForegroundColor Yellow
    }

    Write-Host ""
    Write-Host "  下一步:" -ForegroundColor White
    Write-Host ""
    Write-Host "  1. 登录 Axiqra:" -ForegroundColor Gray
    Write-Host "     $CliPath login" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  2. 初始化接入:" -ForegroundColor Gray
    Write-Host "     $CliPath connect init" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  3. 查看帮助:" -ForegroundColor Gray
    Write-Host "     $CliPath --help" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  文档: https://docs.axiqra.com/connect" -ForegroundColor DarkGray
    Write-Host ""
    Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan

    # 注意 PATH 变更
    Write-Host ""
    Write-Warn "注意: 如果 CLI 命令不可用，请重新打开终端窗口"
    Write-Warn "或手动将以下路径添加到 PATH:" -ForegroundColor Yellow
    Write-Host "  $InstallPath\bin" -ForegroundColor Cyan
}

# ============================================================
# 主流程
# ============================================================
function Main {
    Write-Host ""
    Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "  Axiqra Connect 一键安装" -ForegroundColor Cyan
    Write-Host "  版本: 1.0.0" -ForegroundColor DarkGray
    Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host ""
    Write-Info "安装路径: $InstallPath"
    Write-Info "API 地址: $ApiUrl"
    Write-Host ""

    # 1. 检查系统要求
    if (-not (Test-SystemRequirements)) {
        Write-Err "系统检查失败，安装终止"
        exit 1
    }

    # 2. 初始化目录
    Initialize-InstallDirectory

    # 3. 下载 CLI
    $cliExe = Install-AxiqraCLI
    if (-not $cliExe -and -not $DownloadOnly) {
        Write-Err "CLI 安装失败"
        exit 1
    }

    if ($DownloadOnly) {
        Write-Success "CLI 下载完成"
        exit 0
    }

    # 4. 配置
    Set-AxiqraConfig

    # 5. Doctor 检测（可选）
    if (-not $SkipDoctor) {
        $doctorPassed = Invoke-AxiqraDoctor
    }

    # 6. 验证
    $verified = Test-Installation

    # 7. 显示完成信息
    $fullPath = if ($cliExe) { $cliExe } else { "$InstallPath\bin\axiqra.exe" }
    Show-CompletionMessage -Success $verified -CliPath (Split-Path $fullPath -Leaf)

    Write-Log -Level "INFO" -Message "安装完成"

    exit $(if ($verified) { 0 } else { 0 })  # 即使部分失败也退出 0
}

# 执行
Main
