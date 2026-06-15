# API Guidelines

## Scope

This document defines the initial cross-service API rules for the Jiber Web MVP. The frontend calls only the Spring Boot API. The Spring Boot API owns business rules, persistence, authentication, authorization, and internal model-server calls.

## Versioning

- Public API prefix: `/api/v1`
- Internal model-server API prefix: `/internal/v1`
- Breaking changes require a contract note under `docs/contracts/` and handoff notes to affected agents.

## Service Boundaries

- `frontend/` calls Spring Boot only.
- `backend/` calls MySQL and the FastAPI model server.
- `model-server/` does not call the frontend or own user authentication.
- `db/` stores canonical application data and import/seed scripts.

## Contract Index

- `docs/contracts/property-api.md`: map search, filter search, property detail, public valuation, and public SHAP routes.
- `docs/contracts/favorites-api.md`: favorite apartment and favorite area routes.
- `docs/contracts/notices-api.md`: public notice read and `ADMIN` notice mutation routes.
- `docs/contracts/auth-flow.md`: OAuth2 login, JWT/session draft, protected route rules, and required security decisions.
- `docs/contracts/model-server.md`: internal FastAPI valuation and SHAP routes.
- `docs/contracts/error-response.md`: shared error response shape and initial error codes.

## Request Rules

- Use JSON request and response bodies unless file upload is explicitly introduced later.
- Use `camelCase` JSON field names.
- Use ISO 8601 timestamps with timezone offset for API responses.
- Use explicit pagination parameters: `page`, `size`, `sort`.
- Use Bean Validation in Spring Boot for public request validation.

## Response Rules

- Successful collection responses should include `items` and `page` metadata.
- Public endpoints should not expose internal model-server payloads directly.
- User-facing text produced by the web UI must be natural Korean.
- API error `message` may be Korean when it is safe to show directly, but the frontend may map `code` to localized Korean copy.

## Auth Rules

- Public read endpoints are allowed for landing, map search, property detail, and notice read.
- Favorites, user profile, community write actions, notice mutation, and admin endpoints require authentication.
- Admin endpoints require the `ADMIN` role.

## Valuation / SHAP Rules

- Public valuation and SHAP endpoints live under Spring Boot `/api/v1/properties/{propertyId}/...`.
- Internal model inference endpoints live under FastAPI `/internal/v1/...`.
- The frontend must not call the model server directly.
- Non-apartment valuation and SHAP are unsupported in the MVP.

## Product Guardrails

- The platform provides data, analysis, and model explanations.
- API responses must not contain investment advice, buy/sell recommendations, guaranteed returns, or ranking language that implies a purchase decision.
