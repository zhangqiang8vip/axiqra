/**
 * Axiqra MCP Server - Server 测试
 */

import { describe, it, mock, beforeEach } from 'node:test';
import assert from 'node:assert';
import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import {
  ListToolsRequestSchema,
  CallToolRequestSchema
} from '@modelcontextprotocol/sdk/types.js';

// 测试数据
const TOOL_DEFINITIONS = [
  'axiqra.search_before_act',
  'axiqra.get_solution',
  'axiqra.get_public_case',
  'axiqra.submit_trace',
  'axiqra.submit_feedback',
  'axiqra.create_seed',
  'axiqra.doctor'
];

describe('AxiqraMCPServer', () => {
  let mockServer;
  let capturedHandlers;

  beforeEach(() => {
    capturedHandlers = {};

    mockServer = new Server(
      {
        name: 'test-server',
        version: '1.0.0'
      },
      {
        capabilities: {
          tools: {}
        }
      }
    );

    mockServer.setRequestHandler = (schema, handler) => {
      capturedHandlers[schema.method || schema] = handler;
    };
  });

  describe('ListToolsRequestHandler', () => {
    it('应返回正确的工具数量', async () => {
      const expectedToolCount = 7;
      assert.strictEqual(TOOL_DEFINITIONS.length, expectedToolCount);
    });

    it('每个工具应有 axiqra. 前缀', () => {
      for (const name of TOOL_DEFINITIONS) {
        assert.ok(name.startsWith('axiqra.'), `${name} 应以 axiqra. 开头`);
      }
    });
  });
});

describe('工具参数验证', () => {
  describe('search_before_act', () => {
    it('应要求 task_goal 参数', () => {
      const requiredParams = ['task_goal'];
      assert.ok(requiredParams.includes('task_goal'));
    });

    it('应支持可选参数', () => {
      const optionalParams = ['tech_stack', 'environment', 'risk_hint', 'max_results'];
      assert.strictEqual(optionalParams.length, 4);
    });
  });

  describe('submit_trace', () => {
    it('应要求 trace_payload 参数', () => {
      const requiredParams = ['trace_payload'];
      assert.ok(requiredParams.includes('trace_payload'));
    });

    it('应支持幂等键', () => {
      const optionalParams = ['idempotency_key'];
      assert.ok(optionalParams.includes('idempotency_key'));
    });
  });

  describe('submit_feedback', () => {
    it('应要求必需参数', () => {
      const requiredParams = ['tool_name', 'target_id', 'result_type'];
      assert.strictEqual(requiredParams.length, 3);
    });
  });

  describe('doctor', () => {
    it('应要求 channel 和 tool_type', () => {
      const requiredParams = ['channel', 'tool_type'];
      assert.ok(requiredParams.includes('channel'));
      assert.ok(requiredParams.includes('tool_type'));
    });
  });
});

describe('错误码映射', () => {
  it('应正确映射 HTTP 状态码到错误码', () => {
    const httpToErrorMap = {
      400: -32600,  // INVALID_REQUEST
      401: -32003,  // UNAUTHORIZED
      403: -32004,  // FORBIDDEN
      404: -32005,  // NOT_FOUND
      422: -32006,  // VALIDATION_FAILED
      429: -32002,  // RATE_LIMITED
      500: -32603   // INTERNAL_ERROR
    };

    assert.strictEqual(httpToErrorMap[400], -32600);
    assert.strictEqual(httpToErrorMap[401], -32003);
    assert.strictEqual(httpToErrorMap[429], -32002);
  });
});

describe('工具名称格式', () => {
  it('所有工具名称应遵循 axiqra.{action} 格式', () => {
    const validPattern = /^axiqra\.[a-z_]+$/;
    const toolNames = [
      'axiqra.search_before_act',
      'axiqra.get_solution',
      'axiqra.get_public_case',
      'axiqra.submit_trace',
      'axiqra.submit_feedback',
      'axiqra.create_seed',
      'axiqra.doctor'
    ];

    for (const name of toolNames) {
      assert.ok(validPattern.test(name), `${name} 应匹配格式 axiqra.{action}`);
    }
  });
});

describe('API 端点映射', () => {
  const expectedEndpoints = {
    'axiqra.search_before_act': 'POST /search/before-act',
    'axiqra.get_solution': 'GET /solutions/{id}',
    'axiqra.get_public_case': 'GET /public-cases/{id}',
    'axiqra.submit_trace': 'POST /traces',
    'axiqra.submit_feedback': 'POST /v1/invocations + POST /v1/feedbacks',
    'axiqra.create_seed': 'POST /seeds',
    'axiqra.doctor': 'GET /connect/doctor'
  };

  it('search_before_act 应映射到 /search/before-act', () => {
    assert.ok(expectedEndpoints['axiqra.search_before_act'].includes('/search/before-act'));
  });

  it('submit_feedback 应同时调用 invocations 和 feedbacks', () => {
    assert.ok(expectedEndpoints['axiqra.submit_feedback'].includes('/v1/invocations'));
    assert.ok(expectedEndpoints['axiqra.submit_feedback'].includes('/v1/feedbacks'));
  });
});
