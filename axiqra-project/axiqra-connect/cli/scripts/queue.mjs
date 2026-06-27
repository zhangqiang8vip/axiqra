/**
 * Axiqra CLI - 本地暂存队列模块
 *
 * 实现 D09 文档第 14 节规定的本地暂存失败提交功能：
 * - 网络失败时本地写入 pending queue
 * - 指数退避重试
 * - 重试计数跟踪
 * - 用户确认快照
 */

import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'fs';
import { resolve, dirname } from 'path';

/**
 * 获取平台特定的配置目录
 */
function getConfigDir() {
  if (process.env.AXIQRA_CONFIG_DIR) {
    return process.env.AXIQRA_CONFIG_DIR;
  }
  if (process.platform === 'win32') {
    return resolve(process.env.USERPROFILE || 'C:\\Users\\' + process.env.USERNAME, '.axiqra');
  }
  return resolve(process.env.HOME || '', '.axiqra');
}

/**
 * 暂存条目状态
 */
export const PENDING_STATUS = {
  PENDING: 'pending',
  RETRYING: 'retrying',
  FAILED: 'failed',
  SUCCEEDED: 'succeeded'
};

/**
 * 暂存条目接口
 */
export class PendingEntry {
  constructor({
    id,
    tracePayload,
    idempotencyKey,
    targetWorkspaceId,
    createdAt,
    updatedAt,
    retryCount = 0,
    lastError = null,
    userConfirmationSnapshot = null,
    status = PENDING_STATUS.PENDING,
    nextRetryAt = null
  }) {
    this.id = id;
    this.tracePayload = tracePayload;
    this.idempotencyKey = idempotencyKey;
    this.targetWorkspaceId = targetWorkspaceId;
    this.createdAt = createdAt || new Date().toISOString();
    this.updatedAt = updatedAt || new Date().toISOString();
    this.retryCount = retryCount;
    this.lastError = lastError;
    this.userConfirmationSnapshot = userConfirmationSnapshot;
    this.status = status;
    this.nextRetryAt = nextRetryAt;
  }
}

/**
 * 队列管理器
 */
export class QueueManager {
  constructor(options = {}) {
    this.queueDir = options.queueDir || resolve(getConfigDir(), 'queue');
    this.maxRetries = options.maxRetries || 5;
    this.baseDelayMs = options.baseDelayMs || 1000;
    this.maxDelayMs = options.maxDelayMs || 300000; // 5 分钟
  }

  /**
   * 初始化队列目录
   */
  init() {
    if (!existsSync(this.queueDir)) {
      mkdirSync(this.queueDir, { recursive: true });
    }
  }

  /**
   * 获取队列文件路径
   */
  getQueueFile() {
    return resolve(this.queueDir, 'pending-traces.json');
  }

  /**
   * 加载队列数据
   */
  loadQueue() {
    const file = this.getQueueFile();
    if (!existsSync(file)) {
      return [];
    }
    try {
      const data = JSON.parse(readFileSync(file, 'utf-8'));
      return (data.entries || []).map(e => new PendingEntry(e));
    } catch (e) {
      console.warn('加载队列失败:', e.message);
      return [];
    }
  }

  /**
   * 保存队列数据
   */
  saveQueue(entries) {
    this.init();
    const file = this.getQueueFile();
    writeFileSync(file, JSON.stringify({
      version: 1,
      updatedAt: new Date().toISOString(),
      entries: entries.map(e => ({ ...e }))
    }, null, 2));
  }

  /**
   * 添加暂存条目
   */
  add(tracePayload, idempotencyKey, targetWorkspaceId, userConfirmationSnapshot = null) {
    const entry = new PendingEntry({
      id: this.generateId(),
      tracePayload,
      idempotencyKey: idempotencyKey || this.generateIdempotencyKey(tracePayload),
      targetWorkspaceId,
      userConfirmationSnapshot,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      retryCount: 0,
      status: PENDING_STATUS.PENDING,
      nextRetryAt: new Date().toISOString()
    });

    const queue = this.loadQueue();
    queue.push(entry);
    this.saveQueue(queue);
    return entry;
  }

  /**
   * 获取待处理的条目
   */
  getPending() {
    const queue = this.loadQueue();
    const now = new Date().toISOString();
    return queue.filter(e =>
      e.status === PENDING_STATUS.PENDING ||
      e.status === PENDING_STATUS.RETRYING
    );
  }

  /**
   * 获取可立即重试的条目
   */
  getReadyForRetry() {
    const queue = this.loadQueue();
    const now = new Date().toISOString();
    return queue.filter(e =>
      (e.status === PENDING_STATUS.PENDING || e.status === PENDING_STATUS.RETRYING) &&
      (!e.nextRetryAt || new Date(e.nextRetryAt) <= new Date())
    );
  }

  /**
   * 标记条目失败
   */
  markFailed(id, error) {
    const queue = this.loadQueue();
    const entry = queue.find(e => e.id === id);
    if (!entry) return null;

    entry.retryCount++;
    entry.lastError = error;
    entry.updatedAt = new Date().toISOString();

    if (entry.retryCount >= this.maxRetries) {
      entry.status = PENDING_STATUS.FAILED;
      entry.nextRetryAt = null;
    } else {
      entry.status = PENDING_STATUS.RETRYING;
      entry.nextRetryAt = this.calculateNextRetry(entry.retryCount);
    }

    this.saveQueue(queue);
    return entry;
  }

  /**
   * 标记条目成功
   */
  markSucceeded(id) {
    const queue = this.loadQueue();
    const entry = queue.find(e => e.id === id);
    if (!entry) return null;

    entry.status = PENDING_STATUS.SUCCEEDED;
    entry.updatedAt = new Date().toISOString();
    entry.nextRetryAt = null;

    this.saveQueue(queue);
    return entry;
  }

  /**
   * 删除条目
   */
  remove(id) {
    const queue = this.loadQueue();
    const filtered = queue.filter(e => e.id !== id);
    if (filtered.length < queue.length) {
      this.saveQueue(filtered);
      return true;
    }
    return false;
  }

  /**
   * 清空已完成的条目
   */
  clearSucceeded() {
    const queue = this.loadQueue();
    const filtered = queue.filter(e => e.status !== PENDING_STATUS.SUCCEEDED);
    if (filtered.length < queue.length) {
      this.saveQueue(filtered);
      return true;
    }
    return false;
  }

  /**
   * 清空所有条目
   */
  clearAll() {
    this.saveQueue([]);
    return true;
  }

  /**
   * 清空失败的条目
   */
  clearFailed() {
    const queue = this.loadQueue();
    const filtered = queue.filter(e => e.status !== PENDING_STATUS.FAILED);
    if (filtered.length < queue.length) {
      this.saveQueue(filtered);
      return true;
    }
    return false;
  }

  /**
   * 计算下一次重试时间（指数退避）
   */
  calculateNextRetry(retryCount) {
    const delay = Math.min(
      this.baseDelayMs * Math.pow(2, retryCount),
      this.maxDelayMs
    );
    const jitter = Math.random() * 0.1 * delay; // 10% jitter
    const nextTime = new Date(Date.now() + delay + jitter);
    return nextTime.toISOString();
  }

  /**
   * 生成唯一 ID
   */
  generateId() {
    return `pend_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
  }

  /**
   * 从 trace payload 生成幂等键
   */
  generateIdempotencyKey(tracePayload) {
    const content = JSON.stringify(tracePayload);
    // 简单的 hash
    let hash = 0;
    for (let i = 0; i < content.length; i++) {
      const char = content.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash;
    }
    return `idempotency_${Math.abs(hash).toString(36)}_${Date.now()}`;
  }

  /**
   * 获取队列统计
   */
  getStats() {
    const queue = this.loadQueue();
    return {
      total: queue.length,
      pending: queue.filter(e => e.status === PENDING_STATUS.PENDING).length,
      retrying: queue.filter(e => e.status === PENDING_STATUS.RETRYING).length,
      failed: queue.filter(e => e.status === PENDING_STATUS.FAILED).length,
      succeeded: queue.filter(e => e.status === PENDING_STATUS.SUCCEEDED).length
    };
  }
}

/**
 * 创建队列管理器实例
 */
export function createQueueManager(options = {}) {
  return new QueueManager(options);
}

export default { QueueManager, PendingEntry, PENDING_STATUS, createQueueManager };
