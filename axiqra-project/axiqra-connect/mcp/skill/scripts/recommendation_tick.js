#!/usr/bin/env node
/**
 * Axiqra 推荐脚本
 *
 * 基于用户技能和数字资产生成推荐
 *
 * 用法：
 *   node recommendation_tick.js
 *   node recommendation_tick.js --limit 10
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// 配置
const SKILL_DIR = process.env.AXIQRA_SKILL_DIR || path.resolve(__dirname, '..');
const API_URL = process.env.AXIQRA_API_URL || 'https://api.axiqra.com/api';

// 获取凭证
function getAuthToken() {
  const authPath = path.join(SKILL_DIR, 'memory', 'axiqra-auth.json');
  if (fs.existsSync(authPath)) {
    const auth = JSON.parse(fs.readFileSync(authPath, 'utf-8'));
    return auth.access_token;
  }
  return null;
}

// API 请求
async function apiRequest(method, apiPath, body = null) {
  const token = getAuthToken();
  let url = API_URL.replace(/\/$/, '') + '/' + apiPath.replace(/^\//, '');

  const options = {
    method: method,
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    }
  };

  if (token) {
    options.headers['Authorization'] = `Bearer ${token}`;
  }

  if (body) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(url, options);
  return response.json();
}

async function main() {
  const args = process.argv.slice(2);
  const limit = args.includes('--limit') ? parseInt(args[args.indexOf('--limit') + 1]) || 10 : 10;

  const result = {
    status: 'ok',
    timestamp: new Date().toISOString(),
    recommendations: []
  };

  try {
    // 搜索热门方案
    const searchResult = await apiRequest('POST', '/search/public', {
      query: '',
      max_results: limit
    });

    if (searchResult.code === 200 && searchResult.data?.results) {
      result.recommendations = searchResult.data.results.map(r => ({
        type: 'solution',
        id: r.solution_id,
        title: r.title,
        fit_score: r.fit_score,
        reason: `验证等级 ${r.verification_level}，匹配度 ${Math.round(r.fit_score * 100)}%`
      }));
    }
  } catch (e) {
    result.status = 'error';
    result.message = e.message;
  }

  console.log(JSON.stringify(result, null, 2));
}

main().catch(e => {
  console.error(JSON.stringify({ status: 'error', message: e.message }));
  process.exit(1);
});
