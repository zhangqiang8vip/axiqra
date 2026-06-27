// MCP 端到端测试 + Trace confirm/submit 链路
// 直接调用后端 REST 端点，使用 MCP server 的字段映射逻辑
// （riskLevel 归一化、evidences 构造、workspaceId 解析等）。

import { writeFileSync } from 'fs';

const RAW_API_URL = process.env.AXIQRA_API_URL || 'http://127.0.0.1:8080';
// 兼容：API_URL 可能带 /api 后缀（之前 PowerShell 进程环境残留），去重
const API_URL = RAW_API_URL.replace(/\/api\/?$/, '');

// 1) 登录拿 token
const loginRes = await fetch(`${API_URL}/api/auth/login`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'testuser_309947380', password: 'Test@123456' })
});
const loginBody = await loginRes.json();
if (loginBody.code !== 0) {
  console.error('LOGIN FAILED:', JSON.stringify(loginBody));
  process.exit(1);
}
const token = loginBody.data.token;
const userId = loginBody.data.userId;
console.log('Logged in, userId=', userId);

// 写入 CLI 配置文件
const configPath = `${process.env.USERPROFILE || process.env.HOME}/.axiqra/config.json`;
writeFileSync(configPath, JSON.stringify({
  apiUrl: API_URL,
  token,
  user: { workspaceId: '428103445791625216', userId: String(userId) }
}, null, 2));
console.log('Config written to', configPath);

// 实际查询用户的 workspace（注意：JS Number 可能精度丢失，必须从原始 JSON 文本里提取 ID 字符串）
const wsRes = await fetch(`${API_URL}/api/workspaces`, { headers: { Authorization: token } });
const wsText = await wsRes.text();
const wsBody = JSON.parse(wsText);
// 用 wsBody 中第一条记录的 id，但 JSON 已经被 Jackson 序列化成 Number，
// 后端返回 Long 时精度丢失。这里采取一个 hack：从原始字节里 regex 抓取 19 位 ID
const match = wsText.match(/"id"\s*:\s*(\d{15,})/);
const WORKSPACE_ID = match ? match[1] : '428103445791625216';
const WORKSPACE_ID_NUM = Number(WORKSPACE_ID);
console.log('Resolved WORKSPACE_ID (raw)=', WORKSPACE_ID, ' Number=', WORKSPACE_ID_NUM);
if (WORKSPACE_ID !== String(WORKSPACE_ID_NUM)) {
  console.warn('WARN: workspaceId JS Number 精度丢失！原值:', WORKSPACE_ID, ' 转换后:', WORKSPACE_ID_NUM);
}

const headers = {
  'Content-Type': 'application/json',
  Authorization: token
};

const results = {};

// === 1) axiqra.doctor ===
console.log('\n--- [1] axiqra.doctor (MCP/CLI/API Key 接入诊断) ---');
try {
  const res = await fetch(`${API_URL}/api/mcp/doctor?channel=mcp&toolType=mcp`, { headers });
  const body = await res.json();
  console.log('  code:', body.code, 'message:', body.message);
  if (body.data?.items) {
    body.data.items.forEach(it => console.log('    -', it.name, ':', it.status));
  }
  results.doctor = { ok: body.code === 0 };
} catch (e) {
  console.error('  error:', e.message);
  results.doctor = { ok: false, error: e.message };
}

// === 2) axiqra.search_before_act ===
console.log('\n--- [2] axiqra.search_before_act (search/before-act) ---');
try {
  const res = await fetch(`${API_URL}/api/search/before-act`, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      query: 'login',
      tech_stack: 'Spring Boot',
      environment: 'production',
      risk_hint: 'staging',
      max_results: 5,
      workspace_id: WORKSPACE_ID
    })
  });
  const body = await res.json();
  console.log('  code:', body.code, 'total:', body.data?.total, 'records:', body.data?.results?.length || body.data?.records?.length);
  // Print up to 3 results
  const list = body.data?.results || body.data?.records || [];
  if (list.length > 0) {
    list.slice(0, 3).forEach((r, i) => {
      console.log(`    [${i}]`, (r.title || r.summary || JSON.stringify(r)).substring(0, 100));
    });
  } else {
    console.log('    (no results - search endpoint working but database has no matching rows)');
  }
  results.search = { ok: body.code === 0, total: body.data?.total };
} catch (e) {
  console.error('  error:', e.message);
  results.search = { ok: false, error: e.message };
}

// === 3) axiqra.submit_feedback ===
console.log('\n--- [3] axiqra.submit_feedback (POST /v1/invocations + 可选 /v1/feedbacks) ---');
let invocationId;
try {
  // 模拟 server.mjs 的字段映射
  const invocationPayload = {
    requestId: `mcp-cursor-${Date.now()}`,
    toolName: 'cursor',
    toolVendor: 'Cursor',
    toolType: 'mcp',
    clientChannel: 'mcp',
    targetType: 'solution',
    targetId: 1,
    workspaceId: WORKSPACE_ID, // 保持字符串，避开 Number 精度丢失
    resultType: 'worked',
    feedbackContent: 'E2E test from cursor MCP',
    evidenceRefs: ['mcp-test://cursor-e2e'],
    riskLevel: 1,
    taskGoal: 'cursor MCP e2e feedback',
    idempotencyKey: `mcp-e2e-${Date.now()}`
  };

  const invRes = await fetch(`${API_URL}/api/v1/invocations`, {
    method: 'POST',
    headers,
    body: JSON.stringify(invocationPayload)
  });
  const invBody = await invRes.json();
  invocationId = invBody.data?.id;
  console.log('  invocation code:', invBody.code, 'id:', invocationId);

  // partial / failed 时还要提交 feedback；worked 不需要
  results.feedback = { ok: invBody.code === 0, invocationId };
} catch (e) {
  console.error('  error:', e.message);
  results.feedback = { ok: false, error: e.message };
}

// === 4) axiqra.submit_trace ===
console.log('\n--- [4] axiqra.submit_trace (POST /traces) ---');
let traceId;
try {
  // 模拟 server.mjs 的字段映射
  // context_snapshot / forward_steps / decision_path 都是 jsonb 列，
  // 必须传合法 JSON 字符串（不是 plain text）。
  const tracePayload = {
    workspaceId: WORKSPACE_ID,
    taskGoal: 'cursor MCP e2e trace test',
    toolType: 'cursor',
    contextSnapshot: JSON.stringify({ env: 'e2e', framework: 'cursor' }),
    forwardSteps: JSON.stringify([
      { step: 1, action: 'login user', status: 'success' },
      { step: 2, action: 'search solution', status: 'success' },
      { step: 3, action: 'submit feedback', status: 'success' }
    ]),
    reversePath: JSON.stringify([]),
    decisionPath: JSON.stringify(['chose cursor as tool']),
    outcome: 'success',
    riskLevel: 'R0', // development -> R0
    idempotencyKey: `mcp-trace-${Date.now()}`,
    evidences: [
      { uri: 'mcp-test://cursor-e2e', type: 'file', sizeBytes: 0 }
    ]
  };

  const res = await fetch(`${API_URL}/api/traces`, {
    method: 'POST',
    headers,
    body: JSON.stringify(tracePayload)
  });
  const resText = await res.text();
  const body = JSON.parse(resText);
  // 从原始字节里提取 traceId 字符串（防 Number 精度丢失）
  const m = resText.match(/"id"\s*:\s*(\d{15,})/);
  traceId = m ? m[1] : (body.data?.id ? String(body.data.id) : null);
  console.log('  submit_trace code:', body.code, 'traceId:', traceId, 'status:', body.data?.status);
  results.submitTrace = { ok: body.code === 0, traceId };
} catch (e) {
  console.error('  error:', e.message);
  results.submitTrace = { ok: false, error: e.message };
}

// === 5) Trace confirm + submit chain ===
if (traceId) {
  console.log('\n--- [5a] POST /traces/{id}/confirm ---');
  try {
    const res = await fetch(`${API_URL}/api/traces/${traceId}/confirm`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ userConfirmation: 'OK' })
    });
    const body = await res.json();
    console.log('  confirm code:', body.code, 'status:', body.data?.status);
    results.confirm = { ok: body.code === 0, status: body.data?.status };
  } catch (e) {
    console.error('  error:', e.message);
    results.confirm = { ok: false, error: e.message };
  }

  console.log('\n--- [5b] POST /traces/{id}/submit ---');
  try {
    const res = await fetch(`${API_URL}/api/traces/${traceId}/submit`, {
      method: 'POST',
      headers
    });
    const body = await res.json();
    console.log('  submit code:', body.code, 'status:', body.data?.status);
    results.submit = { ok: body.code === 0, status: body.data?.status };
  } catch (e) {
    console.error('  error:', e.message);
    results.submit = { ok: false, error: e.message };
  }

  console.log('\n--- [5c] GET /traces/{id} (final state) ---');
  try {
    const res = await fetch(`${API_URL}/api/traces/${traceId}`, { headers });
    const body = await res.json();
    console.log('  code:', body.code, 'status:', body.data?.status, 'riskLevel:', body.data?.riskLevel);
    results.finalState = { ok: body.code === 0, status: body.data?.status };
  } catch (e) {
    console.error('  error:', e.message);
    results.finalState = { ok: false, error: e.message };
  }
}

// === 6) axiqra.create_seed ===
console.log('\n--- [6] axiqra.create_seed (POST /seeds, contribution:write) ---');
try {
  const res = await fetch(`${API_URL}/api/seeds`, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      task_goal: 'mcp e2e seed test',
      coverage_gap: 'no solution found via search',
      workspace_id: WORKSPACE_ID
    })
  });
  const body = await res.json();
  console.log('  create_seed code:', body.code, 'seedId:', body.data?.id);
  results.createSeed = { ok: body.code === 0, seedId: body.data?.id };
} catch (e) {
  console.error('  error:', e.message);
  results.createSeed = { ok: false, error: e.message };
}

// === Summary ===
console.log('\n========== SUMMARY ==========');
const fmt = (k, v) => `  ${k.padEnd(14)} ${v.ok ? 'PASS' : 'FAIL'}${v.error ? ' (' + v.error + ')' : ''}`;
console.log(fmt('doctor', results.doctor));
console.log(fmt('search', results.search));
console.log(fmt('feedback', results.feedback));
console.log(fmt('submitTrace', results.submitTrace));
console.log(fmt('confirm', results.confirm || { ok: false, error: 'skipped (no trace)' }));
console.log(fmt('submit', results.submit || { ok: false, error: 'skipped (no trace)' }));
console.log(fmt('finalState', results.finalState || { ok: false, error: 'skipped (no trace)' }));
console.log(fmt('createSeed', results.createSeed));

const allOk = Object.entries(results)
  .filter(([k]) => k !== 'finalState')
  .every(([k, v]) => v.ok);
console.log('\n' + (allOk ? 'ALL PASS' : 'SOME FAILED'));
process.exit(allOk ? 0 : 1);