# Sub-Agent Setup Design

## Context

This project will become a real estate transaction information web platform with:

- Vue 3 SPA for landing, map search, detail pages, favorites, community, notices, and admin screens.
- Spring Boot API server for authentication, authorization, business logic, MySQL access, OpenAPI documentation, and integration with the model server.
- FastAPI model server for apartment-only hedonic price prediction and SHAP explanation.
- MySQL storage for properties, transactions, users, favorites, predictions, SHAP values, boards, comments, and notices.

The workspace is currently empty except for the newly created planning directories. The first durable artifact should therefore be a project-local sub-agent operating model, not production source code.

## Decision

Use a hybrid sub-agent setup:

1. Store durable role definitions under `.agents/`.
2. Use the role definitions as the source of truth when spawning live Codex sub-agents.
3. Keep live agents task-scoped rather than permanently running all six roles.

This keeps the project easy to coordinate while it is still being scaffolded, and it avoids creating always-on agents with no codebase context yet.

## Agent Directory Layout

```text
.agents/
  README.md
  architecture-design-agent.md
  backend-api-agent.md
  auth-security-agent.md
  frontend-map-agent.md
  ai-data-integration-agent.md
  qa-review-agent.md
```

## Shared Agent Rules

Every agent must follow these shared rules:

- Respect existing user changes and never revert unrelated work.
- Keep changes inside the agent's assigned ownership area unless a handoff explicitly expands scope.
- Treat ownership paths as collision-prevention rules, not as absolute permission boundaries.
- Prefer small, reviewable outputs with clear changed paths and verification notes.
- Record API, schema, route, or model-contract changes in the handoff notes.
- Raise blockers early when a task needs credentials, external datasets, API keys, or product decisions.
- Treat investment guidance as out of scope; the platform provides data and explanations, not purchase recommendations.

## Ownership Path Model

Each role definition must include three path groups:

- Primary ownership paths: paths the agent can normally edit directly.
- Shared / contract paths: paths the agent may read and propose changes for, but should coordinate before editing when another role is affected.
- Restricted paths: paths the agent should not edit directly unless the main coordinator explicitly expands scope.

Ownership paths are based on the planned monorepo layout below:

```text
backend/
frontend/
model-server/
db/
docs/
.agents/
AGENTS.md
```

If the scaffolded project uses different directory names, the ownership table must be updated in the same change that introduces the final layout.

## Agent Definitions

### Architecture / Design Agent

Owns the system shape and cross-service contracts.

Responsibilities:

- Maintain high-level architecture for Vue, Spring Boot, MySQL, and FastAPI.
- Define module boundaries, API versioning rules, DTO boundaries, and integration flows.
- Keep MVP scope aligned with the stated implementation priority.
- Review whether features belong in Spring Boot, FastAPI, frontend state, or database design.

Primary outputs:

- Architecture notes.
- Cross-service contract decisions.
- Sequence diagrams or data flow summaries when needed.
- Handoff instructions for implementation agents.

Ownership paths:

- Primary: `docs/architecture/**`, `docs/contracts/**`, `docs/superpowers/specs/**`, `.agents/**`, `AGENTS.md`
- Shared: `backend/**`, `frontend/**`, `model-server/**`, `db/**`, `docs/api/**`
- Restricted: direct feature implementation files unless a task explicitly asks for architecture-driven scaffolding

### Backend API Agent

Owns Spring Boot API implementation excluding security-specific policy.

Responsibilities:

- Implement property map search, filter search, detail APIs, transaction APIs, area averages, similar complexes, infra APIs, favorites, boards, comments, notices, and admin APIs.
- Use MyBatis for data access.
- Use Bean Validation for request validation.
- Keep Swagger / Springdoc OpenAPI documentation current.
- Coordinate with Auth / Security Agent for protected endpoints and roles.

Primary outputs:

- Controller, service, mapper, DTO, and SQL changes.
- API examples and verification commands.
- Notes about DB indexes needed for map bounds and search queries.

Ownership paths:

- Primary: `backend/src/main/java/**/property/**`, `backend/src/main/java/**/apartment/**`, `backend/src/main/java/**/valuation/**`, `backend/src/main/java/**/shap/**`, `backend/src/main/java/**/favorite/**`, `backend/src/main/java/**/board/**`, `backend/src/main/java/**/notice/**`, `backend/src/main/java/**/admin/**`, `backend/src/main/resources/mapper/**`, `backend/src/test/**`
- Shared: `backend/src/main/java/**/security/**`, `backend/src/main/java/**/auth/**`, `backend/src/main/resources/application*.yml`, `docs/api/**`, `docs/contracts/**`, `db/**`
- Restricted: `frontend/**`, `model-server/**`

### Auth / Security Agent

Owns authentication, authorization, token handling, and security posture.

Responsibilities:

- Implement OAuth2 login for Google, Kakao, and Naver.
- Issue and validate JWTs.
- Define role-based access for regular users and admins.
- Protect favorites, community write actions, notice mutations, and admin pages.
- Review CORS, token storage assumptions, validation failures, and security-sensitive logs.

Primary outputs:

- Security configuration.
- OAuth2 provider mapping.
- JWT filter and token service changes.
- Role/access matrix.

Ownership paths:

- Primary: `backend/src/main/java/**/auth/**`, `backend/src/main/java/**/security/**`, `backend/src/main/java/**/user/**`, `backend/src/test/**/auth/**`, `backend/src/test/**/security/**`
- Shared: `backend/src/main/resources/application*.yml`, `frontend/src/router/**`, `frontend/src/stores/**`, `frontend/src/api/**`, `docs/api/**`, `docs/contracts/**`, `docs/security/**`, `db/**`
- Restricted: `model-server/**`, frontend feature screens outside login/callback/token handling unless explicitly assigned

### Frontend / Map Agent

Owns Vue UI, routing, state, charts, and Kakao Maps integration.

Responsibilities:

- Implement landing page, map search, filters, marker interactions, detail page, favorites, community, notices, admin views, and login callback flow.
- Use Vue Router, Pinia, Axios, Kakao Maps API, and ECharts.
- Keep the first screen useful and avoid marketing-only experiences except for the dedicated landing route.
- Make map/list/detail workflows efficient on desktop and mobile.
- Coordinate with Backend API Agent on endpoint contracts.

Primary outputs:

- Vue components, routes, stores, API clients, map modules, and chart modules.
- Browser verification notes and screenshots when UI work is significant.
- API contract questions or mismatches.

Ownership paths:

- Primary: `frontend/src/views/**`, `frontend/src/components/**`, `frontend/src/router/**`, `frontend/src/stores/**`, `frontend/src/api/**`, `frontend/src/map/**`, `frontend/src/charts/**`, `frontend/src/assets/**`, `frontend/src/styles/**`, `frontend/src/tests/**`
- Shared: `frontend/.env.example`, `docs/api/**`, `docs/contracts/**`, `docs/ux/**`
- Restricted: `backend/**`, `model-server/**`, `db/**` except read-only contract review

### AI / Data Integration Agent

Owns model-server boundaries, data contracts, and apartment-only prediction/explanation flows.

Responsibilities:

- Design and implement FastAPI endpoints for hedonic valuation and SHAP explanations.
- Define apartment-only behavior for predictions and XAI.
- Coordinate model version, 기준일, feature inputs, prediction outputs, and SHAP value format.
- Define graceful behavior when data is missing or the property is not an apartment.
- Help map transaction and property data into model features.

Primary outputs:

- FastAPI endpoint contracts.
- Model input/output schemas.
- Integration notes for Spring Boot.
- Data quality assumptions and missing-data handling.

Ownership paths:

- Primary: `model-server/**`, `docs/model/**`, `docs/contracts/model-server/**`
- Shared: `backend/src/main/java/**/apartment/**`, `backend/src/main/java/**/valuation/**`, `backend/src/main/java/**/shap/**`, `backend/src/main/resources/mapper/**`, `db/**`, `docs/api/**`, `docs/contracts/**`
- Restricted: `frontend/**` except proposing chart/input contract needs, `backend/src/main/java/**/security/**`

### QA / Review Agent

Owns verification, regression review, and release readiness.

Responsibilities:

- Review changes across architecture, backend, auth, frontend, AI, and data integration.
- Create focused test strategies for each feature slice.
- Check role access, non-apartment AI restrictions, map-query performance risks, API validation, and UI error states.
- Confirm documentation and handoff notes are adequate before larger integration.

Primary outputs:

- Review findings ordered by severity.
- Test gaps and verification commands.
- Release-readiness notes.

Ownership paths:

- Primary: `docs/qa/**`, `docs/reviews/**`, test plans and review notes under `docs/**`
- Shared: `backend/src/test/**`, `frontend/src/tests/**`, `model-server/tests/**`, `docs/api/**`, `docs/contracts/**`
- Restricted: production implementation files unless explicitly assigned a focused test or review-fix task

## Live Sub-Agent Operating Model

Live Codex sub-agents should be spawned only for bounded tasks with clear ownership. The main Codex agent remains the coordinator and handles integration.

Recommended spawning pattern:

- Use Architecture / Design Agent before large cross-cutting decisions.
- Use Backend API Agent and Frontend / Map Agent in parallel when API contracts are already clear.
- Use Auth / Security Agent separately for login and authorization work because it cuts across backend and frontend.
- Use AI / Data Integration Agent when valuation, SHAP, or model-server contracts are involved.
- Use QA / Review Agent after implementation slices or before merging significant work.

Do not spawn all agents by default for every task. Start only the agents whose scope directly advances the current implementation slice.

Every live sub-agent prompt should include:

- The selected role file.
- The specific task.
- The primary ownership paths for that task.
- Any shared paths it may inspect.
- A reminder not to revert edits made by other agents or the user.
- Required final output: changed paths, verification performed, contract changes, blockers, and handoff notes.

## Initial Build Order

1. Create the `.agents/` role definition files.
2. Add a root `AGENTS.md` that points contributors and Codex agents to `.agents/README.md`.
3. Scaffold the project in implementation phases:
   - Backend API server.
   - Frontend SPA.
   - Model server.
   - Database schema and seed strategy.
4. Use task-scoped live sub-agents during those phases, not before the persistent role files exist.

## Open Decisions

The following decisions are intentionally deferred until project scaffolding begins:

- Monorepo layout names for backend, frontend, model server, and database scripts.
- Local dev orchestration method.
- OAuth2 app credentials and redirect URIs.
- Kakao Maps API key loading strategy.
- Initial sample dataset source and import pipeline.
- Model training and artifact storage format.

## Acceptance Criteria

The sub-agent setup is complete when:

- `.agents/README.md` exists and explains how to use the agents.
- All six requested role definition files exist.
- Each role file states responsibilities, non-responsibilities, inputs, outputs, handoff rules, and first tasks.
- Each role file states primary ownership paths, shared / contract paths, and restricted paths.
- Root `AGENTS.md` points to the role definitions.
- The setup supports both persistent project-local guidance and live Codex sub-agent spawning.
