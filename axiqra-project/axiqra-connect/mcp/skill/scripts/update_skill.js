#!/usr/bin/env node
/**
 * Axiqra Skill 更新脚本
 *
 * 用于检查和更新 Skill 包
 *
 * 用法：
 *   node update_skill.js --check
 *   node update_skill.js --update
 *   node update_skill.js --fill-missing
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import https from 'https';
import http from 'http';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// 配置
const SKILL_DIR = process.env.AXIQRA_SKILL_DIR || path.resolve(__dirname, '..');
const OSS_BASE_URL = process.env.AXIQRA_OSS_URL || 'https://oss.axiqra.com';

// 颜色
const colors = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  cyan: '\x1b[36m',
  red: '\x1b[31m'
};

// 下载文件
function downloadFile(url, dest) {
  return new Promise((resolve, reject) => {
    const lib = url.startsWith('https') ? https : http;
    lib.get(url, response => {
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
      file.on('finish', () => file.close());
      file.on('finish', resolve);
    }).on('error', reject);
  });
}

// 获取本地版本
function getLocalVersion() {
  const configPath = path.join(SKILL_DIR, 'memory', 'axiqra-config.json');
  if (fs.existsSync(configPath)) {
    const config = JSON.parse(fs.readFileSync(configPath, 'utf-8'));
    return config.skill_version;
  }
  return null;
}

// 获取远程版本
async function getRemoteVersion() {
  try {
    const response = await fetch(`${OSS_BASE_URL}/skills/manifest.json`);
    const manifest = await response.json();
    return manifest.version;
  } catch {
    return null;
  }
}

// 检查更新
async function check() {
  const localVersion = getLocalVersion();
  const remoteVersion = await getRemoteVersion();

  if (!remoteVersion) {
    console.log(JSON.stringify({ status: 'error', message: '无法获取远程版本' }));
    return;
  }

  const updateAvailable = localVersion !== remoteVersion;

  console.log(JSON.stringify({
    status: updateAvailable ? 'update_available' : 'ok',
    local_version: localVersion,
    remote_version: remoteVersion,
    message: updateAvailable ? '有新版本可用' : '已是最新版本'
  }, null, 2));
}

// 更新
async function update() {
  console.log(`${colors.cyan}检查更新...${colors.reset}`);

  try {
    const response = await fetch(`${OSS_BASE_URL}/skills/manifest.json`);
    const manifest = await response.json();

    // 下载文件
    for (const file of manifest.files || []) {
      const fileUrl = `${OSS_BASE_URL}/skills/${file}`;
      const filePath = path.join(SKILL_DIR, file);

      try {
        fs.mkdirSync(path.dirname(filePath), { recursive: true });
        await downloadFile(fileUrl, filePath);
        console.log(`${colors.green}✓${colors.reset} ${file}`);
      } catch (e) {
        console.log(`${colors.yellow}⚠${colors.reset} ${file}: ${e.message}`);
      }
    }

    // 更新配置
    const configPath = path.join(SKILL_DIR, 'memory', 'axiqra-config.json');
    const config = JSON.parse(fs.readFileSync(configPath, 'utf-8'));
    config.skill_version = manifest.version;
    config.updated_at = new Date().toISOString();
    fs.writeFileSync(configPath, JSON.stringify(config, null, 2));

    console.log(`\n${colors.green}✓ 更新完成${colors.reset}`);
    console.log(JSON.stringify({ status: 'ok', version: manifest.version }));
  } catch (e) {
    console.log(JSON.stringify({ status: 'error', message: e.message }));
    process.exit(1);
  }
}

// 补齐缺失文件
async function fillMissing() {
  try {
    const response = await fetch(`${OSS_BASE_URL}/skills/manifest.json`);
    const manifest = await response.json();

    let count = 0;
    for (const file of manifest.files || []) {
      const filePath = path.join(SKILL_DIR, file);
      if (!fs.existsSync(filePath)) {
        const fileUrl = `${OSS_BASE_URL}/skills/${file}`;
        fs.mkdirSync(path.dirname(filePath), { recursive: true });
        await downloadFile(fileUrl, filePath);
        count++;
      }
    }

    console.log(JSON.stringify({ status: 'ok', filled: count }));
  } catch (e) {
    console.log(JSON.stringify({ status: 'error', message: e.message }));
    process.exit(1);
  }
}

// 主入口
async function main() {
  const args = process.argv.slice(2);
  const command = args[0];

  switch (command) {
    case '--check':
      await check();
      break;
    case '--update':
      await update();
      break;
    case '--fill-missing':
      await fillMissing();
      break;
    default:
      console.log(`
Axiqra Skill 更新脚本

用法：
  node update_skill.js --check        检查更新
  node update_skill.js --update       更新
  node update_skill.js --fill-missing 补齐缺失文件
`);
  }
}

main().catch(console.error);
