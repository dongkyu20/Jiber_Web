# Project Agent Guide

This repository uses project-local Codex agent definitions for a real estate transaction information web platform.

Start here:

- Shared operating model: `.agents/README.md`
- Architecture / Design Agent: `.agents/architecture-design-agent.md`
- Backend API Agent: `.agents/backend-api-agent.md`
- Auth / Security Agent: `.agents/auth-security-agent.md`
- Frontend / Map Agent: `.agents/frontend-map-agent.md`
- AI / Data Integration Agent: `.agents/ai-data-integration-agent.md`
- QA / Review Agent: `.agents/qa-review-agent.md`

## Default Coordination Rules

- The main Codex session coordinates scope, integration, and final user-facing decisions.
- Spawn live sub-agents only for bounded tasks with clear ownership paths.
- Do not spawn all role agents for every request.
- Every agent must respect user changes and must not revert unrelated work.
- Every implementation task must end with changed paths, verification notes, contract changes, blockers, and handoff notes.
- Build the user-facing web application in Korean.

## Planned Monorepo Layout

Ownership paths assume this layout:

```text
backend/
frontend/
model-server/
db/
docs/
.agents/
README.md
.gitignore
.env.example
AGENTS.md
```

If the scaffolded project uses different names, update `.agents/README.md` and every role file in the same change.

Root operational files such as `README.md`, `.gitignore`, and root `.env.example` are coordinated by the Architecture / Design Agent because they affect multiple services. Implementation agents may propose changes to them in handoff notes when their service requirements change.
