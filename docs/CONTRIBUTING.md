# Contributing to Axiqra

Thank you for helping Axiqra become useful to maintainers, contributors, and AI coding tools.

## Current Contribution Areas

Axiqra is currently best suited for contributions in these areas:

- clearer terminology for Cases, Solutions, Invocations, Feedback, and Engineering Trace Packages
- schema and object-model review
- MCP/API/CLI integration design
- OSS maintainer workflow examples
- security, redaction, and authorization review
- documentation fixes and translations
- public sample engineering Cases and Solutions

## How to Contribute

1. Open an issue describing the improvement or question.
2. Keep pull requests focused on one topic.
3. Explain the maintainer or AI-agent workflow your change supports.
4. Avoid including private code, logs, secrets, customer names, internal domains, or private repository paths.
5. For documentation changes, link the relevant document.

## Public Case Rules

A Public Case must not contain:

- real secrets, tokens, API keys, passwords, or private keys
- customer data or private business data
- internal domains, private IPs, or infrastructure names
- private repository URLs
- copyrighted content shared without permission
- vulnerability details that should be responsibly disclosed first

Use placeholders:

```text
<TOKEN_REDACTED>
<CUSTOMER_REDACTED>
<PRIVATE_REPO_REDACTED>
<INTERNAL_HOST_REDACTED>
```

## Pull Request Expectations

Good pull requests should include:

- a short summary
- the problem or workflow being improved
- links to affected documentation
- security or privacy considerations
- examples when the change affects a schema, protocol, or workflow

## PR Documentation Sync Checklist

When your PR changes code, you **must** update the doc-code mapping table (`docs/.docs/99-DOC-CODE-MAPPING.md`):

- [ ] **API changes**: If you added/modified a Controller or endpoint, update Section 1 (API Endpoints)
- [ ] **Service changes**: If you added/modified a Service, update Section 2 (Function Modules)
- [ ] **Mapper changes**: If you added a new Mapper, add it to the corresponding module
- [ ] **PRD mapping**: If the change affects product behavior, update the corresponding PRD document
- [ ] **New modules**: If you added a new functional module, add complete entries in Section 2

### Status Indicators

| Status | Meaning |
|--------|---------|
| ✅ | Implemented and complete |
| 🆕 | Newly added, pending completion |
| ❌ | Deprecated |
| ⚠️ | Partially implemented |

### Example PR Description

```markdown
## Changes

- Added new endpoint `POST /api/v1/examples`
- Updated ExampleService with new method

## Documentation Sync

- [x] Updated 99-DOC-CODE-MAPPING.md with new API entry (E01)
- [x] Verified PRD mapping (D09)
```

---

## Doc-Code Mapping Reference

The complete API-to-code mapping is maintained in:
- **File**: `docs/.docs/99-DOC-CODE-MAPPING.md`
- **Contents**: All 19 Controllers, 27 Services, 18 Mappers, 59 API endpoints

Always keep this table in sync with your code changes.

---

[English](CONTRIBUTING.md) | [中文](../i18n/CONTRIBUTING_zh.md)
