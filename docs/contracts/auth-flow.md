# Auth Flow Contract Draft

## Scope

The MVP supports social login with Google, Kakao, and Naver through Spring Security OAuth2 Login. The frontend owns route guards and user-facing Korean login states. The backend owns provider integration, user mapping, JWT issuance, refresh policy, and role checks.

## Roles

- `ANONYMOUS`: Public browsing only.
- `USER`: Favorites and authenticated user features.
- `ADMIN`: Notice mutation and admin screens.

## Public Frontend Routes

- `/`
- `/map`
- `/properties/:propertyId`
- `/notices`
- `/login/callback`

## Protected Frontend Routes

- `/favorites`: `USER` or `ADMIN`
- `/admin`: `ADMIN`

## OAuth2 Start Endpoints

- `GET /oauth2/authorization/google`
- `GET /oauth2/authorization/kakao`
- `GET /oauth2/authorization/naver`

## Callback Flow

1. User clicks a social login button in the Vue app.
2. Browser navigates to the backend OAuth2 authorization endpoint.
3. Provider redirects to the backend OAuth2 callback URI.
4. Backend validates the provider response, creates or updates the local user, assigns roles, and issues tokens.
5. Backend redirects the browser to `FRONTEND_PUBLIC_BASE_URL/login/callback`.
6. Frontend loads the current user through `GET /api/v1/auth/me`.

## Session / Token Draft

The exact token storage policy must be finalized by the Auth / Security Agent before implementation.

Preferred draft:

- Access token is short-lived.
- Refresh token is stored in a secure, HttpOnly cookie when browser constraints allow it.
- Frontend does not persist OAuth provider tokens.
- Logout clears server-side refresh state and relevant cookies.

## Required Open Decisions Before Implementation

The Auth / Security Agent must explicitly resolve these decisions before backend or frontend auth implementation starts.

| Decision | Required output | Current draft |
| --- | --- | --- |
| Refresh token storage 방식 | Whether refresh tokens are stored as HttpOnly cookies, server-side sessions, DB-backed token records, or another approved pattern. | Prefer secure HttpOnly cookie plus server-side revocation state when feasible. |
| Access token 전달 방식 | Whether the access token is delivered through response body, Authorization header bootstrap, HttpOnly cookie, or short-lived in-memory frontend state. | Keep access tokens short-lived and avoid localStorage unless explicitly accepted. |
| Logout 처리 방식 | Whether logout revokes refresh tokens server-side, clears cookies, blacklists access tokens, and how multi-device sessions are handled. | Logout should clear relevant cookies and invalidate refresh state. |
| 최초 ADMIN 부여 방식 | How the first administrator is assigned without exposing an open public signup path to `ADMIN`. | Prefer DB seed, migration, or explicit operational script; do not infer admin from email domain in public code without review. |

Until these are resolved, Auth / Security Agent should treat auth implementation as blocked for production-grade behavior.

## Auth API Draft

- `GET /api/v1/auth/me`: returns current user profile and roles.
- `POST /api/v1/auth/refresh`: refreshes access token if refresh policy uses explicit refresh.
- `POST /api/v1/auth/logout`: invalidates refresh token/session state.

`GET /api/v1/auth/me` draft response:

```json
{
  "authenticated": true,
  "user": {
    "userId": 1,
    "email": "user@example.com",
    "displayName": "사용자",
    "roles": ["USER"]
  }
}
```

## Security Guardrails

- Do not commit real OAuth client secrets.
- Do not expose provider access tokens to the frontend.
- Do not log authorization codes, refresh tokens, JWT signing material, or OAuth client secrets.
- Admin role assignment requires an explicit operational decision before production use.
