import { getToolDefinition, getCommandDefinition, MCP_TOOL_DEFINITIONS } from './protocol.mjs';

function createHandlers() {
  return {
    'axiqra.search_before_act': ({ query, workspaceId } = {}) => ({ accepted: true, query: query ?? '', workspaceId: workspaceId ?? null }),
    'axiqra.get_solution': ({ solutionId } = {}) => ({ accepted: true, solutionId: solutionId ?? null }),
    'axiqra.get_public_case': ({ caseId } = {}) => ({ accepted: true, caseId: caseId ?? null }),
    'axiqra.submit_trace': ({ sessionId, trace } = {}) => ({ accepted: true, sessionId: sessionId ?? null, trace: trace ?? null }),
    'axiqra.submit_feedback': ({ solutionId, feedback } = {}) => ({ accepted: true, solutionId: solutionId ?? null, feedback: feedback ?? null }),
    'axiqra.create_candidate_seed': ({ query, source } = {}) => ({ accepted: true, query: query ?? '', source: source ?? null }),
    'axiqra.doctor': ({ channel, toolType } = {}) => ({ accepted: true, channel: channel ?? 'cli', toolType: toolType ?? 'mcp' }),
  };
}

export function createMcpToolkit() {
  const handlers = createHandlers();
  const tools = MCP_TOOL_DEFINITIONS.map((item) => item.name);

  return {
    version: 'r4-structured-runnable',
    tools,
    hasTool(name) {
      return tools.includes(name);
    },
    describe(name) {
      return getToolDefinition(name);
    },
    invoke(name, payload = {}) {
      const definition = getToolDefinition(name);
      const handler = handlers[name];
      if (!definition || !handler) {
        return { tool: name, accepted: false, reason: 'unsupported_tool' };
      }
      return { tool: definition.name, next: definition.next, ...handler(payload) };
    },
  };
}

export function createCliCommands() {
  return MCP_TOOL_DEFINITIONS.map((item) => item.command);
}

export function runCliCommand(command, payload = {}) {
  const definition = getCommandDefinition(command);
  if (!definition) {
    return { command, accepted: false, reason: 'unsupported_command' };
  }
  const toolkit = createMcpToolkit();
  return { command: definition.command, ...toolkit.invoke(definition.name, payload) };
}
