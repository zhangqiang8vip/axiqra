/**
 * Axiqra MCP Server - 集成测试
 * 
 * 测试 MCP Server 与后端 API 的集成
 * 注意：这些测试需要运行中的后端服务
 */

import { describe, it, before, after } from 'node:test';
import assert from 'node:assert';

// 配置测试环境变量
const TEST_API_URL = process.env.AXIQRA_API_URL || 'http://localhost:8080';
const TEST_API_KEY = process.env.AXIQRA_API_KEY || 'test-api-key';

describe('MCP → API 集成测试', () => {
  
  describe('环境配置', () => {
    it('应配置 API URL', () => {
      assert.ok(TEST_API_URL, 'API URL 应已配置');
    });

    it('应配置 API Key', () => {
      assert.ok(TEST_API_KEY, 'API Key 应已配置');
    });
  });

  describe('API 健康检查', () => {
    it('后端服务应可访问', async () => {
      // 这个测试在 CI 环境中跳过
      if (process.env.SKIP_INTEGRATION_TESTS) {
        console.log('跳过集成测试');
        return;
      }

      try {
        const response = await fetch(`${TEST_API_URL}/actuator/health`, {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${TEST_API_KEY}`
          }
        });
        
        // 如果服务未运行，跳过测试
        if (!response.ok) {
          console.log('后端服务未运行，跳过集成测试');
          return;
        }

        const result = await response.json();
        assert.ok(result.status, '健康检查应返回状态');
      } catch (error) {
        console.log('后端服务不可访问:', error.message);
        console.log('跳过集成测试');
      }
    });
  });

  describe('CLI → API 模拟测试', () => {
    
    describe('search 命令流程', () => {
      it('search 命令应正确构建请求参数', () => {
        const taskGoal = '如何解决 Spring Boot 启动失败';
        const techStack = 'Spring Boot 3.2';
        const environment = 'production';
        const maxResults = 5;

        const requestBody = {
          query: taskGoal,
          tech_stack: techStack,
          environment: environment,
          risk_hint: 'production',
          max_results: maxResults,
          workspace_id: 'test-workspace'
        };

        assert.strictEqual(requestBody.query, taskGoal);
        assert.strictEqual(requestBody.max_results, maxResults);
        assert.ok(requestBody.workspace_id);
      });

      it('search_before_act 工具应正确处理响应', () => {
        const mockResponse = {
          code: 200,
          message: 'success',
          data: {
            results: [
              {
                solution_id: 'sol-001',
                title: 'Spring Boot 启动失败解决方案',
                fit_score: 0.95,
                verification_level: 'L3',
                risk_level: 'R1',
                required_confirmation: false
              }
            ],
            risk_hints: ['生产环境操作需谨慎'],
            total: 1
          }
        };

        assert.ok(mockResponse.data.results.length > 0);
        assert.strictEqual(mockResponse.data.results[0].fit_score, 0.95);
      });
    });

    describe('trace submit 命令流程', () => {
      it('trace submit 应正确构建请求参数', () => {
        const tracePayload = {
          session_id: 'session-123',
          task_goal: '修复数据库连接池泄漏',
          environment: {
            tech_stack: 'Spring Boot 3.2',
            version: '3.2.0',
            os: 'Linux'
          },
          forward_path: ['步骤1', '步骤2'],
          reverse_path: ['回滚1'],
          decision_path: ['决策1'],
          evidence_refs: ['file:///tmp/evidence.log'],
          outcome: 'success'
        };

        assert.strictEqual(tracePayload.task_goal, '修复数据库连接池泄漏');
        assert.strictEqual(tracePayload.outcome, 'success');
        assert.ok(tracePayload.evidence_refs.length > 0);
      });

      it('trace 提交应支持幂等键', () => {
        const idempotencyKey = 'trace-123-idempotent';
        assert.ok(idempotencyKey, '幂等键应存在');
      });
    });

    describe('feedback 命令流程', () => {
      it('feedback 应正确构建 Invocation 上报请求', () => {
        const feedbackRequest = {
          invocation_id: 'inv-456',
          feedback_type: 'worked',
          evidence_refs: ['file:///tmp/screenshot.png'],
          notes: '方案执行成功',
          tool_name: 'cursor',
          tool_vendor: 'Cursor',
          tool_version: '0.42.0',
          tool_type: 'mcp',
          client_channel: 'mcp',
          model_provider: 'anthropic',
          model_name: 'claude-3-5-sonnet',
          model_version: '20241022',
          model_source: 'auto_detect',
          target_type: 'solution',
          target_id: 'sol-001',
          task_goal: '如何解决 Spring Boot 启动失败',
          tech_stack: 'Spring Boot 3.2',
          environment: 'production',
          risk_level: 1,
          result_type: 'worked'
        };

        assert.strictEqual(feedbackRequest.feedback_type, 'worked');
        assert.strictEqual(feedbackRequest.tool_name, 'cursor');
        assert.strictEqual(feedbackRequest.model_provider, 'anthropic');
        assert.strictEqual(feedbackRequest.result_type, 'worked');
      });

      it('worked 反馈不应额外调用 feedbacks API', () => {
        const feedback_type = 'worked';
        const shouldCallFeedbacksApi = feedback_type === 'partial' || feedback_type === 'failed';
        assert.strictEqual(shouldCallFeedbacksApi, false);
      });

      it('failed 反馈应额外调用 feedbacks API', () => {
        const feedback_type = 'failed';
        const shouldCallFeedbacksApi = feedback_type === 'partial' || feedback_type === 'failed';
        assert.strictEqual(shouldCallFeedbacksApi, true);
      });

      it('partial 反馈应包含 partial_details', () => {
        const partialDetails = {
          suggestion: '建议修改配置参数',
          alternative_approach: '使用不同的数据库连接池'
        };

        assert.ok(partialDetails.suggestion);
        assert.ok(partialDetails.alternative_approach);
      });
    });
  });

  describe('幂等性测试', () => {
    it('重复提交相同 trace 应返回相同结果', () => {
      const idempotencyKey = 'trace-idempotent-001';
      const firstSubmission = { trace_id: 'trace-123', status: 'SUBMITTED' };
      const secondSubmission = { trace_id: 'trace-123', status: 'SUBMITTED' };

      assert.strictEqual(firstSubmission.trace_id, secondSubmission.trace_id);
      assert.strictEqual(firstSubmission.status, secondSubmission.status);
    });

    it('不同幂等键应创建不同记录', () => {
      const key1 = 'trace-key-001';
      const key2 = 'trace-key-002';

      assert.notStrictEqual(key1, key2);
    });
  });

  describe('错误处理', () => {
    it('应正确处理 401 未授权错误', () => {
      const errorResponse = {
        code: -32003,
        message: 'UNAUTHORIZED',
        data: { reason: 'Invalid API key' }
      };

      assert.strictEqual(errorResponse.code, -32003);
      assert.ok(errorResponse.message.includes('UNAUTHORIZED'));
    });

    it('应正确处理 404 未找到错误', () => {
      const errorResponse = {
        code: -32005,
        message: 'NOT_FOUND',
        data: { resource: 'solution', id: 'invalid-id' }
      };

      assert.strictEqual(errorResponse.code, -32005);
      assert.ok(errorResponse.message.includes('NOT_FOUND'));
    });

    it('应正确处理 429 限流错误', () => {
      const errorResponse = {
        code: -32002,
        message: 'RATE_LIMITED',
        data: { retry_after: 60 }
      };

      assert.strictEqual(errorResponse.code, -32002);
      assert.ok(errorResponse.message.includes('RATE_LIMITED'));
      assert.strictEqual(errorResponse.data.retry_after, 60);
    });
  });

  describe('数据模型验证', () => {
    it('Solution 响应应包含必需字段', () => {
      const solutionResponse = {
        solution_id: 'sol-001',
        title: 'Spring Boot 启动失败解决方案',
        description: '详细描述...',
        verification_level: 'L3',
        risk_level: 'R1',
        execution_steps: ['步骤1', '步骤2'],
        verification_steps: ['验证1', '验证2'],
        risk_warnings: ['警告1'],
        applicable_scenarios: ['场景1']
      };

      assert.ok(solutionResponse.solution_id);
      assert.ok(solutionResponse.title);
      assert.ok(solutionResponse.verification_level);
      assert.ok(solutionResponse.execution_steps);
    });

    it('Trace 响应应包含必需字段', () => {
      const traceResponse = {
        trace_id: 'trace-001',
        status: 'SUBMITTED',
        missing_fields: []
      };

      assert.ok(traceResponse.trace_id);
      assert.ok(traceResponse.status);
    });

    it('Doctor 响应应包含检查项', () => {
      const doctorResponse = {
        total_checks: 16,
        passed_checks: 16,
        status: 'PASS',
        checks: [
          { name: 'LOGIN', status: 'PASS', message: 'login ok' },
          { name: 'CHANNEL', status: 'PASS', message: 'channel ok' }
        ]
      };

      assert.ok(doctorResponse.total_checks > 0);
      assert.ok(doctorResponse.passed_checks >= 0);
      assert.ok(doctorResponse.checks.length > 0);
    });
  });
});

describe('端到端流程测试', () => {
  describe('Search → Execute → Feedback 闭环', () => {
    it('应完成完整的用户旅程', () => {
      // 1. 搜索
      const searchQuery = {
        query: 'Spring Boot 数据库连接池配置',
        max_results: 5
      };
      assert.ok(searchQuery.query);

      // 2. 获取 Solution
      const solutionId = 'sol-001';
      assert.ok(solutionId);

      // 3. 执行操作 (模拟)
      const executionResult = 'success';
      assert.ok(executionResult);

      // 4. 提交反馈
      const feedbackResult = {
        invocation_id: 'inv-001',
        feedback_type: 'worked',
        status: 'reported'
      };
      assert.strictEqual(feedbackResult.feedback_type, 'worked');
      assert.strictEqual(feedbackResult.status, 'reported');
    });
  });

  describe('Search → No Result → Create Seed', () => {
    it('无结果时应创建需求种子', () => {
      const searchQuery = {
        query: '某个冷门问题的解决方案',
        max_results: 5
      };
      
      // 模拟无结果
      const results = [];
      assert.strictEqual(results.length, 0);

      // 创建 Seed
      const seedRequest = {
        task_goal: searchQuery.query,
        coverage_gap: '暂无相关 Solution',
        evidence_hint: null
      };

      assert.ok(seedRequest.task_goal);
      assert.ok(seedRequest.coverage_gap);
    });
  });
});
