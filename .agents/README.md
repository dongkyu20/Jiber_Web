# Sub-Agent Operating Model

This directory defines the durable role prompts for this project's Codex sub-agents.

The project is a real estate transaction information web platform with:

- Vue 3 SPA for landing, map search, detail, favorites, community, notices, admin, and login callback routes.
- Spring Boot API server for business logic, authentication, authorization, MySQL access, OpenAPI docs, and model-server integration.
- FastAPI model server for apartment-only hedonic price prediction and SHAP explanation.
- MySQL persistence for properties, transactions, users, favorites, predictions, SHAP values, boards, comments, and notices.

## Roles

- Architecture / Design Agent: `.agents/architecture-design-agent.md`
- Backend API Agent: `.agents/backend-api-agent.md`
- Auth / Security Agent: `.agents/auth-security-agent.md`
- Frontend / Map Agent: `.agents/frontend-map-agent.md`
- AI / Data Integration Agent: `.agents/ai-data-integration-agent.md`
- QA / Review Agent: `.agents/qa-review-agent.md`

## Hybrid Operation

Use these files as the source of truth for both:

- Persistent project-local guidance.
- Live Codex sub-agents spawned for bounded tasks.

Live sub-agents should be task-scoped. Start only the roles that directly advance the current work.

## Ownership Path Model

Each role uses three path groups:

- Primary ownership paths: paths the agent can normally edit directly.
- Shared / contract paths: paths the agent may read and propose changes for, but should coordinate before editing when another role is affected.
- Restricted paths: paths the agent should not edit directly unless the main coordinator explicitly expands scope.

Ownership paths are collision-prevention rules, not absolute permission boundaries. When a task needs an exception, the handoff notes must explain why.

## Planned Layout

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

If the actual scaffold uses different directory names, update this file and all role ownership sections in the same change.

## Root Operational Files

Root operational files are shared project surfaces:

- `README.md`
- `.gitignore`
- `.env.example`
- Future root orchestration files such as `docker-compose.yml`

The Architecture / Design Agent coordinates these files by default because changes can affect backend, frontend, model-server, database, security, and QA workflows. Other agents may propose or make scoped changes when the task explicitly includes them, and should record the reason in handoff notes.

## Live Sub-Agent Prompt Template

```text
You are acting as the [ROLE NAME] for this project.

Read and follow:
- .agents/README.md
- .agents/[role-file].md

Task:
[specific bounded task]

Primary ownership paths for this task:
- [paths]

Shared paths you may inspect:
- [paths]

Do not revert edits made by the user or other agents. If you need to change a shared or restricted path, stop and report the proposed handoff.

Final response must include:
- Changed paths
- Verification performed
- Contract changes
- Blockers, if any
- Handoff notes for other agents
```

## Standard Handoff Format

```text
Changed paths:
- path

Verification:
- command or manual check

Contract changes:
- API/schema/route/model changes, or "None"

Blockers:
- blocker, or "None"

Next agent notes:
- note, or "None"
```

## Shared Rules

- Respect existing user changes and never revert unrelated work.
- Keep edits inside assigned ownership paths unless a handoff explicitly expands scope.
- Prefer small, reviewable outputs with clear verification notes.
- Record API, schema, route, and model-contract changes in handoff notes.
- Raise blockers early when a task needs credentials, external datasets, API keys, or product decisions.
- Treat investment guidance as out of scope. The platform provides data and explanations, not purchase recommendations.
