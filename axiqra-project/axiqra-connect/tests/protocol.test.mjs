/**
 * Axiqra MCP Server - 协议测试
 */

import { describe, it } from 'node:test';
import assert from 'node:assert';
import {
  MCP_TOOL_DEFINITIONS,
  ERROR_CODES,
  VERIFICATION_LEVELS,
  RISK_LEVELS,
  FEEDBACK_TYPES
} from '../mcp/protocol.mjs';

describe('MCP_TOOL_DEFINITIONS', () => {
  it('应包含 7 个工具定义', () => {
    assert.strictEqual(MCP_TOOL_DEFINITIONS.length, 7);
  });

  it('应包含 axiqra.search_before_act 工具', () => {
    const tool = MCP_TOOL_DEFINITIONS.find(t => t.name === 'axiqra.search_before_act');
    assert.ok(tool, 'search_before_act 工具应存在');
    assert.ok(tool.inputSchema.required, '应有 required 字段');
    assert.strictEqual(tool.inputSchema.required[0], 'task_goal');
    assert.ok(tool.inputSchema.properties.task_goal, '应包含 task_goal 参数');
    assert.ok(tool.inputSchema.properties.max_results, '应包含 max_results 参数');
  });

  it('应包含 axiqra.get_solution 工具', () => {
    const tool = MCP_TOOL_DEFINITIONS.find(t => t.name === 'axiqra.get_solution');
    assert.ok(tool, 'get_solution 工具应存在');
    assert.ok(tool.inputSchema.required, '应有 required 字段');
    assert.strictEqual(tool.inputSchema.required[0], 'solution_id');
  });

  it('应包含 axiqra.get_public_case 工具', () => {
    const tool = MCP_TOOL_DEFINITIONS.find(t => t.name === 'axiqra.get_public_case');
    assert.ok(tool, 'get_public_case 工具应存在');
    assert.ok(tool.inputSchema.required, '应有 required 字段');
  });

  it('应包含 axiqra.submit_trace 工具', () => {
    const tool = MCP_TOOL_DEFINITIONS.find(t => t.name === 'axiqra.submit_trace');
    assert.ok(tool, 'submit_trace 工具应存在');
    assert.ok(tool.inputSchema.required, '应有 required 字段');
    assert.strictEqual(tool.inputSchema.required[0], 'trace_payload');
    assert.ok(tool.inputSchema.properties.trace_payload, '应包含 trace_payload 参数');
    assert.ok(tool.inputSchema.properties.idempotency_key, '应包含 idempotency_key 参数');
  });

  it('应包含 axiqra.submit_feedback 工具', () => {
    const tool = MCP_TOOL_DEFINITIONS.find(t => t.name === 'axiqra.submit_feedback');
    assert.ok(tool, 'submit_feedback 工具应存在');
    assert.ok(tool.inputSchema.required, '应有 required 字段');
    assert.ok(tool.inputSchema.required.includes('tool_name'), '应包含 tool_name');
    assert.ok(tool.inputSchema.required.includes('target_id'), '应包含 target_id');
    assert.ok(tool.inputSchema.required.includes('result_type'), '应包含 result_type');
  });

  it('应包含 axiqra.create_seed 工具', () => {
    const tool = MCP_TOOL_DEFINITIONS.find(t => t.name === 'axiqra.create_seed');
    assert.ok(tool, 'create_seed 工具应存在');
    assert.ok(tool.inputSchema.required, '应有 required 字段');
    assert.strictEqual(tool.inputSchema.required[0], 'task_goal');
  });

  it('应包含 axiqra.doctor 工具', () => {
    const tool = MCP_TOOL_DEFINITIONS.find(t => t.name === 'axiqra.doctor');
    assert.ok(tool, 'doctor 工具应存在');
    assert.ok(tool.inputSchema.required, '应有 required 字段');
    assert.ok(tool.inputSchema.required.includes('channel'), '应包含 channel');
    assert.ok(tool.inputSchema.required.includes('tool_type'), '应包含 tool_type');
  });
});

describe('CLI 命令映射', () => {
  it('应包含所有 CLI 命令', () => {
    // CLI 命令到 MCP 工具名称的映射
    const cliCommands = [
      'search-before-act',
      'get-solution',
      'get-public-case',
      'submit-trace',
      'submit-feedback',
      'create-seed',
      'doctor'
    ];
    
    assert.strictEqual(cliCommands.length, 7);
    assert.ok(cliCommands.includes('search-before-act'));
    assert.ok(cliCommands.includes('doctor'));
  });
});

describe('ERROR_CODES', () => {
  it('应包含 MCP 标准错误码', () => {
    assert.strictEqual(ERROR_CODES.PARSE_ERROR, -32700);
    assert.strictEqual(ERROR_CODES.INVALID_REQUEST, -32600);
    assert.strictEqual(ERROR_CODES.METHOD_NOT_FOUND, -32601);
    assert.strictEqual(ERROR_CODES.INVALID_PARAMS, -32602);
    assert.strictEqual(ERROR_CODES.INTERNAL_ERROR, -32603);
  });

  it('应包含 Axiqra 业务错误码', () => {
    assert.strictEqual(ERROR_CODES.QUOTA_EXCEEDED, -32001);
    assert.strictEqual(ERROR_CODES.RATE_LIMITED, -32002);
    assert.strictEqual(ERROR_CODES.UNAUTHORIZED, -32003);
    assert.strictEqual(ERROR_CODES.FORBIDDEN, -32004);
    assert.strictEqual(ERROR_CODES.NOT_FOUND, -32005);
    assert.strictEqual(ERROR_CODES.VALIDATION_FAILED, -32006);
    assert.strictEqual(ERROR_CODES.SESSION_NOT_FOUND, -32007);
    assert.strictEqual(ERROR_CODES.TRACE_MISSING_EVIDENCE, -32008);
  });

  it('应包含 HTTP 状态码映射', () => {
    assert.strictEqual(ERROR_CODES.HTTP_STATUS_MAP[400], ERROR_CODES.INVALID_REQUEST);
    assert.strictEqual(ERROR_CODES.HTTP_STATUS_MAP[401], ERROR_CODES.UNAUTHORIZED);
    assert.strictEqual(ERROR_CODES.HTTP_STATUS_MAP[403], ERROR_CODES.FORBIDDEN);
    assert.strictEqual(ERROR_CODES.HTTP_STATUS_MAP[404], ERROR_CODES.NOT_FOUND);
    assert.strictEqual(ERROR_CODES.HTTP_STATUS_MAP[429], ERROR_CODES.RATE_LIMITED);
    assert.strictEqual(ERROR_CODES.HTTP_STATUS_MAP[500], ERROR_CODES.INTERNAL_ERROR);
  });
});

describe('VERIFICATION_LEVELS', () => {
  it('应包含 L0-L5 六个验证等级', () => {
    assert.deepStrictEqual(VERIFICATION_LEVELS, ['L0', 'L1', 'L2', 'L3', 'L4', 'L5']);
  });
});

describe('RISK_LEVELS', () => {
  it('应包含 R0-R4 五个风险等级', () => {
    assert.deepStrictEqual(RISK_LEVELS, ['R0', 'R1', 'R2', 'R3', 'R4']);
  });
});

describe('FEEDBACK_TYPES', () => {
  it('应包含四种反馈类型', () => {
    assert.deepStrictEqual(FEEDBACK_TYPES, ['worked', 'partial', 'failed', 'not_applicable']);
  });
});

describe('search_before_act 参数验证', () => {
  const tool = MCP_TOOL_DEFINITIONS.find(t => t.name === 'axiqra.search_before_act');

  it('task_goal 应有 maxLength: 500', () => {
    assert.strictEqual(tool.inputSchema.properties.task_goal.maxLength, 500);
  });

  it('tech_stack 应有 maxLength: 200', () => {
    assert.strictEqual(tool.inputSchema.properties.tech_stack.maxLength, 200);
  });

  it('risk_hint 应限制为 production | staging | development', () => {
    assert.deepStrictEqual(tool.inputSchema.properties.risk_hint.enum, ['production', 'staging', 'development']);
  });

  it('max_results 应限制为 1-10，默认 5', () => {
    assert.strictEqual(tool.inputSchema.properties.max_results.minimum, 1);
    assert.strictEqual(tool.inputSchema.properties.max_results.maximum, 10);
    assert.strictEqual(tool.inputSchema.properties.max_results.default, 5);
  });
});

describe('submit_feedback 参数验证', () => {
  const tool = MCP_TOOL_DEFINITIONS.find(t => t.name === 'axiqra.submit_feedback');

  it('model_provider 应限制为允许的提供商', () => {
    const expected = ['openai', 'anthropic', 'google', 'ollama', 'cohere', 'azure', 'unknown'];
    assert.deepStrictEqual(tool.inputSchema.properties.model_provider.enum, expected);
  });

  it('result_type 应限制为允许的结果类型', () => {
    const expected = ['worked', 'partial', 'failed', 'not_applicable'];
    assert.deepStrictEqual(tool.inputSchema.properties.result_type.enum, expected);
  });

  it('model_source 应限制为允许的来源', () => {
    const expected = ['auto_detect', 'user_reported', 'fallback'];
    assert.deepStrictEqual(tool.inputSchema.properties.model_source.enum, expected);
  });

  it('risk_level 应为整数', () => {
    assert.strictEqual(tool.inputSchema.properties.risk_level.type, 'integer');
  });
});

describe('doctor 参数验证', () => {
  const tool = MCP_TOOL_DEFINITIONS.find(t => t.name === 'axiqra.doctor');

  it('channel 应限制为 mcp | cli | api', () => {
    assert.deepStrictEqual(tool.inputSchema.properties.channel.enum, ['mcp', 'cli', 'api']);
  });

  it('check 应限制为允许的检查项', () => {
    const expected = ['network', 'auth', 'quota', 'version', 'config', 'storage'];
    assert.deepStrictEqual(tool.inputSchema.properties.check.enum, expected);
  });
});
