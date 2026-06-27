/**
 * Axiqra Connect CLI - CLI 命令行接口
 *
 * 实现类似 git 的设备授权流程：
 * 1. axiqra login → 获取设备码和用户码
 * 2. 打开浏览器输入用户码授权
 * 3. CLI 自动轮询获取 token
 */

import { Command } from 'commander';
import chalk from 'chalk';
import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';
import { createQueueManager } from './queue.mjs';
import { createDraftManager } from './draft-manager.mjs';

const __dirname = dirname(fileURLToPath(import.meta.url));

const program = new Command();

/**
 * 配置文件路径
 */
const CONFIG_DIR = process.env.AXIQRA_CONFIG_DIR || (
  process.platform === 'win32'
    ? resolve(process.env.USERPROFILE || 'C:\\Users\\' + process.env.USERNAME, '.axiqra')
    : resolve(process.env.HOME || '', '.axiqra')
);
const CONFIG_FILE = resolve(CONFIG_DIR, 'config.json');

/**
 * 加载本地 token
 */
function loadToken() {
  try {
    if (existsSync(CONFIG_FILE)) {
      const config = JSON.parse(readFileSync(CONFIG_FILE, 'utf-8'));
      return config.token || null;
    }
  } catch {}
  return null;
}

/**
 * 加载用户信息
 */
function loadUser() {
  try {
    if (existsSync(CONFIG_FILE)) {
      const config = JSON.parse(readFileSync(CONFIG_FILE, 'utf-8'));
      return config.user || null;
    }
  } catch {}
  return null;
}

/**
 * 保存登录信息
 */
function saveLogin(token, user) {
  try {
    if (!existsSync(CONFIG_DIR)) {
      mkdirSync(CONFIG_DIR, { recursive: true });
    }
    const config = existsSync(CONFIG_FILE)
      ? JSON.parse(readFileSync(CONFIG_FILE, 'utf-8'))
      : {};
    config.token = token;
    config.user = user;
    // 确保 workspaceId 被保存
    if (user.workspaceId) {
      config.user.workspaceId = user.workspaceId;
    }
    writeFileSync(CONFIG_FILE, JSON.stringify(config, null, 2));
    return true;
  } catch (e) {
    console.warn('保存登录信息失败:', e.message);
    return false;
  }
}

/**
 * 清除登录信息
 */
function clearLogin() {
  try {
    if (existsSync(CONFIG_FILE)) {
      const config = JSON.parse(readFileSync(CONFIG_FILE, 'utf-8'));
      delete config.token;
      delete config.user;
      writeFileSync(CONFIG_FILE, JSON.stringify(config, null, 2));
    }
  } catch (e) {
    // ignore
  }
}

/**
 * API 请求封装
 */
const REQUEST_TIMEOUT_MS = 30000;

async function apiRequest(endpoint, options = {}) {
  const apiUrl = process.env.AXIQRA_API_URL || 'http://localhost:8080/api';
  const token = process.env.AXIQRA_TOKEN || loadToken() || '';

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  try {
    const response = await fetch(`${apiUrl}${endpoint}`, {
      ...options,
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token || '',
        ...options.headers
      }
    });

    clearTimeout(timeout);

    if (!response.ok) {
      try {
        const error = await response.json();
        const code = error.code || response.status;
        const message = error.message || error.error || `API 错误 (${response.status})`;
        throw new Error(formatError(code, message));
      } catch (e) {
        if (e.message.includes('API') || e.message.includes('错误')) {
          throw e;
        }
        throw new Error(formatError(response.status, `请求失败 (HTTP ${response.status})`));
      }
    }

    return response.json();
  } catch (e) {
    clearTimeout(timeout);
    if (e.name === 'AbortError') {
      throw new Error(formatError('TIMEOUT', `请求超时（${REQUEST_TIMEOUT_MS / 1000}秒）`));
    }
    throw e;
  }
}

/**
 * 统一错误格式
 * @param {string|number} code 错误码
 * @param {string} message 错误消息
 * @returns {string} 格式化后的错误信息
 */
function formatError(code, message) {
  const errorMap = {
    'TIMEOUT': { reason: '网络请求超时', tip: '请检查网络连接后重试' },
    401: { reason: '未登录或 Token 已过期', tip: '请运行 axiqra login 重新登录' },
    403: { reason: '无权访问该资源', tip: '请检查账号权限设置' },
    404: { reason: '请求的资源不存在', tip: '请确认资源 ID 是否正确' },
    500: { reason: '服务器内部错误', tip: '请稍后重试或联系技术支持' },
    502: { reason: '网关错误', tip: '请稍后重试' },
    503: { reason: '服务暂不可用', tip: '请稍后重试' }
  };

  const codeStr = String(code);
  const info = errorMap[code] || errorMap[codeStr] || { reason: '未知错误', tip: '请查看完整错误信息' };

  return `错误：${message}\n原因：${info.reason}\n提示：${info.tip}`;
}

/**
 * Loading 动画控制器
 */
class LoadingIndicator {
  constructor(message = '加载中') {
    this.message = message;
    this.frames = ['⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏'];
    this.interval = null;
    this.currentFrame = 0;
  }

  start() {
    process.stdout.write(chalk.cyan(`${this.frames[0]} ${this.message}... `));
    this.interval = setInterval(() => {
      process.stdout.write('\b\b\b\b');
      this.currentFrame = (this.currentFrame + 1) % this.frames.length;
      process.stdout.write(`${chalk.cyan(this.frames[this.currentFrame])} ${this.message}... `);
    }, 100);
  }

  stop(success = true) {
    if (this.interval) {
      clearInterval(this.interval);
      this.interval = null;
      process.stdout.write('\b\b\b\b\b\b');
      console.log(success ? chalk.green('完成') : chalk.red('失败'));
    }
  }
}

/**
 * 打开浏览器
 */
function openBrowser(url) {
  const os = process.platform;
  const start = (cmd) => {
    require('child_process').spawn(cmd, [url], { detached: true, stdio: 'ignore' }).unref();
  };

  if (os === 'darwin') {
    require('child_process').spawn('open', [url], { detached: true, stdio: 'ignore' }).unref();
  } else if (os === 'win32') {
    require('child_process').spawn('cmd', ['/c', 'start', url], { detached: true, stdio: 'ignore' }).unref();
  } else {
    start('xdg-open');
  }
}

// ============================================================
// 版本和帮助
// ============================================================
program
  .name('axiqra')
  .description('Axiqra CLI - AI 工程方案记忆层')
  .version('1.0.0');

// ============================================================
// 登录命令 - OAuth 设备授权流程
// ============================================================
program
  .command('login')
  .description('登录 Axiqra（设备授权/API Key）')
  .usage('[options]')
  .option('-n, --no-browser', '不自动打开浏览器')
  .option('-u, --username <username>', '使用用户名密码登录（不安全，不推荐）')
  .option('-p, --password <password>', '密码')
  .option('-k, --api-key <apiKey>', '使用 API Key 登录')
  .action(async (options) => {
    // API Key 登录
    if (options.apiKey) {
      await loginWithApiKey(options.apiKey);
      return;
    }

    // 如果指定了用户名密码，回退到传统登录（不推荐）
    if (options.username && options.password) {
      await loginWithPassword(options.username, options.password);
      return;
    }

    try {
      console.log(chalk.blue('\n🔐 Axiqra 设备授权'));
      console.log(chalk.gray('='.repeat(50)));

      // 步骤 1: 获取设备码
      console.log(chalk.cyan('\n步骤 1/3: 请求授权码...'));

      const codeResult = await apiRequest('/auth/device/code', {
        method: 'POST'
      });

      const { user_code, verification_url, device_code, expires_in, interval } = codeResult.data;

      console.log(chalk.green('\n✓ 授权码已生成！\n'));

      // 显示用户码
      console.log(chalk.yellow('┌─────────────────────────────────────────┐'));
      console.log(chalk.yellow('│           授权码                        │'));
      console.log(chalk.yellow('├─────────────────────────────────────────┤'));
      console.log(chalk.yellow(`│                                         │`));
      console.log(chalk.yellow(`│     🔑  ${chalk.white.bold(user_code)}      │`));
      console.log(chalk.yellow(`│                                         │`));
      console.log(chalk.yellow('└─────────────────────────────────────────┘'));

      console.log(chalk.gray(`\n  验证页面: ${verification_url}`));
      console.log(chalk.gray(`  有效期: ${Math.floor(expires_in / 60)} 分钟`));

      // 步骤 2: 打开浏览器
      if (!options.noBrowser) {
        console.log(chalk.cyan('\n步骤 2/3: 正在打开浏览器...'));
        try {
          openBrowser(verification_url);
          console.log(chalk.green('✓ 浏览器已打开'));
        } catch (e) {
          console.log(chalk.yellow('⚠ 无法自动打开浏览器，请手动访问上述链接'));
        }
      }

      // 步骤 3: 轮询获取 token
      console.log(chalk.cyan('\n步骤 3/3: 等待授权...\n'));
      console.log(chalk.gray('  请在浏览器中完成授权，然后按 Ctrl+C 取消等待\n'));

      let attempts = 0;
      const maxAttempts = Math.floor(expires_in / interval);

      while (attempts < maxAttempts) {
        attempts++;

        process.stdout.write(chalk.gray(`  正在验证 (${attempts}/${maxAttempts})... `));

        try {
          const tokenResult = await apiRequest(`/auth/device/token?deviceCode=${device_code}`, {
            method: 'POST'
          });

          // code === 0 表示成功（ApiResponse.ok()）
          if (tokenResult.code === 0 && tokenResult.data?.access_token) {
            // 授权成功
            const { access_token, user } = tokenResult.data;

            saveLogin(access_token, user);
            process.env.AXIQRA_TOKEN = access_token;

            console.log(chalk.green('\n\n✓ 授权成功！\n'));
            console.log(chalk.green('┌─────────────────────────────────────────┐'));
            console.log(chalk.green('│           登录信息                      │'));
            console.log(chalk.green('├─────────────────────────────────────────┤'));
            console.log(chalk.green(`│     用户: ${chalk.white(user.nickname || user.username)}     │`));
            console.log(chalk.green(`│     ID:   ${chalk.white(user.id)}   │`));
            console.log(chalk.green('└─────────────────────────────────────────┘'));
            console.log(chalk.gray(`\n  Token 已保存到: ${CONFIG_FILE}`));
            console.log(chalk.gray('  运行 axiqra doctor 验证连接\n'));

            return;
          }
        } catch (e) {
          // 忽略错误，继续轮询
        }

        console.log(chalk.gray('等待中...\n'));

        // 等待 interval 秒
        await new Promise(resolve => setTimeout(resolve, interval * 1000));
      }

      console.log(chalk.red('\n✗ 授权超时，请重新运行 axiqra login\n'));
      process.exit(1);

    } catch (error) {
      console.error(chalk.red('\n✗ 登录失败:'), error.message);
      console.log(chalk.gray('\n提示: 如果设备授权不可用，可以尝试:'));
      console.log(chalk.gray('  axiqra login -u <username> -p <password>\n'));
      process.exit(1);
    }
  });

/**
 * 传统用户名密码登录（不推荐，仅用于兼容）
 */
async function loginWithPassword(username, password) {
  try {
    console.log(chalk.yellow('\n⚠ 使用用户名密码登录（不推荐）\n'));

    const result = await apiRequest('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    });

    if (result.data?.token) {
      const token = result.data.token;
      saveLogin(token, {
        id: result.data.userId,
        username: result.data.username,
        nickname: result.data.nickname,
        workspaceId: result.data.workspaceId
      });
      process.env.AXIQRA_TOKEN = token;

      console.log(chalk.green('\n✓ 登录成功！'));
      console.log(chalk.gray(`  用户: ${result.data.nickname || result.data.username}`));
      console.log(chalk.gray(`  Token 已保存到: ${CONFIG_FILE}`));
      console.log(chalk.gray('\n  运行 axiqra doctor 验证连接\n'));
    } else {
      throw new Error('登录响应中未包含 token');
    }
  } catch (error) {
    console.error(chalk.red('\n✗ 登录失败:'), error.message);
    process.exit(1);
  }
}

/**
 * API Key 登录
 */
async function loginWithApiKey(apiKey) {
  try {
    console.log(chalk.blue('\n⚡ 使用 API Key 登录\n'));

    const result = await apiRequest('/auth/api-key/login', {
      method: 'POST',
      body: JSON.stringify({ apiKey })
    });

    if (result.data?.token) {
      const token = result.data.token;
      saveLogin(token, {
        id: result.data.userId,
        username: result.data.username,
        nickname: result.data.nickname,
        workspaceId: result.data.workspaceId
      });
      process.env.AXIQRA_TOKEN = token;

      console.log(chalk.green('\n✓ 登录成功！'));
      console.log(chalk.gray(`  用户: ${result.data.nickname || result.data.username}`));
      console.log(chalk.gray(`  Token 已保存到: ${CONFIG_FILE}`));
      console.log(chalk.gray('\n  运行 axiqra doctor 验证连接\n'));
    } else {
      throw new Error('API Key 登录响应中未包含 token');
    }
  } catch (error) {
    console.error(chalk.red('\n✗ API Key 登录失败:'), error.message);
    process.exit(1);
  }
}

// ============================================================
// 注册命令
// ============================================================
program
  .command('register')
  .description('注册新用户')
  .option('-u, --username <username>', '用户名')
  .option('-p, --password <password>', '密码')
  .option('-e, --email <email>', '邮箱')
  .option('-n, --nickname <nickname>', '昵称')
  .action(async (options) => {
    try {
      const username = options.username || process.env.AXIQRA_USERNAME;
      const password = options.password || process.env.AXIQRA_PASSWORD;
      const email = options.email || process.env.AXIQRA_EMAIL;
      const nickname = options.nickname;


      if (!username || !password) {
        console.log(chalk.yellow('请提供用户名和密码:'));
        console.log(chalk.gray('  axiqra register -u <username> -p <password>'));
        process.exit(1);
      }

      console.log(chalk.blue(`\n正在注册用户: ${username}...`));

      const result = await apiRequest('/auth/register', {
        method: 'POST',
        body: JSON.stringify({ username, password, email, nickname })
      });

      if (result.data?.token) {
        const token = result.data.token;
        saveLogin(token, {
          id: result.data.userId,
          username: result.data.username,
          nickname: result.data.nickname
        });
        process.env.AXIQRA_TOKEN = token;

        console.log(chalk.green('\n✓ 注册成功！'));
        console.log(chalk.gray(`  Token 已保存到: ${CONFIG_FILE}`));
      } else {
        throw new Error('注册响应中未包含 token');
      }
    } catch (error) {
      console.error(chalk.red('\n✗ 注册失败:'), error.message);
      process.exit(1);
    }
  });

// ============================================================
// Doctor 命令
// ============================================================
program
  .command('doctor')
  .description('执行接入诊断')
  .option('-c, --check <name>', '指定检查项', /^(network|auth|quota|version|config|storage|proxy|firewall)$/i)
  .option('-v, --verbose', '详细输出')
  .action(async (options) => {
    console.log(chalk.blue('\n Axiqra Doctor 诊断'));
    console.log(chalk.gray('=' . repeat(50) + '\n'));

    const checks = options.check ? [options.check] : [
      'network', 'auth', 'quota', 'version', 'config', 'storage'
    ];

    let passed = 0;
    for (const check of checks) {
      try {
        process.stdout.write(`  ${check.padEnd(15)} `);

        switch (check) {
          case 'network':
            try {
              const response = await fetch('http://localhost:8080/api/connect/quota');
              if (response.ok || response.status === 401) {
                console.log(chalk.green('✓ 通过'));
                passed++;
              } else {
                console.log(chalk.red('✗ 失败'));
              }
            } catch {
              console.log(chalk.red('✗ 失败'));
            }
            break;

          case 'auth':
            const token = process.env.AXIQRA_TOKEN || loadToken();
            if (token) {
              try {
                const me = await apiRequest('/auth/me');
                console.log(chalk.green('✓ 已登录'));
                passed++;
              } catch {
                console.log(chalk.yellow('⚠ Token 无效'));
              }
            } else {
              console.log(chalk.yellow('⚠ 未登录'));
            }
            break;

          case 'quota':
            try {
              const quota = await apiRequest('/connect/quota');
              console.log(chalk.green('✓ 配额正常'));
              passed++;
            } catch (e) {
              if (e.message.includes('401')) {
                console.log(chalk.yellow('⚠ 需要登录'));
              } else {
                console.log(chalk.red('✗ ' + e.message));
              }
            }
            break;

          default:
            console.log(chalk.green('✓ 通过'));
            passed++;
        }
      } catch (error) {
        console.log(chalk.yellow('? 跳过'));
      }
    }

    console.log(chalk.gray('\n' + '='.repeat(50)));
    console.log(chalk.cyan(`  通过: ${passed} / ${checks.length}`));
    console.log(chalk.gray('='.repeat(50) + '\n'));
  });

// ============================================================
// Search 命令
// ============================================================
program
  .command('search <query>')
  .description('搜索历史方案')
  .option('-t, --tag <tag>', '标签过滤')
  .option('-l, --level <level>', '验证等级', /^(L[0-5])$/i)
  .option('-m, --max <n>', '最大结果数', parseInt, 5)
  .action(async (query, options) => {
    try {
      const result = await apiRequest('/search/before-act', {
        method: 'POST',
        body: JSON.stringify({
          query,
          tag: options.tag,
          level: options.level,
          maxResults: options.max
        })
      });

      if (!result.data?.results || result.data.results.length === 0) {
        console.log(chalk.yellow('\n未找到匹配的方案\n'));
        console.log(chalk.gray('  建议:'));
        console.log(chalk.gray('  1. 尝试更通用的关键词'));
        console.log(chalk.gray('  2. 使用 axiqra seed create 提交新需求'));
        console.log(chalk.gray('  3. 自行解决后提交轨迹: axiqra trace submit <file>\n'));
        return;
      }

      console.log(chalk.blue(`\n找到 ${result.data.results.length} 个方案:\n`));

      result.data.results.forEach((item, index) => {
        const levelColor = {
          'L0': chalk.gray,
          'L1': chalk.gray,
          'L2': chalk.blue,
          'L3': chalk.green,
          'L4': chalk.cyan,
          'L5': chalk.magenta
        }[item.verificationLevel] || chalk.white;

        console.log(chalk.white(`  ${index + 1}. ${item.title}`));
        console.log(chalk.gray(`     ID: ${item.solutionId}`));
        console.log(levelColor(`     验证等级: ${item.verificationLevel || 'N/A'}`));
        console.log(chalk.gray(`     匹配度: ${((item.fitScore || 0) * 100).toFixed(1)}%`));
        if (item.riskLevel) {
          const riskColor = item.riskLevel >= 'R2' ? chalk.yellow : chalk.gray;
          console.log(riskColor(`     风险等级: ${item.riskLevel}`));
        }
        console.log();
      });
    } catch (error) {
      console.error(chalk.red('搜索失败:'), error.message);
    }
  });

// ============================================================
// Solution 命令
// ============================================================
const solutionCmd = program
  .command('solution')
  .description('Solution 管理');

solutionCmd
  .command('get <id>')
  .usage('<solution-id> [options]')
  .description('获取 Solution 详情')
  .option('-v, --view <mode>', '视图模式', /^(execution|full|metadata)$/i, 'execution')
  .action(async (id, options) => {
    try {
      const result = await apiRequest(`/solutions/${id}?view=${options.view}`);

      console.log(chalk.blue(`\n ${result.data?.title || id}\n`));
      console.log(chalk.gray('='.repeat(50)));
      console.log(chalk.gray(`  ID: ${id}`));
      console.log(chalk.gray(`  验证等级: ${result.data?.verificationLevel || 'N/A'}`));
      console.log(chalk.gray(`  风险等级: ${result.data?.riskLevel || 'N/A'}\n`));

      if (result.data?.executionSteps) {
        console.log(chalk.cyan('  执行步骤:'));
        result.data.executionSteps.forEach((step, i) => {
          console.log(chalk.gray(`    ${i + 1}. ${step}`));
        });
        console.log();
      }

      if (result.data?.riskWarnings) {
        console.log(chalk.yellow('  风险提示:'));
        result.data.riskWarnings.forEach(w => {
          console.log(chalk.yellow(`    ⚠ ${w}`));
        });
        console.log();
      }
    } catch (error) {
      console.error(chalk.red('获取失败:'), error.message);
    }
  });

// ============================================================
// Trace 命令 - 初始化队列管理器
// ============================================================
const queueManager = createQueueManager();

// ============================================================
// trace - 父命令
// ============================================================
const traceCmd = program
  .command('trace')
  .description('轨迹管理（submit/queue/retry/list/get）');

// ============================================================
// trace submit <file>
// ============================================================
traceCmd
  .command('submit <file>')
  .usage('<file> [options]')
  .description('提交轨迹（失败时自动暂存）')
  .option('-t, --tag <tags...>', '标签')
  .option('-n, --note <note>', '备注')
  .option('-w, --workspace <id>', '工作空间 ID（必需）')
  .option('--no-queue', '不启用本地暂存，失败直接报错')
  .action(async (file, options) => {
    // 检查文件是否存在
    const resolvedPath = resolve(file);
    if (!existsSync(resolvedPath)) {
      console.error(chalk.red(`\n✗ 文件不存在: ${file}`));
      console.log(chalk.gray('  请检查文件路径是否正确。\n'));
      return;
    }

    let traceData;
    try {
      traceData = JSON.parse(readFileSync(resolvedPath, 'utf-8'));
    } catch (e) {
      console.error(chalk.red(`\n✗ 无法读取轨迹文件: ${e.message}`));
      return;
    }

    try {
      const idempotencyKey = traceData.idempotency_key || `trace_${Date.now()}`;
      const workspaceId = options.workspace || traceData.workspace_id || traceData.workspaceId;

      if (!workspaceId) {
        console.error(chalk.red('\n✗ 缺少工作空间 ID'));
        console.log(chalk.gray('  请通过 --workspace 指定工作空间 ID，或在轨迹 JSON 中包含 workspace_id 字段。\n'));
        console.log(chalk.gray('  示例: axiqra trace submit trace.json --workspace 1\n'));
        return;
      }

      const result = await apiRequest('/traces', {
        method: 'POST',
        body: JSON.stringify({
          workspaceId: workspaceId,
          taskGoal: traceData.task_goal || traceData.taskGoal || '',
          outcome: traceData.outcome || '',
          riskLevel: traceData.risk_level || traceData.riskLevel || 'low',
          visibilityScope: traceData.visibility_scope || traceData.visibilityScope || 'workspace',
          projectId: traceData.project_id || traceData.projectId,
          toolType: traceData.tool_type || traceData.toolType,
          contextSnapshot: traceData.context_snapshot || traceData.contextSnapshot,
          forwardSteps: traceData.forward_steps || traceData.forwardSteps,
          reversePath: traceData.reverse_path || traceData.reversePath,
          decisions: traceData.decisions,
          rollbackPath: traceData.rollback_path || traceData.rollbackPath,
          solutionId: traceData.solution_id || traceData.solutionId,
          evolutionSuggestion: traceData.evolution_suggestion || traceData.evolutionSuggestion,
          idempotencyKey: idempotencyKey,
          evidences: (traceData.evidences || []).map(e => ({
            uri: e.uri || e.path || '',
            type: e.type || 'file',
            hash: e.hash || null,
            sizeBytes: e.size || e.sizeBytes || 0
          }))
        })
      });

      console.log(chalk.green('\n✓ 轨迹提交成功'));
      console.log(chalk.gray(`  Trace ID: ${result.data?.id || result.data?.traceId}`));
      console.log(chalk.gray(`  状态: ${result.data?.status || 'draft'}\n`));
    } catch (error) {
      if (options.queue !== false) {
        console.log(chalk.yellow('\n⚠ 提交失败，启用本地暂存...\n'));
        const idempotencyKey = traceData.idempotency_key || `trace_${Date.now()}`;
        const entry = queueManager.add(traceData, idempotencyKey, null, null);

        console.log(chalk.green('✓ 已添加到本地暂存队列'));
        console.log(chalk.gray(`  暂存 ID: ${entry.id}`));
        console.log(chalk.gray(`  重试次数: ${entry.retryCount} / ${queueManager.maxRetries}`));
        console.log(chalk.gray('\n  运行 axiqra trace queue 查看队列'));
        console.log(chalk.gray('  运行 axiqra trace retry 重试暂存的轨迹\n'));
      } else {
        console.error(chalk.red('提交失败:'), error.message);
      }
    }
  });

// ============================================================
// trace queue
// ============================================================
traceCmd
  .command('queue')
  .usage('[options]')
  .description('查看本地暂存队列')
  .option('-s, --status <status>', '状态过滤 (pending/retrying/failed/succeeded)')
  .option('-c, --clear', '清空已完成的条目')
  .action(async (options) => {
    if (options.clear) {
      const removed = queueManager.clearSucceeded();
      if (removed) {
        console.log(chalk.green('\n✓ 已清空已完成的条目\n'));
      } else {
        console.log(chalk.gray('\n  没有已完成的条目需要清空\n'));
      }
      return;
    }

    const stats = queueManager.getStats();
    console.log(chalk.blue('\n Axiqra 本地暂存队列'));
    console.log(chalk.gray('='.repeat(50)));
    console.log(chalk.gray(`  总数: ${stats.total}`));
    console.log(chalk.cyan(`  待处理: ${stats.pending}`));
    console.log(chalk.yellow(`  重试中: ${stats.retrying}`));
    console.log(chalk.red(`  失败: ${stats.failed}`));
    console.log(chalk.green(`  成功: ${stats.succeeded}`));
    console.log(chalk.gray('='.repeat(50)));

    let entries = queueManager.loadQueue();
    if (options.status) {
      entries = entries.filter(e => e.status === options.status);
    }

    if (entries.length === 0) {
      console.log(chalk.gray('\n  队列为空\n'));
      return;
    }

    console.log(chalk.blue('\n队列详情:\n'));
    entries.forEach((entry, i) => {
      const statusColor = {
        pending: chalk.cyan,
        retrying: chalk.yellow,
        failed: chalk.red,
        succeeded: chalk.green
      }[entry.status] || chalk.white;

      console.log(statusColor(`  ${i + 1}. ${entry.id}`));
      console.log(chalk.gray(`     状态: ${entry.status}`));
      console.log(chalk.gray(`     重试: ${entry.retryCount} / ${queueManager.maxRetries}`));
      if (entry.lastError) {
        console.log(chalk.gray(`     错误: ${entry.lastError.substring(0, 60)}...`));
      }
      if (entry.nextRetryAt) {
        const nextTime = new Date(entry.nextRetryAt);
        console.log(chalk.gray(`     下次重试: ${nextTime.toLocaleString()}`));
      }
      if (entry.tracePayload?.taskGoal) {
        console.log(chalk.gray(`     任务: ${entry.tracePayload.taskGoal.substring(0, 40)}...`));
      }
      console.log();
    });
  });

// ============================================================
// trace retry
// ============================================================
traceCmd
  .command('retry')
  .usage('[options]')
  .description('重试暂存的轨迹')
  .option('-i, --id <id>', '指定暂存 ID')
  .option('-a, --all', '重试所有待处理的')
  .option('-f, --force', '强制重试即使未到重试时间')
  .action(async (options) => {
    const stats = queueManager.getStats();
    if (stats.pending === 0 && stats.retrying === 0 && stats.failed === 0) {
      console.log(chalk.yellow('\n  队列为空，没有需要重试的条目\n'));
      return;
    }

    let entriesToRetry = [];

    if (options.id) {
      const queue = queueManager.loadQueue();
      const entry = queue.find(e => e.id === options.id);
      if (entry) {
        entriesToRetry = [entry];
      } else {
        console.error(chalk.red(`\n✗ 找不到暂存 ID: ${options.id}\n`));
        return;
      }
    } else if (options.all) {
      entriesToRetry = queueManager.getReadyForRetry();
      if (entriesToRetry.length === 0) {
        console.log(chalk.yellow('\n  没有到达重试时间的条目\n'));
        if (options.force) {
          entriesToRetry = queueManager.getPending();
        } else {
          console.log(chalk.gray('  使用 --force 强制重试\n'));
          return;
        }
      }
    } else {
      entriesToRetry = queueManager.getReadyForRetry();
      if (entriesToRetry.length === 0) {
        console.log(chalk.yellow('\n  没有到达重试时间的条目\n'));
        console.log(chalk.gray('  使用 --all 或 --force 重试所有待处理的\n'));
        return;
      }
    }

    console.log(chalk.blue(`\n 正在重试 ${entriesToRetry.length} 个条目...\n`));

    let successCount = 0;
    let failCount = 0;

    for (const entry of entriesToRetry) {
      try {
        const payload = entry.tracePayload || {};
        const result = await apiRequest('/traces', {
          method: 'POST',
          body: JSON.stringify({
            workspaceId: entry.targetWorkspaceId || payload.workspaceId || payload.workspace_id,
            taskGoal: payload.task_goal || payload.taskGoal || '',
            outcome: payload.outcome || '',
            riskLevel: payload.risk_level || payload.riskLevel || 'low',
            visibilityScope: payload.visibility_scope || payload.visibilityScope || 'workspace',
            projectId: payload.project_id || payload.projectId,
            toolType: payload.tool_type || payload.toolType,
            contextSnapshot: payload.context_snapshot || payload.contextSnapshot,
            forwardSteps: payload.forward_steps || payload.forwardSteps,
            reversePath: payload.reverse_path || payload.reversePath,
            decisions: payload.decisions,
            rollbackPath: payload.rollback_path || payload.rollbackPath,
            idempotencyKey: entry.idempotencyKey,
            evidences: (payload.evidences || []).map(e => ({
              uri: e.uri || e.path || '',
              type: e.type || 'file',
              hash: e.hash || null,
              sizeBytes: e.size || e.sizeBytes || 0
            }))
          })
        });

        queueManager.markSucceeded(entry.id);
        console.log(chalk.green(`  ✓ ${entry.id} 提交成功`));
        successCount++;
      } catch (error) {
        const updatedEntry = queueManager.markFailed(entry.id, error.message);
        console.log(chalk.red(`  ✗ ${entry.id} 失败: ${error.message}`));
        if (updatedEntry?.status === 'failed') {
          console.log(chalk.yellow(`    已达到最大重试次数，不再重试`));
        } else {
          console.log(chalk.gray(`    将在 ${new Date(updatedEntry?.nextRetryAt).toLocaleTimeString()} 重试`));
        }
        failCount++;
      }
    }

    console.log(chalk.gray('\n' + '='.repeat(50)));
    console.log(chalk.green(`  成功: ${successCount}`));
    console.log(chalk.red(`  失败: ${failCount}`));
    console.log(chalk.gray('='.repeat(50) + '\n'));
  });

// ============================================================
// trace list
// ============================================================
traceCmd
  .command('list')
  .usage('[options]')
  .description('查看轨迹列表')
  .option('-s, --status <status>', '状态过滤')
  .option('-p, --page <page>', '页码', '1')
  .option('-l, --limit <limit>', '每页数量', '20')
  .action(async (options) => {
    try {
      const params = new URLSearchParams({
        page: options.page || '1',
        size: options.limit || '20'
      });
      if (options.status) {
        params.append('status', options.status);
      }
      const result = await apiRequest('/traces?' + params.toString());

      console.log(chalk.blue('\n轨迹列表:\n'));
      const traces = result.data || [];
      if (traces.length === 0) {
        console.log(chalk.gray('  暂无轨迹'));
      } else {
        traces.forEach(t => {
          console.log(`  ${t.id || t.traceId}  ${t.status}  ${(t.taskGoal || '').substring(0, 50)}...`);
        });
      }
      console.log();
    } catch (error) {
      console.error(chalk.red('获取失败:'), error.message);
    }
  });

// ============================================================
// trace clean
// ============================================================
traceCmd
  .command('clean')
  .usage('[options]')
  .description('清空暂存队列')
  .option('-a, --all', '清空所有条目（包括失败的）')
  .action(async (options) => {
    if (options.all) {
      queueManager.clearAll();
      console.log(chalk.green('\n✓ 已清空所有暂存条目\n'));
    } else {
      const removed = queueManager.clearSucceeded();
      if (removed) {
        console.log(chalk.green('\n✓ 已清空已完成的条目\n'));
      } else {
        console.log(chalk.gray('\n  没有已完成的条目需要清空\n'));
      }
    }
  });

// ============================================================
// trace remove <id>
// ============================================================
traceCmd
  .command('remove <id>')
  .usage('<id>')
  .description('从暂存队列删除特定条目')
  .action(async (id) => {
    const removed = queueManager.remove(id);
    if (removed) {
      console.log(chalk.green(`\n✓ 已删除暂存条目: ${id}\n`));
    } else {
      console.error(chalk.red(`\n✗ 找不到暂存条目: ${id}\n`));
    }
  });

// ============================================================
// trace get <id>
// ============================================================
traceCmd
  .command('get <id>')
  .description('查看轨迹详情')
  .action(async (id) => {
    try {
      const result = await apiRequest(`/traces/${id}`);
      console.log(JSON.stringify(result, null, 2));
    } catch (error) {
      console.error(chalk.red('获取失败:'), error.message);
    }
  });

// ============================================================
// Feedback 命令
// ============================================================
program
  .command('feedback <invocation-id> <type>')
  .usage('<invocation-id> <type> [options]')
  .description('提交反馈')
  .option('-e, --evidence <files...>', '证据文件')
  .option('-r, --reason <reason>', '原因')
  .option('-n, --note <note>', '备注')
  .action(async (invocationId, type, options) => {
    const validTypes = ['worked', 'partial', 'failed', 'not_applicable'];
    if (!validTypes.includes(type)) {
      console.error(chalk.red(`无效的反馈类型: ${type}`));
      console.log(chalk.gray(`  有效类型: ${validTypes.join(', ')}`));
      process.exit(1);
    }

    try {
      // 修复：如果 invocationId 是数字字符串则发送 invocationId，否则发送 invocationCode
      const isNumeric = /^\d+$/.test(invocationId);
      const requestBody = isNumeric
        ? { invocationId: parseInt(invocationId), feedbackType: type, feedbackContent: options.reason, contextDelta: options.note, evidenceRefs: options.evidence }
        : { invocationCode: invocationId, feedbackType: type, feedbackContent: options.reason, contextDelta: options.note, evidenceRefs: options.evidence };

      const result = await apiRequest('/v1/feedbacks', {
        method: 'POST',
        body: JSON.stringify(requestBody)
      });

      console.log(chalk.green('\n✓ 反馈已提交'));
      console.log(chalk.gray(`  Feedback ID: ${result.data?.feedbackId || result.data?.id}`));
      if (result.data?.impact) {
        console.log(chalk.gray(`  影响: ${JSON.stringify(result.data.impact)}`));
      }
      console.log();
    } catch (error) {
      console.error(chalk.red('提交失败:'), error.message);
    }
  });

// ============================================================
// Quota 命令
// ============================================================
program
  .command('quota')
  .description('查看配额状态')
  .usage('[options]')
  .option('-d, --detail', '详细输出')
  .action(async (options) => {
    try {
      const result = await apiRequest('/connect/quota');

      const used = result.data?.used ?? 0;
      const limit = result.data?.limit ?? 2000;
      const remaining = result.data?.remaining ?? (limit - used);

      console.log(chalk.blue('\n 配额状态\n'));
      console.log(chalk.gray('='.repeat(40)));
      console.log(chalk.gray(`  今日使用: ${used} / ${limit}`));
      console.log(chalk.gray(`  剩余: ${remaining >= 0 ? remaining : 'N/A'}`));

      if (options.detail && result.data) {
        console.log(chalk.gray(`  窗口: daily`));
        console.log(chalk.gray(`  重置时间: ${result.data.resetAt || 'UTC 00:00'}`));
        if (result.data.exceeded !== undefined) {
          console.log(chalk.gray(`  已超限: ${result.data.exceeded ? '是' : '否'}`));
        }
      }
      console.log(chalk.gray('='.repeat(40) + '\n'));
    } catch (error) {
      console.error(chalk.red('获取失败:'), error.message);
    }
  });

// ============================================================
// Seed 命令
// ============================================================
const seedCmd = program
  .command('seed')
  .description('候选 Seed 管理');

seedCmd
  .command('create <query>')
  .usage('<query> [options]')
  .description('创建候选 Seed')
  .option('-t, --tech-stack <techStack>', '技术栈')
  .option('-d, --domain <domain>', '领域')
  .option('-p, --priority <priority>', '优先级', /^(low|medium|high)$/i, 'medium')
  .action(async (query, options) => {
    try {
      const result = await apiRequest('/seeds', {
        method: 'POST',
        body: JSON.stringify({
          query,
          techStack: options.techStack,
          domain: options.domain,
          priority: options.priority
        })
      });

      console.log(chalk.green('\n✓ 候选 Seed 已创建'));
      console.log(chalk.gray(`  Seed ID: ${result.data?.seedId || result.data?.id}`));
      console.log(chalk.gray(`  查询: ${query}`));
      if (result.data?.status) {
        console.log(chalk.gray(`  状态: ${result.data.status}`));
      }
      console.log();
    } catch (error) {
      console.error(chalk.red('创建失败:'), error.message);
    }
  });

seedCmd
  .command('list')
  .usage('[options]')
  .description('查看 Seed 列表')
  .option('-s, --status <status>', '状态过滤 (pending/processed/rejected)')
  .action(async (options) => {
    try {
      const url = options.status ? `/seeds?status=${options.status}` : '/seeds';
      const result = await apiRequest(url);

      console.log(chalk.blue('\n候选 Seed 列表:\n'));
      const seeds = result.data || [];
      if (seeds.length === 0) {
        console.log(chalk.gray('  暂无候选 Seed'));
      } else {
        seeds.forEach((seed, i) => {
          const statusColor = seed.status === 'pending' ? chalk.yellow :
                             seed.status === 'processed' ? chalk.green : chalk.gray;
          console.log(chalk.cyan(`  ${i + 1}. ${seed.seedId || seed.id}`));
          console.log(chalk.gray(`     查询: ${seed.query || seed.taskGoal}`));
          console.log(statusColor(`     状态: ${seed.status || 'N/A'}`));
          if (seed.techStack) {
            console.log(chalk.gray(`     技术栈: ${seed.techStack}`));
          }
          console.log();
        });
      }
    } catch (error) {
      console.error(chalk.red('获取失败:'), error.message);
    }
  });

seedCmd
  .command('get <id>')
  .usage('<seed-id> [options]')
  .description('查看 Seed 详情')
  .action(async (id) => {
    try {
      const result = await apiRequest(`/seeds/${id}`);
      const seed = result.data;

      if (!seed) {
        console.log(chalk.yellow('\n  找不到该 Seed\n'));
        return;
      }

      console.log(chalk.blue(`\n Seed 详情\n`));
      console.log(chalk.gray('='.repeat(50)));
      console.log(chalk.gray(`  ID: ${seed.seedId || seed.id}`));
      console.log(chalk.gray(`  查询: ${seed.query || seed.taskGoal}`));
      console.log(chalk.gray(`  状态: ${seed.status || 'N/A'}`));
      if (seed.techStack) console.log(chalk.gray(`  技术栈: ${seed.techStack}`));
      if (seed.domain) console.log(chalk.gray(`  领域: ${seed.domain}`));
      if (seed.priority) console.log(chalk.gray(`  优先级: ${seed.priority}`));
      if (seed.createdAt) console.log(chalk.gray(`  创建时间: ${seed.createdAt}`));
      console.log(chalk.gray('='.repeat(50) + '\n'));
    } catch (error) {
      console.error(chalk.red('获取失败:'), error.message);
    }
  });

// ============================================================
// Sessions 命令
// ============================================================
program
  .command('sessions')
  .description('会话管理')
  .addCommand(
    new Command('list')
      .usage('[options]')
      .description('查看会话列表')
      .action(async () => {
        try {
          const result = await apiRequest('/connect/sessions');

          console.log(chalk.blue('\n会话列表:\n'));
          const sessions = result.data || [];
          if (sessions.length === 0) {
            console.log(chalk.gray('  暂无会话'));
          } else {
            sessions.forEach(s => {
              console.log(`  ${s.sessionId || s.id}  ${s.status}  ${s.channel}`);
            });
          }
          console.log();
        } catch (error) {
          console.error(chalk.red('获取失败:'), error.message);
        }
      })
  )
  .addCommand(
    new Command('get <id>')
      .usage('<session-id> [options]')
      .description('查看会话详情')
      .action(async (id) => {
        try {
          const result = await apiRequest(`/connect/sessions/${id}`);
          console.log(JSON.stringify(result, null, 2));
        } catch (error) {
          console.error(chalk.red('获取失败:'), error.message);
        }
      })
  );

// ============================================================
// Config 命令
// ============================================================
program
  .command('config')
  .description('配置管理')
  .addCommand(
    new Command('list')
      .description('查看所有配置')
      .action(() => {
        const apiUrl = process.env.AXIQRA_API_URL || 'http://localhost:8080/api';
        const token = process.env.AXIQRA_TOKEN || loadToken();

        console.log(chalk.blue('\n Axiqra 配置\n'));
        console.log(chalk.gray('='.repeat(40)));
        console.log(chalk.gray(`  API URL: ${apiUrl}`));
        console.log(chalk.gray(`  Token: ${token ? '***' + token.slice(-4) : '(未设置)'}`));
        console.log(chalk.gray(`  配置文件: ${CONFIG_FILE}`));
        console.log(chalk.gray('='.repeat(40) + '\n'));
      })
  )
  .addCommand(
    new Command('set <key> <value>')
      .description('设置配置项')
      .action((key, value) => {
        const envKey = `AXIQRA_${key.toUpperCase().replace(/-/g, '_')}`;
        process.env[envKey] = value;
        console.log(chalk.green(`✓ ${key} = ${value}`));
        console.log(chalk.gray(`  (导出 ${envKey}=${value})`));
      })
  )
  .addCommand(
    new Command('get <key>')
      .description('获取配置项')
      .action((key) => {
        const envKey = `AXIQRA_${key.toUpperCase().replace(/-/g, '_')}`;
        const value = process.env[envKey];
        console.log(value || '(未设置)');
      })
  );

// ============================================================
// Whoami 命令
// ============================================================
program
  .command('whoami')
  .description('查看当前用户')
  .action(async () => {
    // 先尝试从本地缓存读取
    const cachedUser = loadUser();
    const token = loadToken();

    if (!token) {
      console.log(chalk.yellow('\n未登录，请运行 axiqra login\n'));
      return;
    }

    try {
      const result = await apiRequest('/auth/me');
      const user = result.data;

      // 更新本地缓存
      saveLogin(token, {
        id: user.userId || user.id,
        username: user.username,
        nickname: user.nickname || user.username
      });

      console.log(chalk.blue('\n当前用户\n'));
      console.log(chalk.gray('═'.repeat(40)));
      console.log(chalk.gray('  ID:     '), chalk.white(String(user.userId || user.id)));
      console.log(chalk.gray('  用户名:  '), chalk.white(user.nickname || user.username || 'N/A'));
      console.log(chalk.gray('  邮箱:   '), chalk.white(user.email || 'N/A'));
      console.log(chalk.gray('═'.repeat(40) + '\n'));
    } catch (error) {
      if (error.message.includes('401')) {
        console.log(chalk.yellow('\n登录已过期，请运行 axiqra login 重新登录\n'));
      } else {
        // 如果 API 失败，尝试显示缓存的用户
        if (cachedUser) {
          console.log(chalk.blue('\n当前用户（缓存）\n'));
          console.log(chalk.gray('═'.repeat(40)));
          console.log(chalk.gray('  ID:     '), chalk.white(String(cachedUser.id)));
          console.log(chalk.gray('  用户名:  '), chalk.white(cachedUser.nickname || cachedUser.username));
          console.log(chalk.yellow('  ⚠ API 请求失败，显示缓存数据\n'));
        } else {
          console.error(chalk.red('获取失败:'), error.message);
        }
      }
    }
  });

// ============================================================
// 登出命令
// ============================================================
program
  .command('logout')
  .description('退出登录')
  .action(async () => {
    try {
      // 调用服务端登出
      await apiRequest('/auth/logout', { method: 'POST' });
    } catch (e) {
      // 忽略服务端错误
    }

    // 清除本地登录信息
    clearLogin();
    delete process.env.AXIQRA_TOKEN;

    console.log(chalk.green('\n✓ 已退出登录\n'));
  });

// ============================================================
// Draft 命令
// ============================================================
const draftManager = createDraftManager();

const draftCmd = program
  .command('draft')
  .description('草稿管理（draft/flush/list/get/abandon）');

// ============================================================
// draft trace <goal>
// ============================================================
draftCmd
  .command('trace <goal>')
  .usage('<task-goal> [options]')
  .description('记录本地草稿步骤')
  .option('-d, --draft-id <draftId>', '指定草稿 ID（为空则创建新草稿）')
  .option('-t, --type <type>', '步骤类型', /^(forward|decision|evidence|blocker|correction|summary)$/i, 'forward')
  .option('-c, --content <content>', '步骤内容')
  .option('-n, --notes <notes>', '备注')
  .option('-k, --idempotency-key <key>', '幂等键')
  .action(async (goal, options) => {
    try {
      const content = options.content || 'recorded';
      const draft = draftManager.addStep(
        options.draftId,
        goal,
        options.type,
        content,
        options.notes,
        options.idempotencyKey
      );

      console.log(chalk.green(`\n✓ 已记录步骤 (草稿: ${draft.draftId})`));
      console.log(chalk.gray(`  任务: ${goal}`));
      console.log(chalk.gray(`  类型: ${options.type}`));
      console.log(chalk.gray(`  步骤数: ${draft.stepCount}\n`));
    } catch (error) {
      console.error(chalk.red('记录失败:'), error.message);
    }
  });

// ============================================================
// draft flush <draft-id>
// ============================================================
draftCmd
  .command('flush <draftId>')
  .usage('<draft-id> <outcome> [options]')
  .description('提交草稿为正式轨迹')
  .option('-n, --notes <notes>', '最终说明')
  .action(async (draftId, outcome, options) => {
    const validOutcomes = ['success', 'failure', 'partial'];
    if (!validOutcomes.includes(outcome)) {
      console.error(chalk.red(`无效的 outcome: ${outcome}`));
      console.log(chalk.gray(`  有效值: ${validOutcomes.join(', ')}\n`));
      process.exit(1);
    }

    try {
      const result = draftManager.flushDraft(
        draftId,
        outcome,
        options.notes,
        true
      );

      if (result.alreadyFlushed) {
        console.log(chalk.yellow(`\n草稿 ${draftId} 已提交过，结果: ${outcome}\n`));
      } else {
        console.log(chalk.green(`\n✓ 草稿已提交 (outcome: ${outcome})`));
        console.log(chalk.gray(`  草稿 ID: ${draftId}`));
        console.log(chalk.gray(`  提交时间: ${result.draft.flushedAt}\n`));
      }
    } catch (error) {
      console.error(chalk.red('提交失败:'), error.message);
    }
  });

// ============================================================
// draft list
// ============================================================
draftCmd
  .command('list')
  .usage('[options]')
  .description('列出所有草稿')
  .option('-s, --status <status>', '状态过滤 (active/idle/ready/flushed/abandoned)')
  .action(async (options) => {
    try {
      const drafts = draftManager.listDrafts();

      console.log(chalk.blue('\n本地草稿列表\n'));
      console.log(chalk.gray('='.repeat(50)));

      let filtered = drafts;
      if (options.status) {
        filtered = drafts.filter(d => d.status === options.status);
      }

      if (filtered.length === 0) {
        console.log(chalk.gray('  暂无草稿'));
      } else {
        filtered.forEach(d => {
          const statusColor = {
            active: chalk.cyan,
            idle: chalk.yellow,
            ready: chalk.blue,
            flushed: chalk.green,
            abandoned: chalk.gray
          }[d.status] || chalk.white;

          console.log(chalk.cyan(`  ${d.draftId}`));
          console.log(chalk.gray(`    任务: ${d.taskGoal}`));
          statusColor(`    状态: ${d.status}`);
          console.log(chalk.gray(`    步骤数: ${d.stepCount}`));
          console.log(chalk.gray(`    更新: ${new Date(d.updatedAt).toLocaleString()}`));
          console.log();
        });
      }

      console.log(chalk.gray('='.repeat(50) + '\n'));
    } catch (error) {
      console.error(chalk.red('获取失败:'), error.message);
    }
  });

// ============================================================
// draft get <draft-id>
// ============================================================
draftCmd
  .command('get <draftId>')
  .usage('<draft-id>')
  .description('查看草稿详情')
  .action(async (draftId) => {
    try {
      const draft = draftManager.getDraft(draftId);

      if (!draft) {
        console.error(chalk.red(`找不到草稿: ${draftId}\n`));
        return;
      }

      console.log(chalk.blue(`\n草稿详情: ${draftId}\n`));
      console.log(chalk.gray('='.repeat(50)));
      console.log(chalk.gray(`  任务: ${draft.taskGoal}`));
      console.log(chalk.gray(`  状态: ${draft.status}`));
      console.log(chalk.gray(`  步骤数: ${draft.stepCount}`));
      console.log(chalk.gray(`  创建: ${new Date(draft.createdAt).toLocaleString()}`));
      console.log(chalk.gray(`  更新: ${new Date(draft.updatedAt).toLocaleString()}`));
      console.log(chalk.gray('='.repeat(50)));

      if (draft.steps.length > 0) {
        console.log(chalk.gray('\n步骤记录:\n'));
        draft.steps.forEach((step, i) => {
          console.log(chalk.cyan(`  ${i + 1}. [${step.stepType}] ${step.stepContent.substring(0, 80)}...`));
        });
      }

      console.log();
    } catch (error) {
      console.error(chalk.red('获取失败:'), error.message);
    }
  });

// ============================================================
// draft abandon <draft-id>
// ============================================================
draftCmd
  .command('abandon <draftId>')
  .usage('<draft-id> [options]')
  .description('放弃草稿')
  .option('-r, --reason <reason>', '放弃原因')
  .action(async (draftId, options) => {
    try {
      draftManager.abandonDraft(draftId, options.reason, true);
      console.log(chalk.green(`\n✓ 已放弃草稿: ${draftId}\n`));
    } catch (error) {
      console.error(chalk.red('放弃失败:'), error.message);
    }
  });

// ============================================================
// draft delete <draft-id>
// ============================================================
draftCmd
  .command('delete <draftId>')
  .usage('<draft-id>')
  .description('删除草稿')
  .action(async (draftId) => {
    try {
      draftManager.deleteDraft(draftId);
      console.log(chalk.green(`\n✓ 已删除草稿: ${draftId}\n`));
    } catch (error) {
      console.error(chalk.red('删除失败:'), error.message);
    }
  });

// ============================================================
// Workspace 命令
// ============================================================
const workspaceCmd = program
  .command('workspace')
  .description('工作空间管理（list/members/add-member）');

// ============================================================
// workspace list
// ============================================================
workspaceCmd
  .command('list')
  .description('查看我的工作空间列表')
  .action(async () => {
    try {
      const result = await apiRequest('/workspaces');

      console.log(chalk.blue('\n我的工作空间\n'));
      console.log(chalk.gray('='.repeat(60)));

      const workspaces = result.data?.content || result.data?.records || [];
      if (workspaces.length === 0) {
        console.log(chalk.gray('  暂无工作空间'));
        console.log(chalk.gray('\n  运行 axiqra workspace create <name> 创建工作空间\n'));
      } else {
        workspaces.forEach((ws, i) => {
          console.log(chalk.cyan(`  ${i + 1}. ${ws.name || ws.workspaceName || '未命名'}`));
          console.log(chalk.gray(`     ID: ${ws.id || ws.workspaceId}`));
          console.log(chalk.gray(`     类型: ${ws.type || ws.workspaceType || 'N/A'}`));
          console.log(chalk.gray(`     角色: ${ws.myRole || 'N/A'}`));
          console.log();
        });
      }

      console.log(chalk.gray('='.repeat(60) + '\n'));
    } catch (error) {
      console.error(chalk.red('获取失败:'), error.message);
    }
  });

// ============================================================
// workspace create
// ============================================================
workspaceCmd
  .command('create <name>')
  .description('创建工作空间')
  .option('-t, --type <type>', '类型', /^(personal|team|enterprise)$/i, 'personal')
  .action(async (name, options) => {
    try {
      const result = await apiRequest('/workspaces', {
        method: 'POST',
        body: JSON.stringify({
          workspaceName: name,
          workspaceType: options.type
        })
      });

      console.log(chalk.green('\n✓ 工作空间创建成功'));
      console.log(chalk.gray(`  ID: ${result.data?.id || result.data?.workspaceId}`));
      console.log(chalk.gray(`  名称: ${name}`));
      console.log(chalk.gray(`  类型: ${options.type}\n`));
    } catch (error) {
      console.error(chalk.red('创建失败:'), error.message);
    }
  });

// ============================================================
// workspace members <workspace-id>
// ============================================================
workspaceCmd
  .command('members <workspaceId>')
  .description('查看工作空间成员')
  .action(async (workspaceId) => {
    try {
      const result = await apiRequest(`/workspaces/${workspaceId}/members`);

      console.log(chalk.blue(`\n工作空间 ${workspaceId} 成员\n`));
      console.log(chalk.gray('='.repeat(60)));

      const members = result.data?.content || result.data?.records || [];
      if (members.length === 0) {
        console.log(chalk.gray('  暂无成员\n'));
      } else {
        members.forEach((m, i) => {
          const roleColor = {
            'owner': chalk.magenta,
            'admin': chalk.red,
            'member': chalk.green,
            'viewer': chalk.gray
          }[m.role] || chalk.white;

          console.log(chalk.cyan(`  ${i + 1}. ${m.nickname || m.username || m.userId}`));
          console.log(chalk.gray(`     用户ID: ${m.userId || m.memberId}`));
          roleColor(`     角色: ${m.role}`);
          console.log(chalk.gray(`     状态: ${m.status || 'active'}`));
          console.log();
        });
      }

      console.log(chalk.gray('='.repeat(60) + '\n'));
    } catch (error) {
      console.error(chalk.red('获取失败:'), error.message);
    }
  });

// ============================================================
// workspace add-member <workspace-id> <user-id>
// ============================================================
workspaceCmd
  .command('add-member <workspaceId> <userId>')
  .description('添加工作空间成员')
  .option('-r, --role <role>', '角色', /^(member|admin)$/i, 'member')
  .action(async (workspaceId, userId, options) => {
    try {
      const result = await apiRequest(`/workspaces/${workspaceId}/members?userId=${userId}&role=${options.role}`, {
        method: 'POST'
      });

      console.log(chalk.green('\n✓ 成员添加成功'));
      console.log(chalk.gray(`  工作空间: ${workspaceId}`));
      console.log(chalk.gray(`  用户ID: ${userId}`));
      console.log(chalk.gray(`  角色: ${options.role}\n`));
    } catch (error) {
      console.error(chalk.red('添加失败:'), error.message);
      if (error.message.includes('403')) {
        console.log(chalk.yellow('  提示: 只有 admin 或 owner 才能添加成员\n'));
      }
    }
  });

// ============================================================
// 解析命令
// ============================================================
program.parse();
