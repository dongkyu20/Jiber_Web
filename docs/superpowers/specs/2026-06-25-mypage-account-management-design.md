# My Page Account Management Design

## Summary

Add a protected Korean My Page where signed-in users can update their nickname, change their password, and withdraw their membership. Membership withdrawal disables the account instead of deleting the `users` row, preserving existing references while blocking future login.

## Goals

- Add `/mypage` as an authenticated route for `USER` and `ADMIN`.
- Let users update `display_name` from the frontend.
- Let local email/password users change passwords after entering their current password.
- Let users withdraw by confirming their password; the backend sets `enabled=false`, revokes refresh sessions, and clears the refresh cookie.
- Keep access tokens in memory only and preserve the existing HttpOnly refresh cookie policy.

## Non-Goals

- Do not hard-delete users.
- Do not add new database tables or migrations.
- Do not implement social-provider unlinking.
- Do not add admin-side changes beyond compatibility with disabled accounts.

## Backend Design

Add authenticated account-management endpoints under `/api/v1/auth/account`:

- `PATCH /profile`
  - Request: `{ "displayName": "New Nickname" }`
  - Response: current `AuthUserResponse`
  - Behavior: trim and validate display name, update `users.display_name`, return the updated principal data.

- `PATCH /password`
  - Request: `{ "currentPassword": "...", "newPassword": "..." }`
  - Response: `{ "message": "<Korean password changed message>" }`
  - Behavior: load current enabled user, require an existing password hash, verify current password, validate the new password with `PasswordPolicy`, update `password_hash`, revoke all refresh sessions for that user.

- `DELETE /deactivate`
  - Request: `{ "password": "..." }`
  - Response: `{ "message": "<Korean membership withdrawal completed message>" }`
  - Behavior: verify password for local accounts, set `users.enabled=false`, revoke all refresh sessions, and clear the refresh cookie in the controller.

Security rules should require authentication for `/api/v1/auth/account/**`. Missing login returns `AUTH_REQUIRED`; wrong password returns `INVALID_CREDENTIALS`; invalid form data returns `VALIDATION_FAILED`.

## Frontend Design

Add `MyPageView.vue` at `/mypage`, using the existing dark panel/form style and Korean UI copy. The page contains three focused sections:

- Account summary: email, current nickname, role badges.
- Nickname form: saves a trimmed nickname and updates Pinia auth state immediately.
- Password form: current password, new password, confirmation; on success clears session and routes to login/home guidance.
- Withdrawal form: password confirmation plus an explicit confirmation checkbox; on success clears session and routes home.

Add a header entry for signed-in users near the existing avatar/name/logout controls. Keep mobile behavior consistent with the current header, which hides auth actions at narrow widths.

## Data Flow

1. Router guard bootstraps the session from the refresh cookie.
2. `/mypage` renders only for authenticated `USER` or `ADMIN`.
3. Form submissions call `authApi` methods.
4. Nickname update calls a store action that replaces `authStore.user`.
5. Password change and withdrawal clear the in-memory access token after backend success because refresh sessions are revoked.

## Testing

Use test-first implementation:

- Backend controller/service tests for profile update, password change, wrong password, and deactivation.
- MyBatis mapper tests for `display_name` update and `enabled=false`.
- Security rule test ensuring `/api/v1/auth/account/**` rejects anonymous requests and accepts authenticated users.
- Frontend API tests for the new endpoints.
- Auth store tests for updating user state and clearing session on password change/deactivation.
- My Page view tests for form validation and success/error states.
- Header/router tests for the new route link and route protection.

## Risks And Decisions

- Social-only accounts may not have a password hash. For this slice, password change and withdrawal require a local password and return `INVALID_CREDENTIALS` when password verification is not possible.
- Disabling a user leaves existing rows intact. Existing admin user management already understands `enabled`, so this aligns with current behavior.
- Password change revokes all refresh sessions to prevent stale sessions from remaining valid after credential rotation.
