/**
 * Axiqra MCP Server - 协议定义
 * 
 * 定义 Axiqra MCP 工具的名称、命令和参数格式
 */

/**
 * MCP 工具定义
 */
export const MCP_TOOL_DEFINITIONS = [
  {
    name: 'axiqra.search_before_act',
    command: 'search-before-act',
    description: '任务前搜索历史方案',
    next: 'search',
    inputSchema: {
      type: 'object',
      properties: {
        task_goal: {
          type: 'string',
          description: '任务目标',
          maxLength: 500
        },
        tech_stack: {
          type: 'string',
          description: '技术栈',
          maxLength: 200
        },
        environment: {
          type: 'string',
          description: '环境',
          maxLength: 200
        },
        risk_hint: {
          type: 'string',
          enum: ['production', 'staging', 'development'],
          description: '风险等级提示'
        },
        max_results: {
          type: 'number',
          minimum: 1,
          maximum: 10,
          default: 5,
          description: '最大返回数量'
        }
      },
      required: ['task_goal']
    },
    outputSchema: {
      type: 'object',
      properties: {
        results: {
          type: 'array',
          items: {
            type: 'object',
            properties: {
              solution_id: { type: 'string' },
              title: { type: 'string' },
              fit_score: { type: 'number' },
              verification_level: { type: 'string' },
              risk_level: { type: 'string' },
              required_confirmation: { type: 'boolean' }
            }
          }
        },
        risk_hints: { type: 'array', items: { type: 'string' } },
        total: { type: 'number' }
      }
    }
  },
  {
    name: 'axiqra.get_solution',
    command: 'get-solution',
    description: '获取 Solution 详情',
    next: 'solution-detail',
    inputSchema: {
      type: 'object',
      properties: {
        solution_id: {
          type: 'string',
          description: 'Solution ID'
        },
        view_mode: {
          type: 'string',
          enum: ['execution', 'full', 'metadata'],
          default: 'execution',
          description: '视图模式'
        }
      },
      required: ['solution_id']
    },
    outputSchema: {
      type: 'object',
      properties: {
        solution_id: { type: 'string' },
        title: { type: 'string' },
        description: { type: 'string' },
        verification_level: { type: 'string' },
        risk_level: { type: 'string' },
        execution_steps: { type: 'array', items: { type: 'string' } },
        verification_steps: { type: 'array', items: { type: 'string' } },
        risk_warnings: { type: 'array', items: { type: 'string' } },
        applicable_scenarios: { type: 'array', items: { type: 'string' } }
      }
    }
  },
  {
    name: 'axiqra.get_public_case',
    command: 'get-public-case',
    description: '获取公开案例',
    next: 'public-case-detail',
    inputSchema: {
      type: 'object',
      properties: {
        public_case_id: {
          type: 'string',
          description: '公开案例 ID'
        },
        view_mode: {
          type: 'string',
          enum: ['learning', 'full', 'metadata'],
          default: 'learning',
          description: '视图模式 (learning: AI可学习视图, full: 完整视图, metadata: 元数据)'
        }
      },
      required: ['public_case_id']
    },
    outputSchema: {
      type: 'object',
      properties: {
        case_id: { type: 'string' },
        title: { type: 'string' },
        problem_description: { type: 'string' },
        solution_summary: { type: 'string' },
        tech_stack: { type: 'array', items: { type: 'string' } },
        verification_level: { type: 'string' },
        risk_level: { type: 'string' },
        failure_patterns: { type: 'array', items: { type: 'string' } },
        lessons_learned: { type: 'array', items: { type: 'string' } },
        applicable_scenarios: { type: 'array', items: { type: 'string' } },
        source_workspace: { type: 'string' },
        published_at: { type: 'string' }
      }
    }
  },
  {
    name: 'axiqra.submit_trace',
    command: 'submit-trace',
    description: '提交工程轨迹包',
    next: 'trace',
    inputSchema: {
      type: 'object',
      properties: {
        trace_payload: {
          type: 'object',
          description: '轨迹数据',
          properties: {
            session_id: { type: 'string' },
            task_goal: { type: 'string' },
            environment: {
              type: 'object',
              properties: {
                tech_stack: { type: 'string' },
                version: { type: 'string' },
                os: { type: 'string' }
              }
            },
            forward_path: { type: 'array' },
            reverse_path: { type: 'array' },
            decision_path: { type: 'array' },
            evidence_refs: { type: 'array', items: { type: 'string' } },
            rollback_path: { type: 'object' },
            outcome: {
              type: 'string',
              enum: ['success', 'failure', 'partial']
            }
          },
          required: ['task_goal', 'outcome']
        },
        idempotency_key: {
          type: 'string',
          description: '幂等键'
        }
      },
      required: ['trace_payload']
    },
    outputSchema: {
      type: 'object',
      properties: {
        trace_id: { type: 'string' },
        status: { type: 'string' },
        missing_fields: { type: 'array', items: { type: 'string' } }
      }
    }
  },
  {
    name: 'axiqra.submit_feedback',
    command: 'submit-feedback',
    description: '提交调用反馈',
    next: 'feedback',
    inputSchema: {
      type: 'object',
      properties: {
        invocation_id: {
          type: 'string',
          description: '调用记录 ID'
        },
        feedback_type: {
          type: 'string',
          enum: ['worked', 'partial', 'failed', 'not_applicable'],
          description: '反馈类型'
        },
        evidence_refs: {
          type: 'array',
          items: { type: 'string' },
          description: '证据引用'
        },
        notes: {
          type: 'string',
          description: '备注'
        },
        failure_reason: {
          type: 'string',
          description: '失败原因（feedback_type 为 failed 时必填）'
        },
        partial_details: {
          type: 'object',
          description: '部分有效详情'
        }
      },
      required: ['invocation_id', 'feedback_type']
    },
    outputSchema: {
      type: 'object',
      properties: {
        feedback_id: { type: 'string' },
        impact: {
          type: 'object',
          properties: {
            solution_verification_level: { type: 'string' },
            weighted_success_rate: { type: 'string' }
          }
        }
      }
    }
  },
  {
    name: 'axiqra.create_seed',
    command: 'create-seed',
    description: '创建候选 Seed',
    next: 'seed',
    inputSchema: {
      type: 'object',
      properties: {
        task_goal: {
          type: 'string',
          description: '任务目标'
        },
        coverage_gap: {
          type: 'string',
          description: '覆盖缺口描述'
        },
        evidence_hint: {
          type: 'string',
          description: '证据提示'
        }
      },
      required: ['task_goal']
    },
    outputSchema: {
      type: 'object',
      properties: {
        seed_id: { type: 'string' },
        status: { type: 'string' }
      }
    }
  },
  {
    name: 'axiqra.doctor',
    command: 'doctor',
    description: '接入诊断',
    next: 'connect',
    inputSchema: {
      type: 'object',
      properties: {
        channel: {
          type: 'string',
          enum: ['mcp', 'cli', 'api'],
          description: '接入渠道'
        },
        tool_type: {
          type: 'string',
          description: '工具类型'
        },
        workspace_id: {
          type: 'string',
          description: '工作空间 ID'
        },
        check: {
          type: 'string',
          enum: ['network', 'auth', 'quota', 'version', 'config', 'storage'],
          description: '单项检查'
        }
      },
      required: ['channel', 'tool_type']
    },
    outputSchema: {
      type: 'object',
      properties: {
        total_checks: { type: 'number' },
        passed_checks: { type: 'number' },
        status: { type: 'string' },
        checks: {
          type: 'array',
          items: {
            type: 'object',
            properties: {
              name: { type: 'string' },
              status: { type: 'string' },
              message: { type: 'string' }
            }
          }
        }
      }
    }
  }
];

/**
 * CLI 命令到 MCP 工具的映射
 */
export const CLI_TO_MCP_MAP = MCP_TOOL_DEFINITIONS.reduce((acc, def) => {
  acc[def.command] = def.name;
  return acc;
}, {});

/**
 * 错误码定义
 */
export const ERROR_CODES = {
  // MCP 错误码 (JSON-RPC 标准)
  PARSE_ERROR: -32700,
  INVALID_REQUEST: -32600,
  METHOD_NOT_FOUND: -32601,
  INVALID_PARAMS: -32602,
  INTERNAL_ERROR: -32603,
  
  // Axiqra 业务错误码
  QUOTA_EXCEEDED: -32001,
  RATE_LIMITED: -32002,
  UNAUTHORIZED: -32003,
  FORBIDDEN: -32004,
  NOT_FOUND: -32005,
  VALIDATION_FAILED: -32006,
  SESSION_NOT_FOUND: -32007,
  TRACE_MISSING_EVIDENCE: -32008,
  
  // HTTP 状态码映射
  HTTP_STATUS_MAP: {
    400: INVALID_REQUEST,
    401: UNAUTHORIZED,
    403: FORBIDDEN,
    404: NOT_FOUND,
    422: VALIDATION_FAILED,
    429: RATE_LIMITED,
    500: INTERNAL_ERROR
  }
};

/**
 * 验证等级
 */
export const VERIFICATION_LEVELS = ['L0', 'L1', 'L2', 'L3', 'L4', 'L5'];

/**
 * 风险等级
 */
export const RISK_LEVELS = ['R0', 'R1', 'R2', 'R3', 'R4'];

/**
 * 反馈类型
 */
export const FEEDBACK_TYPES = ['worked', 'partial', 'failed', 'not_applicable'];

export default {
  MCP_TOOL_DEFINITIONS,
  CLI_TO_MCP_MAP,
  ERROR_CODES,
  VERIFICATION_LEVELS,
  RISK_LEVELS,
  FEEDBACK_TYPES
};
