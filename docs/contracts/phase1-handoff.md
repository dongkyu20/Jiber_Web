# Phase 1 Contract Handoff Notes

## Scope

These notes summarize how the current contract changes affect Phase 1 backend, frontend, auth, and AI work. They are not production code requirements by themselves; implementation agents should use the referenced contract documents as source of truth.

## Backend API Agent Impact

- Implement property search DTOs using `swLat`, `swLng`, `neLat`, `neLng`, `propertyTypes`, `transactionTypes`, and `zoomLevel`.
- Use comma-separated query parsing for list filters.
- Support administrative filters: `sido`, `sigungu`, `legalDong`, `complexName`, and general `keyword`.
- Keep property detail separate from public valuation and public SHAP routes.
- Implement favorite apartment and favorite area endpoints from `docs/contracts/favorites-api.md`.
- Implement public notice read and `ADMIN` notice mutation endpoints from `docs/contracts/notices-api.md`.
- Return shared errors from `docs/contracts/error-response.md`.

## Frontend / Map Agent Impact

- Send map query parameters using `zoomLevel`, plural `propertyTypes`, and plural `transactionTypes`.
- Use `/api/v1/properties/search` for list/sidebar search and include `centerLat`/`centerLng` when distance-prioritized ordering is needed.
- Treat property detail, valuation, and SHAP as separate API calls.
- Use Korean UI text for empty states, validation errors, favorite actions, notice actions, and AI unsupported states.
- Do not call FastAPI model-server endpoints directly.

## Auth / Security Agent Impact

- Resolve the required open decisions in `docs/contracts/auth-flow.md` before implementation:
  - refresh token storage 방식
  - access token 전달 방식
  - logout 처리 방식
  - 최초 ADMIN 부여 방식
- Protect all `/api/v1/favorites/**` endpoints with `USER` or `ADMIN`.
- Protect all `/api/v1/admin/notices/**` endpoints with `ADMIN`.
- Ensure permission failures use `AUTH_REQUIRED` or `ACCESS_DENIED`.

## AI / Data Integration Agent Impact

- Keep FastAPI routes internal under `/internal/v1`.
- Implement apartment-only valuation and SHAP behavior from `docs/contracts/model-server.md`.
- Coordinate feature names and missing-data reason codes with Backend API Agent before DTO implementation.
- Do not expand AI support beyond apartments without Architecture / Design approval.

## QA / Review Agent Impact

- Verify request parameter naming drift: `zoom` and `propertyType` should not be used in Phase 1 contracts.
- Verify anonymous access for map search, filter search, property detail, and public notice reads.
- Verify authenticated access for favorites.
- Verify `ADMIN`-only access for notice mutations.
- Verify non-apartment valuation and SHAP return `VALUATION_UNSUPPORTED_PROPERTY_TYPE`.
- Verify frontend-facing messages remain natural Korean and avoid investment advice.

## Blockers To Track

- Auth token/session decisions are unresolved.
- First `ADMIN` provisioning is unresolved.
- Kakao Maps API key and allowed domains are unresolved.
- MySQL schema and indexes for bounds search, favorite uniqueness, and notices are unresolved.
- Model feature set and model artifact availability are unresolved.
