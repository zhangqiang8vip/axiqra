# Axiqra

Agent-ready engineering memory for AI coding tools and open-source maintainers.

Website: https://www.axiqra.com/

## Overview

Axiqra is an early-stage infrastructure project for turning real engineering work into reusable, reviewable, and agent-callable memory.

It is designed to help AI coding tools such as Codex, Cursor, Claude Code, Gemini CLI, and enterprise-built agents search previous engineering decisions before acting, and write back structured traces after tasks are completed.

The goal is simple:

```
Do not make AI coding agents reason from zero when a real engineering path has already been verified.
```

## Why This Matters

Open-source maintainers often solve the same classes of problems repeatedly:

- recurring issues and bug patterns
- pull request review decisions
- migration, release, rollback, and compatibility problems
- project-specific implementation conventions
- failed approaches that should not be repeated
- debugging paths that live only in comments, chats, local notes, or maintainer memory

Axiqra aims to preserve that knowledge as structured Cases, Solutions, evidence, boundaries, rollback notes, and worked/failed feedback.

## Open-Source Scope

Axiqra is not presented as a mature, widely used OSS library today. The project is currently in an early product and protocol design phase.

This repository is being opened to publish the ecosystem-facing parts first:

- Case, Public Case, Solution, Invocation, and Feedback concepts
- Engineering Trace Package format
- MCP/API/CLI integration design
- Codex-oriented maintainer workflows
- governance, review, redaction, and trusted-source rules
- public documentation for contributors and reviewers

See [Open-Source Scope](OPEN_SOURCE_SCOPE.md) for the exact scope.

## Repository Structure

```
axiqra-project/
  .github/          GitHub workflows and automation scripts
  axiqra-website/   public landing page and waitlist API
  axiqra-infra/     local dev middleware (CockroachDB, PostgreSQL, Redis, MinIO)
logo/               Axiqra logo assets
docs/               English documentation
i18n/               Chinese documentation
```

## Current Status

Axiqra is in an early public design phase.

Current repository focus areas:

- product and protocol specifications
- object model and lifecycle design
- maintainer and community governance
- AI tool integration flows
- public website and waitlist

Next steps are expected to include open protocol components, MCP integration, CLI workflows, and reference examples.

## How Codex Fits

Axiqra is designed to work with Codex-style engineering workflows:

1. Search previous Solutions and Public Cases before making a change.
2. Use evidence, boundaries, risk notes, and rollback paths to decide whether a Solution applies.
3. Execute the engineering task in the target repository.
4. Write back an Engineering Trace Package with what worked, what failed, and what should be reused.
5. Let maintainers review, improve, and publish reusable engineering memory.

This can support OSS workflows such as issue triage, PR review, release notes, migrations, debugging, onboarding, and repository automation.

## Quick Start

### Website (local development)

```bash
cd axiqra-project/axiqra-website
docker compose up -d
# http://127.0.0.1:8080
```

### Infrastructure (local middleware)

```bash
cd axiqra-project/axiqra-infra
cp .env.example .env   # fill in secrets
docker compose up -d
```

### Make targets

```bash
make website-up       # start website
make website-down     # stop website
make infra-up         # start infra
make infra-down       # stop infra
make infra-health     # health check
```

## Contributing

Contributions are welcome in these areas:

- clearer terminology for Cases, Solutions, Invocations, Feedback, and Engineering Trace Packages
- schema and object-model review
- MCP/API/CLI integration design
- OSS maintainer workflow examples
- security, redaction, and authorization review
- documentation fixes and translations
- public sample engineering Cases and Solutions

Please read [Contributing](CONTRIBUTING.md) first.

## Security

Engineering traces may accidentally contain code, logs, secrets, private paths, customer names, or internal infrastructure information.

Please read [Security Policy](SECURITY.md) before reporting sensitive issues.

## License

Apache License 2.0 - see [LICENSE](../LICENSE)

---

[English](README.md) | [中文](../i18n/README_zh.md)