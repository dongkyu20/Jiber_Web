# My Page Account Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a protected My Page where authenticated users can update their nickname, change their password, and withdraw by disabling their account.

**Architecture:** Keep account mutations inside the existing `/api/v1/auth` surface so they reuse JWT principal handling, refresh session revocation, and Pinia memory-only auth state. Add focused DTOs and mapper methods instead of changing the database schema. The frontend adds one protected Vue route and keeps all account API calls behind `authApi` and `useAuthStore`.

**Tech Stack:** Spring Boot 3, Spring Security, MyBatis, JUnit 5, Vue 3, Pinia, Vue Router, Axios, Vitest, Vue Test Utils.

---

## File Structure

- Modify `backend/src/main/java/com/jiber/backend/auth/controller/AuthController.java`: add account mutation endpoints and clear refresh cookie on deactivate.
- Modify `backend/src/main/java/com/jiber/backend/auth/service/AuthService.java`: add current-user lookup, profile update, password change, and deactivation behavior.
- Modify `backend/src/main/java/com/jiber/backend/auth/mapper/AuthUserMapper.java`: add display-name and enabled-state update methods.
- Modify `backend/src/main/resources/mapper/AuthUserMapper.xml`: implement the two mapper updates.
- Create `backend/src/main/java/com/jiber/backend/auth/dto/UpdateProfileRequest.java`: validates nickname input.
- Create `backend/src/main/java/com/jiber/backend/auth/dto/ChangePasswordRequest.java`: validates current/new password input.
- Create `backend/src/main/java/com/jiber/backend/auth/dto/DeactivateAccountRequest.java`: validates password confirmation input.
- Create `backend/src/main/java/com/jiber/backend/auth/dto/AccountMutationResponse.java`: simple Korean message response.
- Modify `backend/src/main/java/com/jiber/backend/security/SecurityConfig.java`: require authentication for `/api/v1/auth/account/**`.
- Modify backend tests in `backend/src/test/java/com/jiber/backend/auth/AuthControllerTest.java`, `backend/src/test/java/com/jiber/backend/auth/AuthUserMapperMyBatisTest.java`, and `backend/src/test/java/com/jiber/backend/security/SecurityRulesTest.java`.
- Modify `frontend/src/api/types.ts`: add account-management request/response types.
- Modify `frontend/src/api/auth.ts`: add profile, password, and deactivation API methods.
- Modify `frontend/src/stores/auth.ts`: add store actions that update or clear auth state.
- Modify `frontend/src/router/index.ts`: add `/mypage` protected route.
- Modify `frontend/src/components/AppHeader.vue`: add a My Page link for authenticated users.
- Create `frontend/src/views/MyPageView.vue`: implement the account management UI.
- Modify frontend tests in `frontend/src/api/__tests__/auth.test.ts`, `frontend/src/stores/__tests__/auth.store.test.ts`, `frontend/src/components/__tests__/AppHeader.test.ts`, and create `frontend/src/views/__tests__/myPageView.test.ts`.
- Modify `docs/contracts/auth-flow.md`: document the new account endpoints.

## Task 1: Backend Tests

**Files:**
- Modify: `backend/src/test/java/com/jiber/backend/auth/AuthControllerTest.java`
- Modify: `backend/src/test/java/com/jiber/backend/auth/AuthUserMapperMyBatisTest.java`
- Modify: `backend/src/test/java/com/jiber/backend/security/SecurityRulesTest.java`

- [ ] **Step 1: Add failing controller tests**

Add tests that call the wished-for controller methods:

```java
var updated = controller.updateProfile(auth(1L), new UpdateProfileRequest("New Name"));
var changed = controller.changePassword(auth(1L), new ChangePasswordRequest(CREDENTIAL, "new-valid-credential-1"));
var deactivated = controller.deactivateAccount("raw-refresh-token", auth(1L), new DeactivateAccountRequest(CREDENTIAL), response);
```

Assert updated user display name, password rotation revokes user sessions, wrong current password returns `INVALID_CREDENTIALS`, deactivation sets `enabled=false`, revokes sessions, and clears the refresh cookie.

- [ ] **Step 2: Add failing mapper tests**

Add tests for:

```java
authUserMapper.updateDisplayName(user.userId(), "New Name", updatedAt);
authUserMapper.updateEnabled(user.userId(), false, updatedAt);
```

Assert only the target user changes.

- [ ] **Step 3: Add failing security tests**

Add MockMvc checks:

```java
patch("/api/v1/auth/account/profile")
patch("/api/v1/auth/account/password")
delete("/api/v1/auth/account/deactivate")
```

Anonymous calls must return `401 AUTH_REQUIRED`; authenticated calls with a mock `AuthUserPrincipal` must reach the controller and return a non-401 status.

- [ ] **Step 4: Run backend red tests**

Run:

```bash
cd backend
./mvnw -Dtest=AuthControllerTest,AuthUserMapperMyBatisTest,SecurityRulesTest test
```

Expected: compilation or test failures because account DTOs and controller methods do not exist yet.

## Task 2: Backend Implementation

**Files:**
- Create: `backend/src/main/java/com/jiber/backend/auth/dto/UpdateProfileRequest.java`
- Create: `backend/src/main/java/com/jiber/backend/auth/dto/ChangePasswordRequest.java`
- Create: `backend/src/main/java/com/jiber/backend/auth/dto/DeactivateAccountRequest.java`
- Create: `backend/src/main/java/com/jiber/backend/auth/dto/AccountMutationResponse.java`
- Modify: `backend/src/main/java/com/jiber/backend/auth/mapper/AuthUserMapper.java`
- Modify: `backend/src/main/resources/mapper/AuthUserMapper.xml`
- Modify: `backend/src/main/java/com/jiber/backend/auth/service/AuthService.java`
- Modify: `backend/src/main/java/com/jiber/backend/auth/controller/AuthController.java`
- Modify: `backend/src/main/java/com/jiber/backend/security/SecurityConfig.java`

- [ ] **Step 1: Add DTOs**

Create records with Bean Validation:

```java
public record UpdateProfileRequest(@NotBlank @Size(max = 100) String displayName) {}
public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = PasswordPolicy.MIN_LENGTH) String newPassword) {}
public record DeactivateAccountRequest(@NotBlank String password) {}
public record AccountMutationResponse(String message) {}
```

- [ ] **Step 2: Add mapper methods**

Add Java methods:

```java
int updateDisplayName(Long userId, String displayName, OffsetDateTime updatedAt);
int updateEnabled(Long userId, Boolean enabled, OffsetDateTime updatedAt);
```

Add SQL updates against `users.display_name`, `users.enabled`, and `users.updated_at`.

- [ ] **Step 3: Add service methods**

Implement:

```java
public AuthUserResponse updateProfile(Authentication authentication, UpdateProfileRequest request)
public AccountMutationResponse changePassword(Authentication authentication, ChangePasswordRequest request)
public AccountMutationResponse deactivateAccount(Authentication authentication, DeactivateAccountRequest request)
```

Use the authenticated principal user id, reload the enabled user from `AuthUserMapper`, verify passwords with `matches`, validate new password with `PasswordPolicy`, revoke refresh sessions after password change and deactivation, and return Korean success messages.

- [ ] **Step 4: Add controller endpoints**

Add:

```java
@PatchMapping("/account/profile")
@PatchMapping("/account/password")
@DeleteMapping("/account/deactivate")
```

Use `@Valid @RequestBody`, pass `Authentication`, and clear the refresh cookie on deactivation.

- [ ] **Step 5: Protect account endpoints**

Add an authenticated matcher in `SecurityConfig`:

```java
.requestMatchers("/api/v1/auth/account/**").authenticated()
```

- [ ] **Step 6: Run backend green tests**

Run:

```bash
cd backend
./mvnw -Dtest=AuthControllerTest,AuthUserMapperMyBatisTest,SecurityRulesTest test
```

Expected: all selected backend tests pass.

## Task 3: Frontend Tests

**Files:**
- Modify: `frontend/src/api/__tests__/auth.test.ts`
- Modify: `frontend/src/stores/__tests__/auth.store.test.ts`
- Modify: `frontend/src/components/__tests__/AppHeader.test.ts`
- Create: `frontend/src/views/__tests__/myPageView.test.ts`

- [ ] **Step 1: Add failing API tests**

Assert calls:

```ts
authApi.updateProfile({ displayName: 'New Name' })
authApi.changePassword({ currentPassword: 'old-password-8', newPassword: 'new-password-8' })
authApi.deactivateAccount({ password: 'old-password-8' })
```

Expected endpoints are `/auth/account/profile`, `/auth/account/password`, and `/auth/account/deactivate`.

- [ ] **Step 2: Add failing store tests**

Assert `updateProfile` replaces `store.user.displayName`; `changePassword` and `deactivateAccount` clear `accessToken` and `user`.

- [ ] **Step 3: Add failing header and view tests**

Header test should find a `/mypage` link after login. My Page view tests should mount with an authenticated store, submit nickname/password/deactivation forms, and verify API/store calls plus success or route behavior.

- [ ] **Step 4: Run frontend red tests**

Run:

```bash
cd frontend
npm run test -- auth.test.ts auth.store.test.ts AppHeader.test.ts myPageView.test.ts
```

Expected: failures because My Page view and auth API/store methods do not exist yet.

## Task 4: Frontend Implementation

**Files:**
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/auth.ts`
- Modify: `frontend/src/stores/auth.ts`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/components/AppHeader.vue`
- Create: `frontend/src/views/MyPageView.vue`

- [ ] **Step 1: Add API types and methods**

Add request/response interfaces and `authApi` methods for the three endpoints. `deactivateAccount` must use Axios `delete` with a request body through `{ data: payload, withCredentials: true }`.

- [ ] **Step 2: Add store actions**

Implement:

```ts
async updateProfile(payload) { this.user = await authApi.updateProfile(payload) }
async changePassword(payload) { await authApi.changePassword(payload); this.clearSession(); this.bootstrapped = true }
async deactivateAccount(payload) { await authApi.deactivateAccount(payload); this.clearSession(); this.bootstrapped = true }
```

- [ ] **Step 3: Add route and header link**

Add `/mypage` with `requiresAuth: true, roles: ['USER', 'ADMIN']`, and add a signed-in header link labeled in Korean.

- [ ] **Step 4: Build `MyPageView.vue`**

Use three panels: nickname, password, withdrawal. Validate empty nickname, password confirmation mismatch, and withdrawal checkbox before API calls. Use Korean labels and status messages.

- [ ] **Step 5: Run frontend green tests**

Run:

```bash
cd frontend
npm run test -- auth.test.ts auth.store.test.ts AppHeader.test.ts myPageView.test.ts
```

Expected: all selected frontend tests pass.

## Task 5: Docs And Verification

**Files:**
- Modify: `docs/contracts/auth-flow.md`

- [ ] **Step 1: Document account endpoints**

Add a short `Account Management` section with the three endpoint shapes and security rules.

- [ ] **Step 2: Run focused verification**

Run:

```bash
cd backend
./mvnw -Dtest=AuthControllerTest,AuthUserMapperMyBatisTest,SecurityRulesTest test
cd ../frontend
npm run test -- auth.test.ts auth.store.test.ts AppHeader.test.ts myPageView.test.ts
npm run typecheck
```

Expected: all commands pass.

- [ ] **Step 3: Review changed paths**

Run:

```bash
git status --short
git diff -- backend/src/main/java/com/jiber/backend/auth frontend/src/views/MyPageView.vue
```

Expected: only planned files changed, plus pre-existing unrelated dirty files still untouched.
