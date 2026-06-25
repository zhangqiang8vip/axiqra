#!/usr/bin/env node
/**
 * Axiqra REST 请求脚本
 *
 * 用于执行 Axiqra API 请求的统一入口
 *
 * 用法：
 *   node rest_request.js GET <path> [--param KEY VALUE] [--file FILE] [--idempotency-key KEY] [--dry-run]
 *   node rest_request.js POST <path> [--file FILE] [--idempotency-key KEY] [--dry-run]
 *   node rest_request.js PUT <path> [--file FILE] [--dry-run]
 *   node rest_request.js DELETE <path> [--dry-run]
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

// 获取当前脚本位置
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// ============================================================
// 配置
// ============================================================

const SKILL_DIR = process.env.AXIQRA_SKILL_DIR || path.resolve(__dirname, '..');
const API_URL = process.env.AXIQRA_API_URL || readConfiguredApiUrl() || 'http://localhost:8080/api';

function readConfiguredApiUrl() {
  const configPath = path.join(SKILL_DIR, 'memory', 'axiqra-config.json');
  if (!fs.existsSync(configPath)) {
    return null;
  }
  try {
    return JSON.parse(fs.readFileSync(configPath, 'utf-8')).base_url;
  } catch {
    return null;
  }
}

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
${colors.bright}Axiqra REST 请求脚本${colors.reset}
${colors.gray}版本 1.0.0${colors.reset}

${colors.bright}用法:${colors.reset}
  node rest_request.js <METHOD> <PATH> [OPTIONS]

${colors.bright}方法:${colors.reset}
  GET    获取资源
  POST   创建资源
  PUT    更新资源
  DELETE 删除资源

${colors.bright}选项:${colors.reset}
  --param KEY VALUE    添加查询参数（用于 GET）
  --file FILE          添加请求体（用于 POST/PUT）
  --idempotency-key KEY  添加幂等键
  --dry-run            仅验证，不发送请求

${colors.bright}示例:${colors.reset}
  # 搜索方案
  node rest_request.js POST /search/before-act \\
    --file memory/sessions/s1/request-search.json

  # 获取方案列表
  node rest_request.js GET /solutions/public \\
    --param query 用户登录 \\
    --param limit 10

  # 提交反馈
  node rest_request.js POST /v1/feedbacks \\
    --file memory/sessions/s1/request-feedback.json \\
    --idempotency-key s1-feedback-001

${colors.bright}会话隔离:${colors.reset}
  所有写请求的 JSON 必须放在:
  {SKILL_DIR}/memory/sessions/{SESSION_ID}/

  推荐命名：
  - request-search.json    方案搜索
  - request-draft.json     草稿记录
  - request-feedback.json  反馈提交
`);
}

// ============================================================
// 获取凭证
// ============================================================

function getAuthToken() {
  const authPath = path.join(SKILL_DIR, 'memory', 'axiqra-auth.json');

  if (!fs.existsSync(authPath)) {
    return null;
  }

  try {
    const auth = JSON.parse(fs.readFileSync(authPath, 'utf-8'));
    return auth.access_token;
  } catch (e) {
    return null;
  }
}

// ============================================================
// HTTP 请求
// ============================================================

async function request(method, apiPath, options = {}) {
  const { params = {}, body = null, idempotencyKey = null, dryRun = false } = options;

  // 构建 URL
  let baseUrl = API_URL;
  if (!baseUrl.endsWith('/')) {
    baseUrl += '/';
  }

  const fullPath = apiPath.startsWith('/') ? apiPath.slice(1) : apiPath;
  const url = new URL(baseUrl + fullPath);

  // 添加查询参数
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null) {
      url.searchParams.set(key, value);
    }
  }

  // 构建请求选项
  const fetchOptions = {
    method: method,
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    }
  };

  // 添加认证
  const token = getAuthToken();
  if (token) {
    fetchOptions.headers['Authorization'] = `Bearer ${token}`;
  }

  // 添加幂等键
  if (idempotencyKey) {
    fetchOptions.headers['Idempotency-Key'] = idempotencyKey;
  }

  // 添加请求体
  if (body && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
    fetchOptions.body = JSON.stringify(body, null, 2);
  }

  // Dry run
  if (dryRun) {
    console.log(`\n${colors.yellow}[DRY RUN]${colors.reset} 不会发送实际请求\n`);
    console.log(`${colors.bright}${method}${colors.reset} ${url.href}`);
    if (body) {
      console.log(`\n${colors.gray}Body:${colors.reset}`);
      console.log(JSON.stringify(body, null, 2));
    }
    return null;
  }

  // 发送请求
  logInfo(`${method} ${url.href}`);

  try {
    const response = await fetch(url.href, fetchOptions);
    const text = await response.text();

    // 解析响应
    let data;
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }

    // 输出到 stdout（供 Agent 解析）
    console.log('\n--- RESPONSE ---');
    console.log(JSON.stringify(data, null, 2));
    console.log('--- END ---');

    // 返回结构化结果
    return {
      status: response.status,
      ok: response.ok,
      data: data
    };
  } catch (error) {
    logError(`请求失败: ${error.message}`);
    throw error;
  }
}

// ============================================================
// 参数解析
// ============================================================

function parseArgs(args) {
  const result = {
    method: null,
    path: null,
    params: {},
    body: null,
    idempotencyKey: null,
    dryRun: false
  };

  let i = 0;
  while (i < args.length) {
    const arg = args[i];

    if (arg === '--param') {
      const key = args[++i];
      const value = args[++i];
      if (key && value !== undefined) {
        result.params[key] = value;
      }
    } else if (arg === '--file') {
      const filePath = args[++i];
      if (filePath) {
        // 支持绝对路径和相对路径
        const absolutePath = path.isAbsolute(filePath)
          ? filePath
          : path.join(SKILL_DIR, filePath);

        if (fs.existsSync(absolutePath)) {
          result.body = JSON.parse(fs.readFileSync(absolutePath, 'utf-8'));
          logInfo(`加载请求体: ${absolutePath}`);
        } else {
          logWarn(`文件不存在: ${absolutePath}`);
        }
      }
    } else if (arg === '--idempotency-key') {
      result.idempotencyKey = args[++i];
    } else if (arg === '--dry-run') {
      result.dryRun = true;
    } else if (!result.method) {
      result.method = arg.toUpperCase();
    } else if (!result.path) {
      result.path = arg;
    }
    i++;
  }

  return result;
}

// ============================================================
// 主入口
// ============================================================

async function main() {
  const args = process.argv.slice(2);

  // 显示帮助
  if (args.length === 0 || args[0] === '--help' || args[0] === '-h') {
    showHelp();
    return;
  }

  // 解析参数
  const options = parseArgs(args);

  // 验证
  if (!options.method) {
    logError('缺少 HTTP 方法');
    console.log(`\n用法: node rest_request.js <METHOD> <PATH> [OPTIONS]\n`);
    process.exit(1);
  }

  if (!options.path) {
    logError('缺少 API 路径');
    console.log(`\n用法: node rest_request.js <METHOD> <PATH> [OPTIONS]\n`);
    process.exit(1);
  }

  const validMethods = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'];
  if (!validMethods.includes(options.method)) {
    logError(`无效的 HTTP 方法: ${options.method}`);
    console.log(`\n有效方法: ${validMethods.join(', ')}\n`);
    process.exit(1);
  }

  // 执行请求
  await request(options.method, options.path, {
    params: options.params,
    body: options.body,
    idempotencyKey: options.idempotencyKey,
    dryRun: options.dryRun
  });
}

main().catch(console.error);
