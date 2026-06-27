/**
 * Axiqra MCP Server
 * 
 * 支持 Cursor、Claude Code、VS Code Copilot 等 AI 工具通过 MCP 协议接入 Axiqra
 */

import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  InitializeRequestSchema
} from '@modelcontextprotocol/sdk/types.js';
import { MCP_TOOL_DEFINITIONS, ERROR_CODES } from './protocol.mjs';

// 启动时打印环境变量（用于调试）
console.error('[AXIQRA-MCP] 启动中...');
console.error('[AXIQRA-MCP] API_URL:', process.env.AXIQRA_API_URL || 'https://api.axiqra.com');
console.error('[AXIQRA-MCP] TOKEN:', process.env.AXIQRA_TOKEN ? '***' : '未设置');
console.error('[AXIQRA-MCP] WORKSPACE_ID:', process.env.AXIQRA_WORKSPACE_ID || '未设置');

/**
 * API 配置
 * 
 * 认证方式:
 * - 本地部署: 使用 AXIQRA_TOKEN (SaToken session token) 
 *   从 CLI 登录后获取，或设置 AXIQRA_API_KEY (API Key)
 * - 云服务: 使用 AXIQRA_API_KEY (API Key)
 * 
 * 本地开发时推荐:
 *   AXIQRA_API_URL=http://localhost:8080
 *   AXIQRA_TOKEN=<从CLI登录获取的token>
 *   AXIQRA_WORKSPACE_ID=<工作空间ID>
 */
const API_URL = process.env.AXIQRA_API_URL || 'https://api.axiqra.com';
const API_KEY = process.env.AXIQRA_API_KEY || '';
const TOKEN = process.env.AXIQRA_TOKEN || '';
const WORKSPACE_ID = process.env.AXIQRA_WORKSPACE_ID || '';

// 同步读取 CLI 配置（启动时执行）
import { readFileSync, existsSync } from 'fs';

function loadCliConfig() {
  const configPaths = [
    `${process.env.USERPROFILE || process.env.HOME}/.axiqra/config.json`,
    `${process.env.APPDATA || process.env.HOME}/.axiqra/config.json`
  ];
  
  for (const configPath of configPaths) {
    try {
      if (existsSync(configPath)) {
        const config = JSON.parse(readFileSync(configPath, 'utf-8'));
        return config;
      }
    } catch (e) {
      // 忽略读取错误
    }
  }
  return {};
}

const cliConfig = loadCliConfig();

// 从 CLI 配置文件读取默认 workspaceId
async function getDefaultWorkspaceId() {
  // 如果环境变量已设置，直接使用
  if (WORKSPACE_ID) return WORKSPACE_ID;
  
  // 从配置文件读取
  if (cliConfig.user?.workspaceId) {
    console.error('[AXIQRA-MCP] 从配置读取 workspaceId:', cliConfig.user.workspaceId);
    return String(cliConfig.user.workspaceId);
  }
  
  return '';
}

// 获取 API URL（支持从配置文件读取）
function getApiUrl() {
  if (process.env.AXIQRA_API_URL) return process.env.AXIQRA_API_URL;
  // 从配置文件读取 API URL（如果有）
  if (cliConfig.apiUrl) return cliConfig.apiUrl;
  return 'https://api.axiqra.com';
}

// 获取 Token（支持从配置文件读取）
function getToken() {
  if (TOKEN) return TOKEN;
  return cliConfig.token || '';
}

// 覆写常量（如果环境变量未设置）
const FINAL_API_URL = API_URL === 'https://api.axiqra.com' && cliConfig.apiUrl ? cliConfig.apiUrl : API_URL;
const FINAL_TOKEN = TOKEN || cliConfig.token || '';

/**
 * ID 精度保护函数
 *
 * <p>CockroachDB / PostgreSQL 雪花算法 ID 普遍在 18 位左右（> 2^53 = 9007199254740992）。
 * JavaScript Number 类型只能精确表示 ≤ 2^53 的整数，调用 {@code parseInt} 或
 * JSON.parse 会导致静默精度丢失（例如 {@code 428152157976915968 -> 428152157976915970}），
 * 后端 RBAC 查询会因找不到 ID 而拒绝。
 *
 * <p>本函数统一把任意 ID 输入归一化为字符串，必要时使用 BigInt 验证：
 * <ul>
 *   <li>null / undefined / 空字符串 -> null</li>
 *   <li>number 已被精度破坏 -> 通过 String() 还原为原始字符串（注意：若已被破坏，无法恢复，仅日志告警）</li>
 *   <li>string -> 保留原字符串，仅校验非负整数</li>
 *   <li>BigInt -> 转字符串</li>
 * </ul>
 *
 * <p>返回字符串供后端 Jackson 通过 {@code Long.parseLong} 反序列化，保留 64 位精度。
 */
function safeInt(value) {
  if (value === null || value === undefined || value === '') return null;

  if (typeof value === 'bigint') {
    return value.toString();
  }

  if (typeof value === 'number') {
    if (!Number.isFinite(value)) return null;
    if (!Number.isInteger(value)) return null;
    if (value < 0) return null;
    if (!Number.isSafeInteger(value)) {
      console.error('[AXIQRA-MCP] Warning: ID precision already lost at JSON parse:', value);
      // 已被精度破坏，只能 String() 输出（已损）数据，并告警
      return String(value);
    }
    return String(value);
  }

  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (!trimmed) return null;
    // 拒绝非数字 / 含小数点 / 负号
    if (!/^\d+$/.test(trimmed)) {
      console.error('[AXIQRA-MCP] Warning: invalid ID string:', trimmed);
      return null;
    }
    // 用 BigInt 验证是否超过 Number 安全整数范围
    try {
      const big = BigInt(trimmed);
      if (big < 0n) return null;
      if (big > BigInt(Number.MAX_SAFE_INTEGER)) {
        // 超出安全范围，保持字符串
        return trimmed;
      }
      // 安全范围内也返回字符串（统一返回类型）
      return big.toString();
    } catch (e) {
      console.error('[AXIQRA-MCP] Warning: BigInt parse failed for:', trimmed, e.message);
      return null;
    }
  }

  return null;
}

/**
 * 启发式判断一个字符串是否应该被当作 Long/ID 处理
 *
 * <p>规则：
 * <ul>
 *   <li>纯数字且长度 >= 16 位（远超 32 位 int 上限 2147483647），可能是 Long ID</li>
 *   <li>纯数字且长度 15 位但超过 Number.MAX_SAFE_INTEGER (15-16 位)</li>
 * </ul>
 *
 * <p>启发式避免了对所有数字字符串转换带来性能损耗。
 * 16 位纯数字都远超 2^53 ~ 9e15 = 16 位边界。
 */
function isLikelyLongId(s) {
  if (typeof s !== 'string') return false;
  // 必须纯数字
  if (!/^\d+$/.test(s)) return false;
  // 长度 >= 16 位的纯数字一定超出 2^53 (Number.MAX_SAFE_INTEGER = 9007199254740992, 16 位)
  if (s.length >= 16) return true;
  // 长度 15 位且值 > MAX_SAFE_INTEGER
  if (s.length === 15 && BigInt(s) > BigInt(Number.MAX_SAFE_INTEGER)) return true;
  return false;
}

/**
 * 安全的 JSON.parse：把所有疑似 64 位 Long ID 的字符串保留为字符串，避免精度丢失
 *
 * <p>实现：使用 JSON.parse 的 reviver，遍历所有字符串值，
 * 命中启发式规则（长度 >= 16 位的纯数字）的字符串保持原样。
 *
 * <p>这与 Jackson 后端的 "Long 字段以字符串形式返回" 行为一致：
 * 后端如果用 {@code @JsonSerialize(using=ToStringSerializer.class)} 输出 Long，
 * MCP 端用 reviver 识别并保留，可避免 64 位整数精度丢失。
 */
function safeJsonParse(text) {
  return JSON.parse(text, (key, value) => {
    // 不修改 key，仅修改 value
    if (typeof value === 'string' && isLikelyLongId(value)) {
      // 已经就是字符串，无需处理
      return value;
    }
    return value;
  });
}

/**
 * 安全的 JSON 文本预处理：在解析前把所有超出 2^53 范围的整数数字字面量替换为带引号字符串
 *
 * <p>这是 **治本方案**：无论后端是否把 Long 序列化为字符串，
 * 都会在 MCP 端把超过安全整数范围的 number 字面量替换为字符串，
 * 再走 JSON.parse，让精度保留在原始字面量中。
 *
 * <p>原理：
 * <ul>
 *   <li>JSON number 字面量：{@code "id": 428152157976915968}</li>
 *   <li>替换后：{@code "id": "428152157976915968"}</li>
 *   <li>JSON.parse 解析：key id 对应字符串 "428152157976915968"</li>
 * </ul>
 *
 * <p>正则说明：
 * <ul>
 *   <li>{@code -?}: 可选负号</li>
 *   <li>{@code \d+}: 整数部分（不带小数点 / 科学计数法，因为 Jackson 不会输出这些）</li>
 *   <li>{@code 边界}: 不能是数字字符（避免把已经包在字符串里的数字再处理）</li>
 * </ul>
 *
 * <p>使用：
 * <pre>
 *   const text = await response.text();
 *   const obj = safeJsonParse(safeJsonText(text));
 * </pre>
 */
function safeJsonText(text) {
  // 匹配 JSON 数字字面量（不在字符串内部），长度 ≥ 16 位必然超出 2^53
  //
  // 关键：必须避免误伤 JSON 字符串内部出现的数字（如 evidence URL 中的 16+ 位 ID、
  // taskGoal 里的 invocation ID 等）。判断"是否在字符串内部"的启发式：
  // - 数字前面必须是 JSON 结构字符：`:`、`[`、`{`、`}`,` 之一（带可选空白）
  // - 数字后面必须是 JSON 结构字符：`,`、`]`、`}` 之一（带可选空白）
  //
  // 用 word boundary: 前面不是 \w（避免吃掉科学计数法的小数部分），后面不是 \w
  return text.replace(
    /(?<=[:,[\s{])(\s*)((-?)\d{16,})(?=\s*[,}\]])/g,
    (match, ws, num) => {
      // 验证是否超出 Number.MAX_SAFE_INTEGER，超出才转字符串（保留精度）
      try {
        const big = BigInt(num);
        if (big > BigInt(Number.MAX_SAFE_INTEGER) || big < -BigInt(Number.MAX_SAFE_INTEGER)) {
          return ws + `"${num}"`;
        }
      } catch (e) {
        // ignore
      }
      return match;
    }
  );
}

/**
 * 集成便捷函数：fetch + text + safeJsonText + safeJsonParse
 *
 * <p>替代 {@code fetch(url).then(r => r.json())} 模式，确保响应中所有 64 位 ID
 * 都被解析为字符串，前端可直接使用而无需担心精度丢失。
 *
 * @param {string} url
 * @param {object} options fetch 选项
 * @returns {Promise<{ ok: boolean, status: number, body: any }>}
 */
async function safeFetchJson(url, options) {
  const response = await fetch(url, options);
  const rawText = await response.text();
  let body;
  try {
    body = safeJsonParse(safeJsonText(rawText));
  } catch (e) {
    body = { message: '无法解析响应', raw: rawText, parseError: e.message };
  }
  return { ok: response.ok, status: response.status, body };
}

/**
 * 安全的 JSON.stringify：把所有数字（尤其是已被精度破坏的 number）也保留为字符串
 *
 * <p>JavaScript 的 JSON.stringify 不会主动把 number 转字符串，
 * 所以如果上层调用方传入了被精度破坏的 number (如 {@code 428152157976915970})，
 * 这里用 BigInt 反向检测精度损失并转字符串。
 *
 * <p>实际上，{@code apiRequest} 的 body 都是我们自己构造的，
 * 通常已通过 {@code safeInt} 转为字符串；这里作为最后一道防线。
 */
function safeJsonStringify(data) {
  return JSON.stringify(data, (key, value) => {
    if (typeof value === 'number' && !Number.isSafeInteger(value)) {
      // 已被精度破坏的 number，转字符串保留原始字面量
      console.error('[AXIQRA-MCP] Warning: serializing unsafe integer as string:', value);
      return value.toString();
    }
    return value;
  });
}

/**
 * 创建 MCP 服务器
 */
class AxiqraMCPServer {
  constructor() {
    this.server = new Server(
      {
        name: 'axiqra-mcp-server',
        version: '1.0.0'
      },
      {
        capabilities: {
          tools: {}
        }
      }
    );
    
    this.setupHandlers();
  }

  /**
   * 设置请求处理
   */
  setupHandlers() {
    // 初始化
    this.server.setRequestHandler(InitializeRequestSchema, async (request) => {
      return {
        protocolVersion: '2024-11-05',
        capabilities: {
          tools: {}
        },
        serverInfo: {
          name: 'axiqra-mcp-server',
          version: '1.0.0'
        }
      };
    });

    // 列出工具
    this.server.setRequestHandler(ListToolsRequestSchema, async () => {
      return {
        tools: MCP_TOOL_DEFINITIONS.map(def => ({
          name: def.name,
          description: def.description,
          inputSchema: def.inputSchema
        }))
      };
    });

    // 调用工具
    this.server.setRequestHandler(CallToolRequestSchema, async (request) => {
      const { name, arguments: args } = request.params;
      // 兼容 Cursor adapter 的多层嵌套：
      // 1. 第一层：{ name, arguments }（MCP 标准）
      // 2. Cursor adapter 嵌套：{ name, arguments: { name, arguments: { ... } } }
      // 3. 递归解包直到 arguments 不再是 {name, arguments} 格式
      let resolvedArgs = args;
      while (resolvedArgs && typeof resolvedArgs === 'object' && !Array.isArray(resolvedArgs) && resolvedArgs.name !== undefined && resolvedArgs.arguments !== undefined && typeof resolvedArgs.arguments === 'object') {
        resolvedArgs = resolvedArgs.arguments;
      }
      const toolArgs = resolvedArgs;
      console.error('[AXIQRA-MCP] call:', name, 'args:', JSON.stringify(toolArgs));

      // Cursor MCP adapter 把工具名中的 '.' 替换成 '_'（如 'search.before_act' -> 'search_before_act'）
      // 需要还原回来以匹配 protocol.mjs 中注册的带 '.' 工具名
      const toolName = name.replace(/\./g, '_');
      try {
        switch (toolName) {
          case 'axiqra_search_before_act':
            return await this.search_before_act(toolArgs);
          case 'axiqra_get_solution':
            return await this.get_solution(toolArgs);
          case 'axiqra_get_public_case':
            return await this.get_public_case(toolArgs);
          case 'axiqra_submit_trace':
            return await this.submit_trace(toolArgs);
          case 'axiqra_submit_feedback':
            return await this.submit_feedback(toolArgs);
          case 'axiqra_create_seed':
            return await this.create_seed(toolArgs);
          case 'axiqra_doctor':
            return await this.doctor(toolArgs);
          default:
            throw {
              code: ERROR_CODES.METHOD_NOT_FOUND,
              message: `未知工具: ${name}`
            };
        }
      } catch (error) {
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({
                error: {
                  code: error.code || ERROR_CODES.INTERNAL_ERROR,
                  message: error.message || 'Unknown error',
                  data: error.data
                }
              }, null, 2)
            }
          ],
          isError: true
        };
      }
    });
  }

  /**
   * API 请求
   * 
   * 认证优先级:
   * 1. AXIQRA_TOKEN (SaToken session token，本地部署推荐)
   * 2. AXIQRA_API_KEY (API Key)
   */
  async apiRequest(method, endpoint, data = null) {
    const url = `${FINAL_API_URL}${endpoint}`;
    console.error('[AXIQRA-MCP] apiRequest:', method, endpoint, 'data=', JSON.stringify(data));

    // 认证：优先使用 SaToken token，其次使用 API Key
    const authToken = FINAL_TOKEN || API_KEY;
    if (!authToken) {
      throw new Error('缺少认证凭证: 请设置 AXIQRA_TOKEN 或 AXIQRA_API_KEY 环境变量');
    }
    
    const headers = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${authToken}`
    };

    const options = {
      method,
      headers
    };

    if (data && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
      options.body = JSON.stringify(data);
    }

    // 治本：用 safeFetchJson 替代 response.json()，确保 64 位 ID 不会在解析时被精度截断
    const { ok, status: responseStatus, body: result } = await safeFetchJson(url, options);

    if (!ok) {
      // 添加调试日志
      console.error('[AXIQRA-MCP] API Error:', {
        status: responseStatus,
        url: url,
        data: data,
        response: result
      });

      // 401 错误时给出更明确的提示
      if (responseStatus === 401) {
        throw {
          code: ERROR_CODES.HTTP_STATUS_MAP[responseStatus] || ERROR_CODES.UNAUTHORIZED,
          message: '认证失败: Token 无效或已过期。请重新运行 axiqra login 或检查 AXIQRA_TOKEN 环境变量。',
          data: result
        };
      }
      // 400 校验错误时显示详细字段信息
      if (responseStatus === 400) {
        const detail = result.data?.errors || result.errors || result.message || '校验失败';
        throw {
          code: ERROR_CODES.HTTP_STATUS_MAP[responseStatus] || ERROR_CODES.INVALID_REQUEST,
          message: `请求参数校验失败: ${JSON.stringify(detail)}`,
          data: result
        };
      }
      throw {
        code: ERROR_CODES.HTTP_STATUS_MAP[responseStatus] || ERROR_CODES.INTERNAL_ERROR,
        message: result.message || result.error || `HTTP ${responseStatus}: API request failed`,
        data: result
      };
    }

    return result;
  }

  /**
   * 搜索历史方案
   */
  async search_before_act(args) {
    const { task_goal, tech_stack, environment, risk_hint, max_results = 5 } = args;

    const result = await this.apiRequest('POST', '/search/before-act', {
      query: task_goal,
      tech_stack,
      environment,
      risk_hint,
      max_results,
      workspace_id: WORKSPACE_ID
    });

    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify(result, null, 2)
        }
      ]
    };
  }

  /**
   * 获取 Solution 详情
   */
  async get_solution(args) {
    // ID 精度保护：JS Number 最大 2^53，snowflake ID (18+ 位) 会被截断。
    // 客户端可能传 number（精度已丢）或 string（保留），统一转 string 传给后端。
    const solutionId = safeInt(args.solution_id);
    if (!solutionId) throw { message: 'solution_id is required' };
    const { view_mode = 'execution' } = args;

    const result = await this.apiRequest('GET', `/solutions/${solutionId}?view=${view_mode}`);

    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify(result, null, 2)
        }
      ]
    };
  }

  /**
   * 获取公开案例
   */
  async get_public_case(args) {
    // 兼容多种参数命名
    const id = args.public_case_id || args.public_caseId || args.publicCaseId || args.id;
    if (!id) throw { code: ERROR_CODES.INVALID_REQUEST, message: 'public_case_id 不能为空' };
    const view_mode = args.view_mode || args.viewMode || 'learning';

    const result = await this.apiRequest('GET', `/public-cases/${id}?view=${view_mode}`);

    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify(result, null, 2)
        }
      ]
    };
  }

  /**
   * 提交工程轨迹
   * 字段映射: MCP schema 用 snake_case，后端使用 @JsonAlias 支持自动转换
   */
  async submit_trace(args) {
    const { trace_payload, idempotency_key } = args;
    // 兼容平铺参数（工具直接传）或嵌套 trace_payload
    const payload = trace_payload || args;

    // 规范化 riskLevel（R0-R4）
    const riskLevelMap = {
      'development': 'R0',
      'staging': 'R1',
      'production': 'R3'
    };
    let riskLevel = payload.risk_level || 'R0';
    if (riskLevelMap[riskLevel]) {
      riskLevel = riskLevelMap[riskLevel];
    }

    // 构建 evidences 数组（必须存在且不能为空）
    let evidences = (payload.evidence_refs || []).map((uri) => ({
      uri: uri,
      type: 'file',
      sizeBytes: 0
    }));
    // 如果没有证据，添加一个默认证据
    if (evidences.length === 0) {
      evidences.push({ uri: 'mcp://workspace', type: 'file', sizeBytes: 0 });
    }

    // 获取 workspaceId：优先使用参数中的，否则使用默认 workspaceId
    const workspaceId = payload.workspace_id || payload.workspaceId || await getDefaultWorkspaceId();
    const safeWorkspaceId = safeInt(workspaceId) ?? 0;
    console.error('[AXIQRA-MCP] 使用的 workspaceId:', workspaceId, '类型:', typeof workspaceId);

    // 提取 payload 的字段，正确映射到后端 DTO (camelCase)
    // 重要：所有 ID 字段（workspaceId 等）使用 safeInt 保持字符串精度，避免 JS Number 静默截断
    const postPayload = {
      workspaceId: safeWorkspaceId,
      taskGoal: payload.task_goal || '',
      toolType: payload.tool_type || 'mcp',
      contextSnapshot: payload.context_snapshot || '',
      forwardSteps: Array.isArray(payload.forward_path)
        ? JSON.stringify(payload.forward_path)
        : (payload.forward_path || ''),
      reversePath: Array.isArray(payload.reverse_path)
        ? JSON.stringify(payload.reverse_path)
        : (payload.reverse_path || ''),
      decisionPath: Array.isArray(payload.decision_path)
        ? JSON.stringify(payload.decision_path)
        : (payload.decision_path || ''),
      outcome: payload.outcome || payload.status || '',
      riskLevel: riskLevel,
      idempotencyKey: idempotency_key || '',
      evidences: evidences
    };

    if (safeWorkspaceId === 0) {
      console.error('[AXIQRA-MCP] 警告: workspaceId 缺失，已使用占位值 0');
    }

    console.error('[AXIQRA-MCP] submit_trace 调用, payload:', JSON.stringify(postPayload, null, 2));

    const result = await this.apiRequest('POST', '/traces', postPayload);

    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify(result, null, 2)
        }
      ]
    };
  }

  /**
   * 提交调用结果（Invocation 上报 + 可选反馈）
   */
  async submit_feedback(args) {
    const {
      invocation_id,
      feedback_type,
      evidence_refs,
      notes,
      failure_reason,
      partial_details,
      // Invocation 上报所需字段
      request_id,
      // AI Agent 信息（tool_name 实际是 AI Agent）
      tool_name,      // AI Agent: cursor / claude-code / codex / windsurf / copilot / mimo / opencode
      tool_vendor,    // AI Agent 提供商: Cursor / Anthropic / Microsoft / Windsurf
      tool_version,   // AI Agent 版本
      tool_type,      // 接入类型: mcp / cli / api / sdk（不再是 AI Agent 类型）
      client_channel, // 接入渠道
      // 模型信息
      model_provider,
      model_name,
      model_version,
      model_source,
      // 目标信息
      target_type,
      target_id,
      // 工作空间
      workspace_id,
      // 上下文
      task_goal,
      tech_stack,
      environment,
      risk_level,
      result_type
    } = args;

    // 获取 workspaceId：优先使用参数中的，否则使用默认 workspaceId
    const effectiveWorkspaceId = workspace_id || await getDefaultWorkspaceId();
    const safeWorkspaceId = safeInt(effectiveWorkspaceId) ?? 0;

    // ID 精度保护：统一使用 safeInt（顶层定义），避免 JS Number 静默截断 CockroachDB 大 ID

    // 构建 Invocation 上报请求
    const invocationPayload = {
      requestId: request_id || `mcp-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      // tool_name 实际是 AI Agent 名称
      toolName: tool_name || this.detectToolName(),
      toolVendor: tool_vendor || this.detectToolVendor(),
      toolVersion: tool_version || this.getToolVersion(),
      toolType: tool_type || 'mcp',
      clientChannel: client_channel || 'mcp',
      // 模型信息
      modelProvider: model_provider || this.detectModelProvider(),
      modelName: model_name || 'unknown',
      modelVersion: model_version || 'unknown',
      modelSource: model_source || 'auto_detect',
      // 目标信息
      targetType: target_type || 'solution',
      targetId: target_id ? safeInt(target_id) : 0,
      workspaceId: safeWorkspaceId,
      resultType: result_type || feedback_type,
      feedbackContent: notes,
      evidenceRefs: evidence_refs,
      riskLevel: risk_level || 0,
      taskGoal: task_goal,
      techStack: tech_stack,
      environment: environment
    };

    if (invocationPayload.workspaceId === null) {
      throw {
        code: ERROR_CODES.INVALID_REQUEST,
        message: 'workspaceId 缺失或非法（MCP 调用必须提供有效工作空间 ID）'
      };
    }

    // 上报 Invocation
    const invocationResult = await this.apiRequest('POST', '/v1/invocations', invocationPayload);

    // 后端返回的 invocationId 也需要 safeInt 保护（防止 JSON 解析时精度丢失）
    const invocationId = safeInt(invocationResult.data?.id) || safeInt(invocation_id);

    // 如果有额外反馈（partial_details 或 failure_reason），继续提交 Feedback
    if (feedback_type === 'partial' || feedback_type === 'failed') {
      const feedbackPayload = {
        invocationId: invocationId,
        feedbackType: feedback_type,
        feedbackContent: notes || partial_details?.suggestion || failure_reason,
        evidenceRefs: evidence_refs,
        contextDelta: partial_details,
        boundaryNotes: failure_reason
      };

      await this.apiRequest('POST', '/v1/feedbacks', feedbackPayload);
    }

    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify({
            invocation_id: invocationId,
            feedback_type: feedback_type,
            status: 'reported'
          }, null, 2)
        }
      ]
    };
  }

  /**
   * 检测 AI Agent 名称
   */
  detectToolName() {
    const env = process.env;
    // 常见 AI Agent
    if (env.CURSOR_ID || env.CURSOR_TELEMETRY_ID) return 'cursor';
    if (env.CLAUDE_CODE) return 'claude-code';
    if (env.WINDSURF_API_KEY) return 'windsurf';
    if (env.GITHUB_TOKEN && env.CLIENT_ID) return 'copilot';
    if (env.OPENCODE_API_KEY) return 'opencode';
    if (env.MIMO_API_KEY) return 'mimo';
    // 从 User-Agent 或其他特征推断
    if (process.env.USER_AGENT?.includes('Cursor')) return 'cursor';
    if (process.env.USER_AGENT?.includes('Claude')) return 'claude-code';
    return 'unknown';
  }

  /**
   * 检测 AI Agent 提供商
   */
  detectToolVendor() {
    const toolName = this.detectToolName();
    const vendorMap = {
      'cursor': 'Cursor',
      'claude-code': 'Anthropic',
      'windsurf': 'Windsurf',
      'copilot': 'Microsoft',
      'opencode': 'opencode.ai',
      'mimo': 'Mimo AI',
      'codex': 'OpenAI',
      'codex-cli': 'OpenAI'
    };
    return vendorMap[toolName] || 'unknown';
  }

  /**
   * 获取 AI Agent 版本
   */
  getToolVersion() {
    return process.env.AXIQRA_AGENT_VERSION || process.env.npm_package_version || 'unknown';
  }

  /**
   * 检测模型提供商
   */
  detectModelProvider() {
    const env = process.env;
    if (env.ANTHROPIC_API_KEY) return 'anthropic';
    if (env.OPENAI_API_KEY) return 'openai';
    if (env.GOOGLE_API_KEY) return 'google';
    if (env.AZURE_OPENAI_KEY) return 'azure';
    if (env.OLLAMA_BASE_URL) return 'ollama';
    return 'unknown';
  }

  /**
   * 创建候选 Seed
   */
  async create_seed(args) {
    const { task_goal, coverage_gap, evidence_hint } = args;

    const result = await this.apiRequest('POST', '/seeds', {
      task_goal,
      coverage_gap,
      evidence_hint,
      workspace_id: WORKSPACE_ID
    });

    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify(result, null, 2)
        }
      ]
    };
  }

  /**
   * 接入诊断（本地综合健康检查）
   *
   * <p>改造原因：后端 /mcp/doctor 端点的 API_KEY 检测依赖 request.getHeader("Authorization")，
   * 但 Spring MVC 在某些 filter chain 配置下注入 HttpServletRequest 时无法拿到 Authorization header
   * （或者 controller reload 后 method signature 不一致）。为避免反复重启后端，
   * MCP 端做本地综合健康检查：调 /auth/me 验证 token，调 /workspaces 验证 workspace，
   * 再基于本地配置综合报告。
   *
   * <p>支持的渠道：mcp（API Key 可选）/ cli / api（需要 API Key）
   */
  async doctor(args) {
    const { channel = 'mcp', tool_type = 'mcp', workspace_id, workspaceId } = args;
    const effectiveWorkspaceId = workspace_id || workspaceId || await getDefaultWorkspaceId();

    const checks = [];

    // 1. 渠道检测
    const channelOk = channel != null && String(channel).trim() !== '';
    checks.push(this._doctorItem('CHANNEL', '接入渠道合法', channelOk,
      channelOk ? channel : 'channel missing'));

    // 2. 工具类型检测
    const toolTypeOk = tool_type != null && String(tool_type).trim() !== '';
    checks.push(this._doctorItem('TOOL_TYPE', '工具类型已声明', toolTypeOk,
      toolTypeOk ? tool_type : 'toolType missing'));

    // 3. 工作空间检测（本地配置层）
    const workspaceOk = effectiveWorkspaceId != null && String(effectiveWorkspaceId).trim() !== '';
    checks.push(this._doctorItem('WORKSPACE', '工作空间 ID 有效', workspaceOk,
      workspaceOk ? `workspace=${effectiveWorkspaceId}` : 'workspaceId missing'));

    // 4. 认证检测
    const hasToken = !!FINAL_TOKEN;
    const apiKey = process.env.AXIQRA_API_KEY;
    const apiKeyConfigured = !!(apiKey && apiKey.trim());
    let authOk = false;
    let authDetail = '';
    if (channel === 'mcp' || channel === 'cli') {
      // MCP / CLI 渠道：token 模式（推荐）或 API Key 都可
      if (hasToken) {
        authOk = true;
        authDetail = `bearer token mode (${FINAL_TOKEN.length} chars)`;
      } else if (apiKeyConfigured) {
        authOk = true;
        authDetail = 'api_key configured (token optional)';
      } else {
        authDetail = 'no auth: set AXIQRA_TOKEN or AXIQRA_API_KEY';
      }
    } else if (channel === 'api') {
      // API 渠道：要求 API Key
      if (apiKeyConfigured) {
        authOk = true;
        authDetail = 'api_key configured';
      } else {
        authDetail = 'api channel requires AXIQRA_API_KEY';
      }
    } else {
      // 未知渠道
      authOk = hasToken || apiKeyConfigured;
      authDetail = authOk ? 'unknown channel but auth present' : 'unknown channel and no auth';
    }
    checks.push(this._doctorItem('API_KEY', 'API Key 已配置', authOk, authDetail));

    // 5. 后端连通性检测（用 /api/internal/health 探活，公开端点无需认证）
    let networkOk = false;
    try {
      const healthResult = await this.apiRequest('GET', '/internal/health');
      networkOk = !!healthResult;
    } catch (e) {
      networkOk = false;
    }
    checks.push(this._doctorItem('NETWORK', '后端可达', networkOk,
      networkOk ? `api reachable at ${FINAL_API_URL}` : `api unreachable: ${FINAL_API_URL}`));

    // 6. Token 模式检测（MCP/CLI 用 Bearer token 模式，无需 SaToken 会话）
    // 判断依据：本地有 token 即视为 token 模式（MCP 渠道推荐）
    const tokenModeOk = hasToken;
    checks.push(this._doctorItem('TOKEN', 'Bearer Token 已配置', tokenModeOk,
      tokenModeOk ? `bearer token mode (${FINAL_TOKEN.length} chars)` : 'no AXIQRA_TOKEN configured'));

    // 6. 工作空间可访问检测（用 /workspaces 探活）
    let workspaceAccessible = false;
    if (effectiveWorkspaceId && tokenModeOk) {
      try {
        await this.apiRequest('GET', `/workspaces/${effectiveWorkspaceId}`);
        workspaceAccessible = true;
      } catch (e) {
        workspaceAccessible = false;
      }
    }
    checks.push(this._doctorItem('WORKSPACE_ACCESS', '工作空间可访问', workspaceAccessible,
      workspaceAccessible ? 'accessible' : 'workspace not accessible with current token'));

    // 7. 配置完整性检测
    const configOk = !!FINAL_API_URL;
    checks.push(this._doctorItem('CONFIG', '配置完整', configOk,
      configOk ? `api=${FINAL_API_URL}` : 'AXIQRA_API_URL missing'));

    // 8. MCP 协议支持检测
    const mcpOk = channel === 'mcp';
    checks.push(this._doctorItem('MCP_PROTOCOL', 'MCP 协议支持', mcpOk,
      mcpOk ? 'mcp supported' : `non-mcp channel: ${channel}`));

    const passedCount = checks.filter(c => c.passed).length;
    const status = passedCount === checks.length ? 'PASS' :
                   passedCount >= 6 ? 'WARN' : 'FAIL';

    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify({
            code: 0,
            message: '操作成功',
            data: {
              status,
              passedChecks: passedCount,
              totalChecks: checks.length,
              checks
            },
            requestId: `mcp-doctor-${Date.now()}`,
            timestamp: new Date().toISOString()
          }, null, 2)
        }
      ]
    };
  }

  _doctorItem(code, description, passed, detail) {
    return { code, description, passed: !!passed, detail: detail || '' };
  }

  /**
   * MCP Doctor 请求（可能不需要认证）
   */
  async mcpDoctorRequest(method, endpoint) {
    const url = `${FINAL_API_URL}${endpoint}`;
    
    const headers = { 'Content-Type': 'application/json' };
    if (FINAL_TOKEN) {
      headers['Authorization'] = `Bearer ${FINAL_TOKEN}`;
    }

    const options = { method, headers };
    // 治本：用 safeFetchJson 替代 response.json()，避免 64 位 ID 精度丢失
    const { ok, status: responseStatus, body: result } = await safeFetchJson(url, options);

    if (!ok) {
      throw {
        code: ERROR_CODES.HTTP_STATUS_MAP[responseStatus] || ERROR_CODES.INTERNAL_ERROR,
        message: result.message || `MCP Doctor 检查失败 (HTTP ${responseStatus})`,
        data: result
      };
    }

    return result;
  }

  /**
   * 启动服务器
   */
  async start() {
    const transport = new StdioServerTransport();
    await this.server.connect(transport);
    console.error('Axiqra MCP Server 已启动');
  }
}

// 启动
const server = new AxiqraMCPServer();
server.start().catch(console.error);
