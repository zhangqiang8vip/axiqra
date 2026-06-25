#!/usr/bin/env node
/**
 * Axiqra 安装脚本
 *
 * 用于首次安装 Axiqra Skill 到宿主技能库
 *
 * 功能：
 * 1. 检测依赖
 * 2. 下载 Skill 包
 * 3. 初始化配置
 * 4. 发起授权
 *
 * 用法：
 *   node install.js
 *   node install.js --skip-download
 *   node install.js --platform cursor
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import https from 'https';
import http from 'http';

// 获取当前脚本位置
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// ============================================================
// 配置
// ============================================================

const SKILL_DIR = process.env.AXIQRA_SKILL_DIR || path.resolve(__dirname, '..');
const OSS_BASE_URL = process.env.AXIQRA_OSS_URL || 'https://oss.axiqra.com';
const API_URL = process.env.AXIQRA_API_URL || 'http://localhost:8080/api';
const WEB_URL = process.env.AXIQRA_WEB_URL || 'http://localhost:5173';

// ============================================================
// 颜色
// ============================================================

const colors = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  dim: '\x1b[2m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  cyan: '\x1b[36m',
  red: '\x1b[31m',
  gray: '\x1b[90m'
};

function log(color, prefix, ...args) {
  const p = prefix ? `  ${color}${prefix}${colors.reset}` : '';
  console.log(p, ...args.map(a => `${color}${a}${colors.reset}`));
}

function logInfo(msg) {
  log(colors.cyan, 'ℹ', msg);
}

function logSuccess(msg) {
  log(colors.green, '✓', msg);
}

function logWarn(msg) {
  log(colors.yellow, '⚠', msg);
}

function logError(msg) {
  log(colors.red, '✗', msg);
}

// ============================================================
// 帮助
// ============================================================

function showHelp() {
  console.log(`
${colors.bright}Axiqra 安装脚本${colors.reset}
${colors.gray}版本 1.0.0${colors.reset}

${colors.bright}用法:${colors.reset}
  node install.js [OPTIONS]

${colors.bright}选项:${colors.reset}
  --skip-download    跳过下载，使用本地文件
  --platform <name>  指定平台 (cursor/claude-code/copilot)
  --no-auth          跳过授权步骤
  --help             显示帮助

${colors.bright}环境变量:${colors.reset}
  AXIQRA_SKILL_DIR    Skill 安装目录
  AXIQRA_API_URL      API 地址
  AXIQRA_OSS_URL      OSS 地址
`);
}

// ============================================================
// 工具函数
// ============================================================

function ensureDir(dir) {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
}

function downloadFile(url, dest) {
  return new Promise((resolve, reject) => {
    const lib = url.startsWith('https') ? https : http;

    lib.get(url, (response) => {
      // 处理重定向
      if (response.statusCode >= 300 && response.statusCode < 400 && response.headers.location) {
        downloadFile(response.headers.location, dest).then(resolve).catch(reject);
        return;
      }

      if (response.statusCode !== 200) {
        reject(new Error(`HTTP ${response.statusCode}`));
        return;
      }

      const file = fs.createWriteStream(dest);
      response.pipe(file);
      file.on('finish', () => {
        file.close();
        resolve();
      });
    }).on('error', reject);
  });
}

// ============================================================
// 安装步骤
// ============================================================

async function checkNodeVersion() {
  const version = process.version.match(/^v(\d+)/)?.[1];
  if (!version || parseInt(version) < 16) {
    logWarn(`Node.js 版本 ${process.version}，建议 >= 16`);
  } else {
    logSuccess(`Node.js ${process.version}`);
  }
}

async function downloadSkillPackage(skipDownload) {
  if (skipDownload) {
    logInfo('跳过下载，使用本地文件');
    return;
  }

  console.log(`\n${colors.bright}下载 Skill 包${colors.reset}`);

  const manifestUrl = `${OSS_BASE_URL}/skills/manifest.json`;
  const manifestPath = path.join(SKILL_DIR, 'manifest.json');

  try {
    ensureDir(SKILL_DIR);
    await downloadFile(manifestUrl, manifestPath);

    const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf-8'));
    logSuccess(`下载 manifest.json`);

    // 下载其他文件
    for (const file of manifest.files || []) {
      if (file.startsWith('scripts/') || file === 'SKILL.md') {
        const fileUrl = `${OSS_BASE_URL}/skills/${file}`;
        const filePath = path.join(SKILL_DIR, file);
        ensureDir(path.dirname(filePath));

        try {
          await downloadFile(fileUrl, filePath);
          logSuccess(`下载 ${file}`);
        } catch (e) {
          logWarn(`下载 ${file} 失败: ${e.message}`);
        }
      }
    }
  } catch (e) {
    logWarn(`下载失败: ${e.message}`);
    logInfo('将使用本地文件继续');
  }
}

function initConfig() {
  console.log(`\n${colors.bright}初始化配置${colors.reset}`);

  const configPath = path.join(SKILL_DIR, 'memory', 'axiqra-config.json');
  ensureDir(path.dirname(configPath));

  const config = {
    base_url: API_URL,
    web_url: WEB_URL,
    mode: API_URL.includes('localhost') ? 'local' : 'production',
    skill_version: '1.0.0',
    installed_at: new Date().toISOString()
  };

  fs.writeFileSync(configPath, JSON.stringify(config, null, 2));
  logSuccess(`配置文件已创建: ${configPath}`);
}

async function startAuth() {
  console.log(`\n${colors.bright}发起授权${colors.reset}`);
  console.log(`  ${colors.gray}模式: ${API_URL.includes('localhost') ? colors.yellow : colors.green}${API_URL.includes('localhost') ? 'LOCAL' : 'PRODUCTION'}${colors.reset}\n`);

  // 调用 auth.js
  const authScript = path.join(__dirname, 'auth.js');
  if (fs.existsSync(authScript)) {
    console.log(`运行授权脚本...\n`);
    // 注意：这里会启动 auth.js 子进程
    // 由于权限问题，我们在这里内联授权逻辑
    await runInlineAuth();
  } else {
    throw new Error(`auth.js 不存在，无法发起授权: ${authScript}`);
  }
}

async function runInlineAuth() {
  // 获取设备码
  const deviceCode = 'axiqra-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);

  try {
    const response = await fetch(`${API_URL}/auth/device/code`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        client_id: 'axiqra-mcp-agent',
        device_code: deviceCode,
        platform: 'cursor'
      })
    });

    const data = await response.json();

    if (data.code === 0) {
      const authData = data.data;

      console.log(colors.bright + '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━' + colors.reset);
      console.log(colors.green + '\n✓ 授权已发起\n');

      if (authData.verification_url) {
        console.log(colors.cyan + `  授权网址: ${authData.verification_url}\n`);
      }

      console.log(colors.yellow + `  授权码:   ${colors.bright}${authData.user_code}${colors.reset}`);
      console.log(colors.blue + `  有效期:   ${Math.floor((authData.expires_in || 300) / 60)} 分钟\n`);
      console.log(colors.bright + '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━' + colors.reset);

      // 保存待确认的授权信息
      const pendingPath = path.join(SKILL_DIR, 'memory', 'axiqra-auth-pending.json');
      ensureDir(path.dirname(pendingPath));
      fs.writeFileSync(pendingPath, JSON.stringify({
        device_code: authData.device_code,
        user_code: authData.user_code,
        verification_url: authData.verification_url,
        interval: authData.interval || 5,
        expires_at: Date.now() + (authData.expires_in || 300) * 1000
      }, null, 2));

      console.log(`\n${colors.cyan}下一步：${colors.reset}`);
      console.log(`  1. 在浏览器中打开授权网址`);
      console.log(`  2. 输入授权码 ${authData.user_code}`);
      console.log(`  3. 点击确认`);
      console.log(`  4. 回到终端运行: node scripts/auth.js --wait ${authData.device_code}\n`);

      return authData;
    } else {
      logError(`授权发起失败: ${data.msg || data.message}`);
      return null;
    }
  } catch (e) {
    logError(`连接失败: ${e.message}`);
    logInfo('请确保 Axiqra 服务已启动');
    return null;
  }
}

// ============================================================
// 主入口
// ============================================================

async function main() {
  console.log(`\n${colors.bright}Axiqra 安装向导${colors.reset}`);
  console.log(colors.gray + '═'.repeat(50) + colors.reset);
  console.log(`  Skill 目录: ${SKILL_DIR}`);
  console.log(`  API 地址:   ${API_URL}\n`);

  // 解析参数
  const args = process.argv.slice(2);
  const skipDownload = args.includes('--skip-download');
  const noAuth = args.includes('--no-auth');

  if (args.includes('--help') || args.includes('-h')) {
    showHelp();
    return;
  }

  // 步骤 1: 检查环境
  console.log(`${colors.bright}步骤 1: 检查环境${colors.reset}`);
  await checkNodeVersion();

  // 步骤 2: 下载包
  console.log(`\n${colors.bright}步骤 2: 下载 Skill 包${colors.reset}`);
  await downloadSkillPackage(skipDownload);

  // 步骤 3: 初始化配置
  console.log(`\n${colors.bright}步骤 3: 初始化配置${colors.reset}`);
  initConfig();

  // 步骤 4: 授权
  if (!noAuth) {
    await startAuth();
  }

  // 完成
  console.log(`\n${colors.bright}安装完成！${colors.reset}`);
  console.log(`\n接下来：`);
  console.log(`  1. 完成授权（见上方提示）`);
  console.log(`  2. 将 Skill 目录添加到宿主配置`);
  console.log(`  3. 配置规则文件（见 SKILL.md）\n`);
}

main().catch(console.error);
