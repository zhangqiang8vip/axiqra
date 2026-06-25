#!/usr/bin/env node
/**
 * Axiqra 巡航脚本
 *
 * 用于定期检查用户的工作候选和状态变化
 *
 * 功能：
 * 1. 检查方案更新
 * 2. 检查反馈统计
 * 3. 生成推荐摘要
 *
 * 用法：
 *   node cruise_tick.js
 *   node cruise_tick.js --limit 10
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
const API_URL = process.env.AXIQRA_API_URL || (
  process.env.AXIQRA_API_URL?.includes('localhost') || process.env.AXIQRA_API_URL?.includes('127.0.0.1')
    ? 'http://localhost:8080/api'
    : 'https://api.axiqra.com/api'
);

// ============================================================
// 颜色
// ============================================================

const colors = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  cyan: '\x1b[36m',
  red: '\x1b[31m',
  gray: '\x1b[90m'
};

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

function getWorkspaceId() {
  const authPath = path.join(SKILL_DIR, 'memory', 'axiqra-auth.json');

  if (!fs.existsSync(authPath)) {
    return null;
  }

  try {
    const auth = JSON.parse(fs.readFileSync(authPath, 'utf-8'));
    return auth.workspace_id;
  } catch (e) {
    return null;
  }
}

// ============================================================
// API 请求
// ============================================================

async function apiRequest(method, apiPath, body = null) {
  const token = getAuthToken();
  const workspaceId = getWorkspaceId();

  let url = API_URL;
  if (!url.endsWith('/')) {
    url += '/';
  }
  url += apiPath.startsWith('/') ? apiPath.slice(1) : apiPath;

  const options = {
    method: method,
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    }
  };

  if (token) {
    options.headers['Authorization'] = `Bearer ${token}`;
  }

  if (body && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(url, options);
  const data = await response.json();

  return { status: response.status, data: data };
}

// ============================================================
// 巡航检查
// ============================================================

async function checkSolutions() {
  try {
    const result = await apiRequest('GET', '/solutions/public?limit=10');
    if (result.status === 200) {
      return {
        type: 'solutions_check',
        status: 'ok',
        count: result.data.data?.length || 0
      };
    }
  } catch (e) {
    return { type: 'solutions_check', status: 'error', message: e.message };
  }
}

async function checkTraces() {
  try {
    const result = await apiRequest('GET', '/traces');
    if (result.status === 200) {
      return {
        type: 'traces_check',
        status: 'ok',
        count: result.data.data?.length || 0
      };
    }
  } catch (e) {
    return { type: 'traces_check', status: 'error', message: e.message };
  }
}

async function checkSkillUpdate() {
  // 检查 OSS 上的 manifest 版本
  try {
    const ossUrl = 'https://oss.axiqra.com/skills/manifest.json';
    const response = await fetch(ossUrl);
    const manifest = await response.json();

    const localConfigPath = path.join(SKILL_DIR, 'memory', 'axiqra-config.json');
    let localVersion = '1.0.0';

    if (fs.existsSync(localConfigPath)) {
      const config = JSON.parse(fs.readFileSync(localConfigPath, 'utf-8'));
      localVersion = config.skill_version || '1.0.0';
    }

    const needsUpdate = manifest.version !== localVersion;

    return {
      type: 'skill_update_check',
      status: 'ok',
      current_version: localVersion,
      latest_version: manifest.version,
      update_available: needsUpdate
    };
  } catch (e) {
    return { type: 'skill_update_check', status: 'error', message: e.message };
  }
}

// ============================================================
// 主入口
// ============================================================

async function main() {
  const token = getAuthToken();
  const workspaceId = getWorkspaceId();

  const result = {
    status: 'ok',
    workspace_id: workspaceId,
    timestamp: new Date().toISOString(),
    checks: {}
  };

  // 执行检查
  result.checks.solutions = await checkSolutions();
  result.checks.traces = await checkTraces();
  result.checks.skill_update = await checkSkillUpdate();

  // 生成摘要
  const summaries = [];

  if (result.checks.skill_update?.update_available) {
    summaries.push(`有新版本 ${result.checks.skill_update.latest_version} 可用`);
  }

  result.summaries = summaries;

  // 输出 JSON（供 Agent 解析）
  console.log(JSON.stringify(result, null, 2));
}

main().catch(e => {
  console.error(JSON.stringify({
    status: 'error',
    message: e.message
  }));
  process.exit(1);
});
