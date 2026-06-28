#!/usr/bin/env node
/**
 * Axiqra MCP server wrapper that auto-respawns on crash.
 * Solves Cursor's lack of stdio MCP auto-recovery.
 *
 * Forwards stdin/stdout/stderr to child server.mjs.
 * If child exits, respawns after 1s.
 */
import { spawn } from 'child_process';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import { existsSync } from 'fs';

const __dirname = dirname(fileURLToPath(import.meta.url));
const serverPath = join(__dirname, 'server.mjs');

if (!existsSync(serverPath)) {
  console.error('[WRAPPER] server.mjs not found at', serverPath);
  process.exit(1);
}

let child = null;
let stopped = false;
let restarting = false;

function start() {
  console.error('[WRAPPER] spawning server.mjs pid=' + process.pid);
  child = spawn(process.execPath, [serverPath], {
    stdio: ['inherit', 'inherit', 'inherit'],
    env: process.env
  });

  child.on('exit', (code, signal) => {
    console.error('[WRAPPER] server.mjs exited code=' + code + ' signal=' + signal);
    if (!stopped && !restarting) {
      restarting = true;
      setTimeout(() => {
        restarting = false;
        start();
      }, 1000);
    }
  });

  child.on('error', (err) => {
    console.error('[WRAPPER] spawn error:', err.message);
  });
}

process.on('SIGTERM', () => {
  stopped = true;
  if (child) child.kill('SIGTERM');
  process.exit(0);
});
process.on('SIGINT', () => {
  stopped = true;
  if (child) child.kill('SIGINT');
  process.exit(0);
});

start();