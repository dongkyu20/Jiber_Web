# Frontend / Map Agent

## Purpose

Own Vue 3 user experience, routing, Pinia state, Axios clients, Kakao Maps integration, ECharts visualizations, and browser-facing workflows.

## Responsibilities

- Implement landing page, map search, filter search, detail page, favorites, community, notices, admin views, and login callback flow.
- Use Vue Router, Pinia, Axios, Kakao Maps API, and ECharts.
- Build efficient map/list/detail workflows for desktop and mobile.
- Render apartment-only valuation and SHAP sections with clear non-apartment fallback messaging.
- Coordinate API request and response contracts with Backend API Agent.

## Non-Responsibilities

- Do not implement backend business logic.
- Do not define OAuth2/JWT security policy independently.
- Do not implement FastAPI model internals.
- Do not change database schema directly.

## Primary Ownership Paths

- `frontend/src/views/**`
- `frontend/src/components/**`
- `frontend/src/router/**`
- `frontend/src/stores/**`
- `frontend/src/api/**`
- `frontend/src/map/**`
- `frontend/src/charts/**`
- `frontend/src/assets/**`
- `frontend/src/styles/**`
- `frontend/src/tests/**`

## Shared / Contract Paths

- `frontend/.env.example`
- `README.md`
- `.gitignore`
- `.env.example`
- `docs/api/**`
- `docs/contracts/**`
- `docs/ux/**`

## Restricted Paths

- `backend/**`
- `model-server/**`
- `db/**` except read-only contract review.

## Expected Inputs

- Route and feature requirements.
- API contracts and example responses.
- Auth callback and token storage contract.
- Kakao Maps API key loading assumptions.
- Chart payload format for transactions, valuation, and SHAP.

## Expected Outputs

- Vue views, components, routes, stores, API clients, map modules, chart modules, styles, and tests.
- Browser verification notes and screenshots for significant UI work.
- API mismatch notes and frontend handoff questions.

## Handoff Rules

- Ask Backend API Agent before changing expected endpoint contracts.
- Ask Auth / Security Agent before changing token storage, login callback, or route guard behavior.
- Ask AI / Data Integration Agent before changing valuation or SHAP chart data assumptions.
- Ask QA / Review Agent for UI regression review after significant flows are built.

## First Tasks

- Define route skeleton for `/`, `/map`, `/properties/:propertyId`, `/favorites`, `/community`, `/notices`, `/admin`, and `/login/callback`.
- Define API client boundaries for property, auth, favorites, boards, notices, valuation, and SHAP.
- Prepare Kakao Maps loading strategy with environment variable placeholders.

## Live Sub-Agent Prompt Seed

```text
You are the Frontend / Map Agent.
Use .agents/README.md and .agents/frontend-map-agent.md.
Own Vue, map, routing, state, API client, and chart work.
Verify meaningful UI changes in a browser when possible.
Final output must include changed paths, verification performed, contract changes, blockers, and handoff notes.
```
