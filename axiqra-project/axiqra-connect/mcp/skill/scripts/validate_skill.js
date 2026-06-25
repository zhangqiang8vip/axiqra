#!/usr/bin/env node
/**
 * Axiqra Skill 校验脚本
 *
 * 校验 Skill 包完整性
 *
 * 用法：
 *   node validate_skill.js
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// 配置
const SKILL_DIR = process.env.AXIQRA_SKILL_DIR || path.resolve(__dirname, '..');
const OSS_BASE_URL = process.env.AXIQRA_OSS_URL || 'https://oss.axiqra.com';

// 颜色
const colors = {
  reset: '\x1b[0m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  red: '\x1b[31m'
};

// 必需文件
const REQUIRED_FILES = [
  'SKILL.md',
  'manifest.json',
  'scripts/auth.js',
  'scripts/rest_request.js'
];

// 可选文件
const OPTIONAL_FILES = [
  'API_REFERENCE.md',
  'PLAYBOOKS.md',
  'HOSTS.md',
  'SAFETY.md',
  'TROUBLESHOOTING.md',
  'SKILL.zh-CN.md',
  'scripts/install.js',
  'scripts/cruise_tick.js',
  'scripts/update_skill.js'
];

async function main() {
  const result = {
    status: 'ok',
    timestamp: new Date().toISOString(),
    checks: {
      required: [],
      optional: [],
      config: null
    }
  };

  // 检查必需文件
  for (const file of REQUIRED_FILES) {
    const filePath = path.join(SKILL_DIR, file);
    const exists = fs.existsSync(filePath);

    result.checks.required.push({
      file,
      exists,
      status: exists ? 'ok' : 'missing'
    });

    if (!exists) {
      result.status = 'error';
    }
  }

  // 检查可选文件
  for (const file of OPTIONAL_FILES) {
    const filePath = path.join(SKILL_DIR, file);
    const exists = fs.existsSync(filePath);

    result.checks.optional.push({
      file,
      exists,
      status: exists ? 'ok' : 'missing'
    });
  }

  // 检查配置文件
  const configPath = path.join(SKILL_DIR, 'memory', 'axiqra-config.json');
  if (fs.existsSync(configPath)) {
    try {
      const config = JSON.parse(fs.readFileSync(configPath, 'utf-8'));
      result.checks.config = {
        status: 'ok',
        mode: config.mode,
        version: config.skill_version
      };
    } catch {
      result.checks.config = { status: 'error', message: '配置文件损坏' };
    }
  } else {
    result.checks.config = { status: 'missing', message: '配置文件不存在' };
  }

  // 检查授权
  const authPath = path.join(SKILL_DIR, 'memory', 'axiqra-auth.json');
  result.checks.auth = {
    exists: fs.existsSync(authPath),
    status: fs.existsSync(authPath) ? 'authorized' : 'not_authorized'
  };

  console.log(JSON.stringify(result, null, 2));

  // 输出摘要
  const missingRequired = result.checks.required.filter(r => !r.exists);
  if (missingRequired.length > 0) {
    console.log(`\n${colors.red}✗ 缺少必需文件：${missingRequired.map(r => r.file).join(', ')}${colors.reset}`);
    process.exit(1);
  }

  console.log(`\n${colors.green}✓ 校验通过${colors.reset}`);
}

main().catch(e => {
  console.error(JSON.stringify({ status: 'error', message: e.message }));
  process.exit(1);
});
