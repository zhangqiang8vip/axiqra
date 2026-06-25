/**
 * Axiqra CLI - 本地草稿管理模块
 *
 * 实现 draft_trace、flush_trace、list_drafts、get_draft、abandon_draft 功能：
 * - 本地 JSON 文件存储
 * - 多任务草稿隔离
 * - 幂等性保证
 * - outcome 必须由用户确认
 */

import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'fs';
import { resolve, dirname } from 'path';

/**
 * 草稿状态
 */
export const DRAFT_STATUS = {
  ACTIVE: 'active',
  IDLE: 'idle',
  READY: 'ready',
  FLUSHED: 'flushed',
  ABANDONED: 'abandoned'
};

/**
 * 步骤类型
 */
export const STEP_TYPES = {
  FORWARD: 'forward',
  DECISION: 'decision',
  EVIDENCE: 'evidence',
  BLOCKER: 'blocker',
  CORRECTION: 'correction',
  SUMMARY: 'summary'
};

/**
 * 草稿步骤
 */
export class DraftStep {
  constructor({
    stepId,
    stepType,
    stepContent,
    notes = null,
    contentHash = null,
    idempotencyKey = null,
    createdAt = null
  }) {
    this.stepId = stepId;
    this.stepType = stepType;
    this.stepContent = stepContent;
    this.notes = notes;
    this.contentHash = contentHash || this.generateHash(stepContent);
    this.idempotencyKey = idempotencyKey || this.generateHash(stepContent + stepType + Date.now());
    this.createdAt = createdAt || new Date().toISOString();
  }

  generateHash(content) {
    let hash = 0;
    for (let i = 0; i < content.length; i++) {
      const char = content.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash;
    }
    return Math.abs(hash).toString(36);
  }
}

/**
 * 草稿
 */
export class Draft {
  constructor({
    draftId,
    taskGoal,
    status = DRAFT_STATUS.ACTIVE,
    steps = [],
    createdAt = null,
    updatedAt = null,
    lastStepHash = null,
    reminderState = {
      lastRemindedAt: null,
      reminderSuppressed: false
    }
  }) {
    this.draftId = draftId;
    this.taskGoal = taskGoal;
    this.status = status;
    this.steps = steps;
    this.createdAt = createdAt || new Date().toISOString();
    this.updatedAt = updatedAt || new Date().toISOString();
    this.lastStepHash = lastStepHash;
    this.reminderState = reminderState;
  }

  get stepCount() {
    return this.steps.length;
  }

  get lastStepSummary() {
    if (this.steps.length === 0) return null;
    const last = this.steps[this.steps.length - 1];
    return last.stepContent.substring(0, 60) + (last.stepContent.length > 60 ? '...' : '');
  }

  get needsReminder() {
    if (this.status === DRAFT_STATUS.FLUSHED || this.status === DRAFT_STATUS.ABANDONED) {
      return false;
    }
    const config = this.getConfig();
    const inactiveMs = Date.now() - new Date(this.updatedAt).getTime();
    const inactiveMinutes = inactiveMs / 60000;
    return inactiveMinutes > (config.draft_inactive_minutes || 30);
  }
}

/**
 * 草稿管理器
 */
export class DraftManager {
  constructor(options = {}) {
    const configDir = options.configDir || process.env.AXIQRA_CONFIG_DIR ||
      (process.platform === 'win32'
        ? resolve(process.env.USERPROFILE || 'C:\\Users\\' + process.env.USERNAME, '.axiqra')
        : resolve(process.env.HOME || '', '.axiqra'));

    this.draftsDir = resolve(configDir, 'drafts');
    this.indexFile = resolve(this.draftsDir, 'index.json');
  }

  /**
   * 获取配置
   */
  getConfig() {
    return {
      draft_inactive_minutes: 30,
      draft_max_steps: 50,
      reminder_cooldown_minutes: 30
    };
  }

  /**
   * 初始化草稿目录
   */
  init() {
    if (!existsSync(this.draftsDir)) {
      mkdirSync(this.draftsDir, { recursive: true });
    }
    if (!existsSync(this.indexFile)) {
      writeFileSync(this.indexFile, JSON.stringify({ version: 1, drafts: {} }, null, 2));
    }
  }

  /**
   * 加载索引
   */
  loadIndex() {
    this.init();
    try {
      return JSON.parse(readFileSync(this.indexFile, 'utf-8'));
    } catch {
      return { version: 1, drafts: {} };
    }
  }

  /**
   * 保存索引
   */
  saveIndex(index) {
    this.init();
    writeFileSync(this.indexFile, JSON.stringify(index, null, 2));
  }

  /**
   * 获取草稿文件路径
   */
  getDraftFile(draftId) {
    return resolve(this.draftsDir, `${draftId}.json`);
  }

  /**
   * 加载草稿
   */
  loadDraft(draftId) {
    const file = this.getDraftFile(draftId);
    if (!existsSync(file)) {
      return null;
    }
    try {
      const data = JSON.parse(readFileSync(file, 'utf-8'));
      return new Draft(data);
    } catch {
      return null;
    }
  }

  /**
   * 保存草稿
   */
  saveDraft(draft) {
    const file = this.getDraftFile(draft.draftId);
    writeFileSync(file, JSON.stringify(draft, null, 2));

    // 更新索引
    const index = this.loadIndex();
    index.drafts[draft.draftId] = {
      taskGoal: draft.taskGoal,
      status: draft.status,
      updatedAt: draft.updatedAt
    };
    this.saveIndex(index);
  }

  /**
   * 创建新草稿
   */
  createDraft(taskGoal) {
    const draftId = `draft_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
    const draft = new Draft({
      draftId,
      taskGoal,
      status: DRAFT_STATUS.ACTIVE
    });
    this.saveDraft(draft);
    return draft;
  }

  /**
   * 获取或创建草稿
   */
  getOrCreateDraft(draftId, taskGoal) {
    if (draftId) {
      const existing = this.loadDraft(draftId);
      if (existing) return existing;
    }
    return this.createDraft(taskGoal);
  }

  /**
   * 记录步骤
   */
  addStep(draftId, taskGoal, stepType, stepContent, notes = null, idempotencyKey = null) {
    const draft = this.getOrCreateDraft(draftId, taskGoal);

    if (draft.status === DRAFT_STATUS.FLUSHED) {
      throw new Error(`草稿 ${draftId} 已提交，不能再添加步骤`);
    }
    if (draft.status === DRAFT_STATUS.ABANDONED) {
      throw new Error(`草稿 ${draftId} 已放弃，不能再添加步骤`);
    }

    // 幂等性检查
    if (idempotencyKey) {
      const exists = draft.steps.some(s => s.idempotencyKey === idempotencyKey);
      if (exists) {
        console.log(chalk.gray(`  跳过重复步骤 (idempotency_key: ${idempotencyKey})`));
        return draft;
      }
    }

    // 内容 hash 去重
    const contentHash = new DraftStep({}).generateHash(stepContent);
    const hashExists = draft.steps.some(s => s.contentHash === contentHash && s.stepType === stepType);
    if (hashExists) {
      console.log(chalk.gray(`  跳过重复步骤 (内容相同)`));
      return draft;
    }

    const step = new DraftStep({
      stepId: `${draft.stepCount + 1}`.padStart(3, '0'),
      stepType,
      stepContent,
      notes,
      idempotencyKey
    });

    draft.steps.push(step);
    draft.updatedAt = new Date().toISOString();
    draft.lastStepHash = step.contentHash;

    // 超过最大步骤数，标记为 idle
    if (draft.stepCount >= this.getConfig().draft_max_steps) {
      draft.status = DRAFT_STATUS.IDLE;
    }

    this.saveDraft(draft);
    return draft;
  }

  /**
   * 列出所有草稿
   */
  listDrafts() {
    const index = this.loadIndex();
    const drafts = [];

    for (const [draftId, meta] of Object.entries(index.drafts)) {
      const draft = this.loadDraft(draftId);
      if (draft) {
        drafts.push({
          draftId: draft.draftId,
          taskGoal: draft.taskGoal,
          status: draft.status,
          stepCount: draft.stepCount,
          createdAt: draft.createdAt,
          updatedAt: draft.updatedAt,
          lastStepSummary: draft.lastStepSummary,
          needsReminder: draft.needsReminder
        });
      }
    }

    return drafts.sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt));
  }

  /**
   * 获取草稿详情
   */
  getDraft(draftId) {
    return this.loadDraft(draftId);
  }

  /**
   * 提交草稿
   */
  flushDraft(draftId, outcome, finalNotes = null, confirmedByUser = true, idempotencyKey = null) {
    if (!confirmedByUser) {
      throw new Error('必须由用户确认才能提交草稿');
    }

    const draft = this.loadDraft(draftId);
    if (!draft) {
      throw new Error(`草稿 ${draftId} 不存在`);
    }

    if (draft.status === DRAFT_STATUS.FLUSHED) {
      // 幂等性：返回成功但不重复提交
      console.log(chalk.gray(`  草稿 ${draftId} 已提交过，结果: ${draft.outcome || 'unknown'}`));
      return { alreadyFlushed: true, draft };
    }

    if (draft.status === DRAFT_STATUS.ABANDONED) {
      throw new Error(`草稿 ${draftId} 已放弃，不能提交`);
    }

    draft.status = DRAFT_STATUS.FLUSHED;
    draft.outcome = outcome;
    draft.finalNotes = finalNotes;
    draft.flushedAt = new Date().toISOString();
    draft.updatedAt = new Date().toISOString();

    this.saveDraft(draft);
    return { alreadyFlushed: false, draft };
  }

  /**
   * 放弃草稿
   */
  abandonDraft(draftId, reason = null, confirmedByUser = true) {
    if (!confirmedByUser) {
      throw new Error('必须由用户确认才能放弃草稿');
    }

    const draft = this.loadDraft(draftId);
    if (!draft) {
      throw new Error(`草稿 ${draftId} 不存在`);
    }

    if (draft.status === DRAFT_STATUS.FLUSHED) {
      throw new Error(`草稿 ${draftId} 已提交，不能放弃`);
    }

    draft.status = DRAFT_STATUS.ABANDONED;
    draft.abandonReason = reason;
    draft.updatedAt = new Date().toISOString();

    this.saveDraft(draft);
    return draft;
  }

  /**
   * 删除草稿
   */
  deleteDraft(draftId) {
    const file = this.getDraftFile(draftId);
    if (existsSync(file)) {
      const fs = require('fs');
      fs.unlinkSync(file);
    }

    const index = this.loadIndex();
    if (index.drafts[draftId]) {
      delete index.drafts[draftId];
      this.saveIndex(index);
    }
  }
}

/**
 * 创建草稿管理器实例
 */
export function createDraftManager(options = {}) {
  return new DraftManager(options);
}

export default { DraftManager, Draft, DraftStep, DRAFT_STATUS, STEP_TYPES, createDraftManager };
