import test from 'node:test';
import assert from 'node:assert/strict';
import { createMcpToolkit, createCliCommands, runCliCommand } from '../../src/s1-core/index.mjs';
import { getToolDefinition, getCommandDefinition, MCP_TOOL_DEFINITIONS } from '../../src/s1-core/protocol.mjs';

test('R4 MCP toolkit exposes seven tools', () => {
  const toolkit = createMcpToolkit();
  assert.equal(toolkit.tools.length, 7);
  assert.equal(toolkit.hasTool('axiqra.doctor'), true);
  assert.equal(toolkit.hasTool('axiqra.unknown'), false);
});

test('R4 MCP protocol definitions stay aligned', () => {
  assert.equal(MCP_TOOL_DEFINITIONS.length, 7);
  assert.equal(getToolDefinition('axiqra.search_before_act')?.command, 'search-before-act');
  assert.equal(getCommandDefinition('doctor')?.name, 'axiqra.doctor');
});

test('R4 MCP toolkit can invoke handlers', () => {
  const toolkit = createMcpToolkit();
  const result = toolkit.invoke('axiqra.search_before_act', { query: 'redis quota' });
  assert.equal(result.accepted, true);
  assert.equal(result.next, 'search');
  assert.equal(result.query, 'redis quota');
});

test('R4 CLI exposes protocol commands', () => {
  const commands = createCliCommands();
  assert.equal(commands.length, 7);
  assert.ok(commands.includes('doctor'));
  assert.ok(commands.includes('create-candidate-seed'));
});

test('R4 CLI command maps to MCP tool', () => {
  const result = runCliCommand('doctor', { channel: 'cli', toolType: 'mcp' });
  assert.equal(result.accepted, true);
  assert.equal(result.tool, 'axiqra.doctor');
});
