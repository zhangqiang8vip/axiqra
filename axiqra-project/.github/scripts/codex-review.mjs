/**
 * Codex Review Script
 * 使用 OpenAI API (Codex 模型) 进行代码审查
 * 需要环境变量: OPENAI_API_KEY
 */

import { writeFileSync } from 'fs';

const OPENAI_API_KEY = process.env.OPENAI_API_KEY;

if (!OPENAI_API_KEY) {
  console.log('::warning::OPENAI_API_KEY not set. Skipping Codex review.');
  process.exit(0);
}

async function getPRDiff(prNumber) {
  const { execSync } = await import('child_process');
  try {
    const diff = execSync(`gh pr diff ${prNumber}`, { encoding: 'utf8' });
    return diff;
  } catch (e) {
    console.log('Could not get PR diff:', e.message);
    return '';
  }
}

async function getPRInfo(prNumber) {
  const { execSync } = await import('child_process');
  try {
    const pr = execSync(`gh api repos/{owner}/{repo}/pulls/${prNumber} --jq '{title: .title, body: .body, base: .base.ref, head: .head.ref, author: .user.login}'`, { encoding: 'utf8' });
    return JSON.parse(pr);
  } catch (e) {
    console.log('Could not get PR info:', e.message);
    return { title: 'Unknown', author: 'unknown', base: 'main' };
  }
}

async function callCodex(prompt) {
  const response = await fetch('https://api.openai.com/v1/chat/completions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${OPENAI_API_KEY}`
    },
    body: JSON.stringify({
      model: 'gpt-4o',
      messages: [
        {
          role: 'system',
          content: `You are an expert code reviewer for the Axiqra project.
Axiqra is a knowledge engineering and memory system with the following key components:
- MCP (Model Context Protocol) adapters and services
- Search service with public/private isolation
- Audit and tracing system
- Policy-based access control
- JSON store for persistence

Review guidelines:
1. SECURITY FIRST: Check for hardcoded secrets, API key leaks, injection risks
2. MCP compliance: quota limits, error handling, protocol adherence
3. Error handling: async try/catch, proper HTTP status codes
4. Test coverage: new code has corresponding tests
5. Code quality: ESM imports, naming conventions, error context

Output format (use Markdown):
## Summary
[Brief overview of the changes]

## Critical Issues (Block Merge)
- [file:line] - Issue description

## Suggestions (Recommended)
- [file:line] - Suggestion

## Positive Highlights
- What was done well

Be concise and actionable.`
        },
        {
          role: 'user',
          content: prompt
        }
      ],
      temperature: 0.3,
      max_tokens: 2000
    })
  });

  if (!response.ok) {
    const error = await response.text();
    console.log(`::warning::OpenAI API error: ${response.status} - ${error}`);
    return null;
  }

  const data = await response.json();
  return data.choices?.[0]?.message?.content || null;
}

async function main() {
  const prNumber = process.env.PR_NUMBER || process.argv[2] || '1';

  console.log(`Reviewing PR #${prNumber} with Codex...`);

  const [prInfo, diff] = await Promise.all([
    getPRInfo(prNumber),
    getPRDiff(prNumber)
  ]);

  if (!diff) {
    console.log('No diff found. Exiting.');
    return;
  }

  // 限制 diff 大小以控制 token 消耗
  const truncatedDiff = diff.length > 15000
    ? diff.substring(0, 15000) + '\n... (truncated, showing first 15000 chars)'
    : diff;

  const prompt = `Please review the following PR:

PR Title: ${prInfo.title}
Author: ${prInfo.author}
Base Branch: ${prInfo.base}

Code Changes:
${truncatedDiff}

Provide your review focusing on:
1. Security vulnerabilities
2. MCP protocol compliance
3. Error handling quality
4. Test coverage
5. Code maintainability`;

  const review = await callCodex(prompt);

  if (review) {
    const outputPath = process.env.GITHUB_OUTPUT;
    if (outputPath) {
      writeFileSync(outputPath, `review_body<<EOF\n${review}\nEOF\n`, { flag: 'a' });
    }
    console.log('\n=== Codex Review ===\n');
    console.log(review);
  } else {
    console.log('::warning::Could not generate review');
  }
}

main().catch(console.error);
