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

/**
 * API 配置
 */
const API_URL = process.env.AXIQRA_API_URL || 'https://api.axiqra.com';
const API_KEY = process.env.AXIQRA_API_KEY || '';
const WORKSPACE_ID = process.env.AXIQRA_WORKSPACE_ID || '';

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

      try {
        switch (name) {
          case 'axiqra.search_before_act':
            return await this.search_before_act(args);
          case 'axiqra.get_solution':
            return await this.get_solution(args);
          case 'axiqra.get_public_case':
            return await this.get_public_case(args);
          case 'axiqra.submit_trace':
            return await this.submit_trace(args);
          case 'axiqra.submit_feedback':
            return await this.submit_feedback(args);
          case 'axiqra.create_seed':
            return await this.create_seed(args);
          case 'axiqra.doctor':
            return await this.doctor(args);
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
   */
  async apiRequest(method, endpoint, data = null) {
    const url = `${API_URL}${endpoint}`;
    const headers = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${API_KEY}`
    };

    const options = {
      method,
      headers
    };

    if (data && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
      options.body = JSON.stringify(data);
    }

    const response = await fetch(url, options);
    const result = await response.json();

    if (!response.ok) {
      throw {
        code: ERROR_CODES.HTTP_STATUS_MAP[response.status] || ERROR_CODES.INTERNAL_ERROR,
        message: result.message || result.error || 'API request failed',
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
    const { solution_id, view_mode = 'execution' } = args;

    const result = await this.apiRequest('GET', `/solutions/${solution_id}?view=${view_mode}`);

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
    const { public_case_id, view_mode = 'learning' } = args;

    const result = await this.apiRequest('GET', `/public-cases/${public_case_id}?view=${view_mode}`);

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
   */
  async submit_trace(args) {
    const { trace_payload, idempotency_key } = args;

    const result = await this.apiRequest('POST', '/traces', {
      trace: trace_payload,
      idempotency_key,
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
   * 提交反馈
   */
  async submit_feedback(args) {
    const { invocation_id, feedback_type, evidence_refs, notes, failure_reason, partial_details } = args;

    const result = await this.apiRequest('POST', '/v1/feedbacks', {
      invocation_id,
      feedback_type,
      evidence_refs,
      notes,
      failure_reason,
      partial_details,
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
   * 接入诊断
   */
  async doctor(args) {
    const { channel, tool_type, workspace_id, check } = args;

    let endpoint = `/connect/doctor?channel=${channel}&toolType=${tool_type}`;
    if (workspace_id) endpoint += `&workspaceId=${workspace_id}`;
    if (check) endpoint += `&check=${check}`;

    const result = await this.apiRequest('GET', endpoint);

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
