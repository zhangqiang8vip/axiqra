export const MCP_TOOL_DEFINITIONS = [
  { name: 'axiqra.search_before_act', command: 'search-before-act', next: 'search' },
  { name: 'axiqra.get_solution', command: 'get-solution', next: 'solution-detail' },
  { name: 'axiqra.get_public_case', command: 'get-public-case', next: 'public-case-detail' },
  { name: 'axiqra.submit_trace', command: 'submit-trace', next: 'trace-submit' },
  { name: 'axiqra.submit_feedback', command: 'submit-feedback', next: 'feedback-submit' },
  { name: 'axiqra.create_candidate_seed', command: 'create-candidate-seed', next: 'candidate-seed' },
  { name: 'axiqra.doctor', command: 'doctor', next: 'doctor' },
];

export function getToolDefinition(name) {
  return MCP_TOOL_DEFINITIONS.find((item) => item.name === name) ?? null;
}

export function getCommandDefinition(command) {
  return MCP_TOOL_DEFINITIONS.find((item) => item.command === command) ?? null;
}
