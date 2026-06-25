import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    sessionStorage.clear()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('keeps the access token in Pinia memory only after callback refresh', async () => {
    vi.spyOn(authApi, 'refresh').mockResolvedValueOnce({
      accessToken: 'memory-only-token',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: {
        userId: 1,
        email: 'user@example.com',
        displayName: '사용자',
        roles: ['USER']
      }
    })
    vi.spyOn(authApi, 'getMe').mockResolvedValueOnce({
      authenticated: true,
      user: {
        userId: 1,
        email: 'user@example.com',
        displayName: '사용자',
        roles: ['USER']
      }
    })

    const store = useAuthStore()
    await store.completeLoginCallback()

    expect(store.accessToken).toBe('memory-only-token')
    expect(store.user?.roles).toContain('USER')
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(sessionStorage.getItem('accessToken')).toBeNull()
  })

  it('bootstraps a session from refresh cookie only once for concurrent callers', async () => {
    const refreshSpy = vi.spyOn(authApi, 'refresh').mockResolvedValue({
      accessToken: 'memory-only-bootstrap-token',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: {
        userId: 4,
        email: 'bootstrap@example.com',
        displayName: '복원 사용자',
        roles: ['USER']
      }
    })

    const store = useAuthStore()
    const [firstResult, secondResult] = await Promise.all([
      store.bootstrapSessionFromCookie(),
      store.bootstrapSessionFromCookie()
    ])

    expect(firstResult).toBe(true)
    expect(secondResult).toBe(true)
    expect(refreshSpy).toHaveBeenCalledTimes(1)
    expect(store.user?.displayName).toBe('복원 사용자')
  })

  it('keeps public bootstrap failures silent and unauthenticated', async () => {
    vi.spyOn(authApi, 'refresh').mockRejectedValueOnce({
      isAxiosError: true,
      response: {
        data: {
          code: 'AUTH_REQUIRED',
          message: '로그인이 필요합니다.',
          path: '/api/v1/auth/refresh',
          timestamp: '2026-06-18T00:00:00+09:00'
        }
      }
    })

    const store = useAuthStore()
    const restored = await store.bootstrapSessionFromCookie()

    expect(restored).toBe(false)
    expect(store.isAuthenticated).toBe(false)
    expect(store.errorMessage).toBeNull()
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(sessionStorage.getItem('accessToken')).toBeNull()
  })

  it('keeps the access token in Pinia memory only after email login', async () => {
    vi.spyOn(authApi, 'login').mockResolvedValueOnce({
      accessToken: 'memory-only-login-token',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: {
        userId: 2,
        email: 'login@example.com',
        displayName: '로그인 사용자',
        roles: ['USER']
      }
    })

    const store = useAuthStore()
    await store.loginWithPassword({ email: 'login@example.com', password: 'password-8' })

    expect(store.accessToken).toBe('memory-only-login-token')
    expect(store.user?.displayName).toBe('로그인 사용자')
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(sessionStorage.getItem('accessToken')).toBeNull()
  })

  it('keeps the access token in Pinia memory only after social link', async () => {
    vi.spyOn(authApi, 'linkPendingSocialAccount').mockResolvedValueOnce({
      accessToken: 'memory-only-social-token',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: {
        userId: 3,
        email: 'linked@example.com',
        displayName: '연결 사용자',
        roles: ['USER']
      }
    })

    const store = useAuthStore()
    await store.linkPendingSocialAccount({ email: 'linked@example.com', password: 'password-8' })

    expect(store.accessToken).toBe('memory-only-social-token')
    expect(store.user?.email).toBe('linked@example.com')
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(sessionStorage.getItem('accessToken')).toBeNull()
  })

  it('updates the current user profile in memory', async () => {
    vi.spyOn(authApi, 'updateProfile').mockResolvedValueOnce({
      userId: 1,
      email: 'user@example.com',
      displayName: 'New Nickname',
      roles: ['USER']
    })

    const store = useAuthStore()
    store.setSession({
      accessToken: 'memory-only-token',
      user: {
        userId: 1,
        email: 'user@example.com',
        displayName: 'Old Nickname',
        roles: ['USER']
      }
    })

    await store.updateProfile({ displayName: 'New Nickname' })

    expect(store.accessToken).toBe('memory-only-token')
    expect(store.user?.displayName).toBe('New Nickname')
  })

  it('clears the session after password change because refresh sessions are revoked', async () => {
    vi.spyOn(authApi, 'changePassword').mockResolvedValueOnce({
      message: '비밀번호가 변경되었습니다.'
    })

    const store = useAuthStore()
    store.setSession({
      accessToken: 'memory-only-token',
      user: {
        userId: 1,
        email: 'user@example.com',
        displayName: 'Test User',
        roles: ['USER']
      }
    })

    await store.changePassword({
      currentPassword: 'password-8',
      newPassword: 'new-password-8'
    })

    expect(store.accessToken).toBeNull()
    expect(store.user).toBeNull()
    expect(store.bootstrapped).toBe(true)
  })

  it('refreshes the access token once and retries password change when authentication expired', async () => {
    const expiredAuthError = {
      isAxiosError: true,
      response: {
        status: 401,
        data: {
          code: 'AUTH_REQUIRED',
          message: '로그인이 필요합니다.',
          path: '/api/v1/auth/account/password',
          timestamp: '2026-06-25T00:00:00+09:00'
        }
      }
    }
    const payload = {
      currentPassword: 'password-8',
      newPassword: 'new-password-8'
    }
    const refreshSpy = vi.spyOn(authApi, 'refresh').mockResolvedValueOnce({
      accessToken: 'refreshed-token',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: {
        userId: 1,
        email: 'user@example.com',
        displayName: 'Test User',
        roles: ['USER']
      }
    })
    const changePasswordSpy = vi
      .spyOn(authApi, 'changePassword')
      .mockRejectedValueOnce(expiredAuthError)
      .mockResolvedValueOnce({
        message: '비밀번호가 변경되었습니다.'
      })

    const store = useAuthStore()
    store.setSession({
      accessToken: 'expired-token',
      user: {
        userId: 1,
        email: 'user@example.com',
        displayName: 'Test User',
        roles: ['USER']
      }
    })

    await store.changePassword(payload)

    expect(refreshSpy).toHaveBeenCalledTimes(1)
    expect(changePasswordSpy).toHaveBeenCalledTimes(2)
    expect(changePasswordSpy).toHaveBeenNthCalledWith(2, payload)
    expect(store.accessToken).toBeNull()
    expect(store.user).toBeNull()
    expect(store.bootstrapped).toBe(true)
  })

  it('clears the session after account deactivation', async () => {
    vi.spyOn(authApi, 'deactivateAccount').mockResolvedValueOnce({
      message: '회원탈퇴가 완료되었습니다.'
    })

    const store = useAuthStore()
    store.setSession({
      accessToken: 'memory-only-token',
      user: {
        userId: 1,
        email: 'user@example.com',
        displayName: 'Test User',
        roles: ['USER']
      }
    })

    await store.deactivateAccount({ password: 'password-8' })

    expect(store.accessToken).toBeNull()
    expect(store.user).toBeNull()
    expect(store.bootstrapped).toBe(true)
  })

  it('clears memory auth state after logout', async () => {
    vi.spyOn(authApi, 'logout').mockResolvedValueOnce({
      message: '로그아웃되었습니다.'
    })

    const store = useAuthStore()
    store.setSession({
      accessToken: 'memory-only-token',
      user: {
        userId: 1,
        email: 'user@example.com',
        displayName: '사용자',
        roles: ['ADMIN']
      }
    })

    await store.logout()

    expect(store.accessToken).toBeNull()
    expect(store.user).toBeNull()
  })
})
