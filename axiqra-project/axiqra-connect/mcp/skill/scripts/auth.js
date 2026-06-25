#!/usr/bin/env node

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const SKILL_DIR = process.env.AXIQRA_SKILL_DIR || path.resolve(__dirname, '..');
const MEMORY_DIR = path.join(SKILL_DIR, 'memory');
const AUTH_PATH = path.join(MEMORY_DIR, 'axiqra-auth.json');
const PENDING_PATH = path.join(MEMORY_DIR, 'axiqra-auth-pending.json');
const CONFIG_PATH = path.join(MEMORY_DIR, 'axiqra-config.json');

const DEFAULT_API_URL = 'http://localhost:8080/api';
const DEFAULT_WEB_URL = 'http://localhost:5173';

function ensureDir(dir) {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
}

function readJson(file) {
  if (!fs.existsSync(file)) return null;
  try {
    return JSON.parse(fs.readFileSync(file, 'utf-8'));
  } catch {
    return null;
  }
}

function getConfig() {
  const config = readJson(CONFIG_PATH) || {};
  return {
    apiUrl: process.env.AXIQRA_API_URL || config.base_url || DEFAULT_API_URL,
    webUrl: process.env.AXIQRA_WEB_URL || config.web_url || DEFAULT_WEB_URL
  };
}

function jsonOut(payload) {
  process.stdout.write(`${JSON.stringify(payload, null, 2)}\n`);
}

function normalizeApiResponse(data) {
  if (data && typeof data === 'object' && 'code' in data) {
    return data;
  }
  return { code: 0, data };
}

async function requestJson(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      ...(options.headers || {})
    }
  });
  const text = await response.text();
  let body;
  try {
    body = text ? JSON.parse(text) : null;
  } catch {
    body = { raw: text };
  }
  return { ok: response.ok, status: response.status, body: normalizeApiResponse(body) };
}

function buildVerificationUrl(webUrl, userCode, deviceCode) {
  const url = new URL('/auth/device', webUrl.endsWith('/') ? webUrl : `${webUrl}/`);
  url.searchParams.set('code', userCode);
  url.searchParams.set('device', deviceCode);
  return url.href;
}

async function startAuth() {
  const { apiUrl, webUrl } = getConfig();
  const result = await requestJson(`${apiUrl}/auth/device/code`, {
    method: 'POST',
    body: JSON.stringify({
      client_id: 'axiqra-skill',
      platform: process.env.AXIQRA_PLATFORM || 'codex'
    })
  });

  if (result.body.code !== 0 || !result.body.data) {
    jsonOut({
      ok: false,
      step: 'start',
      status: result.status,
      code: result.body.code,
      message: result.body.message || '设备授权码获取失败'
    });
    process.exitCode = 1;
    return;
  }

  const data = result.body.data;
  const verificationUrl = buildVerificationUrl(webUrl, data.user_code, data.device_code);
  const pending = {
    device_code: data.device_code,
    user_code: data.user_code,
    verification_url: verificationUrl,
    interval: data.interval || 2,
    expires_in: data.expires_in || 600,
    expires_at: Date.now() + (data.expires_in || 600) * 1000,
    api_url: apiUrl,
    web_url: webUrl
  };

  ensureDir(MEMORY_DIR);
  fs.writeFileSync(PENDING_PATH, JSON.stringify(pending, null, 2));

  jsonOut({
    ok: true,
    user_code: pending.user_code,
    device_code: pending.device_code,
    verification_url: pending.verification_url,
    interval: pending.interval,
    expires_in: pending.expires_in,
    next: `打开 ${pending.verification_url} 完成确认，然后运行 node scripts/auth.js --wait ${pending.device_code}`
  });
}

async function waitForToken(deviceCodeArg) {
  const pending = readJson(PENDING_PATH) || {};
  const { apiUrl } = getConfig();
  const deviceCode = deviceCodeArg || pending.device_code;
  if (!deviceCode) {
    jsonOut({ ok: false, step: 'wait', message: '缺少 device_code，请先运行 --start' });
    process.exitCode = 1;
    return;
  }

  const intervalSeconds = Number(pending.interval || 2);
  const expiresAt = Number(pending.expires_at || (Date.now() + 600_000));

  while (Date.now() < expiresAt) {
    const url = new URL(`${apiUrl}/auth/device/token`);
    url.searchParams.set('deviceCode', deviceCode);
    const result = await requestJson(url.href, { method: 'POST' });
    const body = result.body;

    if (body.code === 0 && body.data?.access_token) {
      const auth = {
        access_token: body.data.access_token,
        token_type: body.data.token_type || 'Bearer',
        expires_in: body.data.expires_in,
        user: body.data.user || null,
        api_url: pending.api_url || apiUrl,
        web_url: pending.web_url || DEFAULT_WEB_URL,
        authorized_at: new Date().toISOString()
      };
      ensureDir(MEMORY_DIR);
      fs.writeFileSync(AUTH_PATH, JSON.stringify(auth, null, 2));
      if (fs.existsSync(PENDING_PATH)) fs.unlinkSync(PENDING_PATH);
      jsonOut({ ok: true, auth_file: AUTH_PATH, user: auth.user });
      return;
    }

    const pendingMessage = body.message || '';
    if (body.code !== 20009 && !pendingMessage.includes('等待')) {
      jsonOut({
        ok: false,
        step: 'wait',
        status: result.status,
        code: body.code,
        message: body.message || '授权失败'
      });
      process.exitCode = 1;
      return;
    }

    await new Promise(resolve => setTimeout(resolve, intervalSeconds * 1000));
  }

  jsonOut({ ok: false, step: 'wait', message: '授权等待超时，请重新运行 --start' });
  process.exitCode = 1;
}

async function checkAuth(silent = false) {
  const auth = readJson(AUTH_PATH);
  const { apiUrl } = getConfig();
  if (!auth?.access_token) {
    const result = { ok: false, auth_file: AUTH_PATH, message: '未找到本地授权 token' };
    if (!silent) jsonOut(result);
    return result;
  }

  const result = await requestJson(`${apiUrl}/auth/me`, {
    method: 'GET',
    headers: { Authorization: `Bearer ${auth.access_token}` }
  });
  const payload = {
    ok: result.body.code === 0,
    status: result.status,
    auth_file: AUTH_PATH,
    user: result.body.data || auth.user || null,
    message: result.body.message
  };
  if (!silent) jsonOut(payload);
  return payload;
}

async function doctor() {
  const { apiUrl, webUrl } = getConfig();
  const checks = [];

  try {
    const health = await requestJson(`${apiUrl}/internal/health`, { method: 'GET' });
    checks.push({ name: 'api', ok: health.status < 500, status: health.status });
  } catch (error) {
    checks.push({ name: 'api', ok: false, message: error.message });
  }

  try {
    const response = await fetch(`${webUrl}/auth/device`);
    checks.push({ name: 'web_auth_page', ok: response.status < 500, status: response.status });
  } catch (error) {
    checks.push({ name: 'web_auth_page', ok: false, message: error.message });
  }

  checks.push({ name: 'auth_script', ok: fs.existsSync(__filename), path: __filename });
  checks.push({ name: 'auth_file', ok: fs.existsSync(AUTH_PATH), path: AUTH_PATH });
  checks.push({ name: 'token', ...(await checkAuth(true)) });

  jsonOut({
    ok: checks.every(check => check.ok),
    api_url: apiUrl,
    web_url: webUrl,
    checks
  });
}

function showHelp() {
  jsonOut({
    ok: true,
    usage: [
      'node scripts/auth.js --start',
      'node scripts/auth.js --wait <device_code>',
      'node scripts/auth.js --check',
      'node scripts/auth.js --doctor'
    ]
  });
}

const args = process.argv.slice(2);
const command = args[0];

try {
  if (command === '--start') {
    await startAuth();
  } else if (command === '--wait') {
    await waitForToken(args[1]);
  } else if (command === '--check') {
    await checkAuth();
  } else if (command === '--doctor') {
    await doctor();
  } else {
    showHelp();
  }
} catch (error) {
  jsonOut({ ok: false, message: error.message });
  process.exitCode = 1;
}
