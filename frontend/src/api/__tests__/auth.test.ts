import { afterEach, describe, expect, it, vi } from 'vitest'

import { authApi, getOAuthStartUrl } from '@/api/auth'
import { apiClient } from '@/api/client'

const sessionResponse = {
  accessToken: 'memory-only-token',
  tokenType: 'Bearer' as const,
  expiresIn: 900,
  user: {
    userId: 1,
    email: 'user@example.com',
    displayName: '사용자',
    roles: ['USER' as const]
  }
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('authApi OAuth start URL', () => {
  it.each(['google', 'kakao', 'naver'] as const)(
    'builds the %s backend OAuth start URL without nesting under /api/v1',
    (provider) => {
      const startUrl = getOAuthStartUrl(provider)

      expect(startUrl).toMatch(new RegExp(`/oauth2/authorization/${provider}$`))
      expect(startUrl).not.toContain('/api/v1/oauth2')
    }
  )
})

describe('authApi email and social endpoints', () => {
  it('posts email login with credentials included', async () => {
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({ data: sessionResponse })

    await authApi.login({ email: 'user@example.com', password: 'password-8' })

    expect(postSpy).toHaveBeenCalledWith(
      '/auth/login',
      { email: 'user@example.com', password: 'password-8' },
      { withCredentials: true }
    )
  })

  it('posts email signup with credentials included', async () => {
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({ data: sessionResponse })

    await authApi.signup({
      email: 'user@example.com',
      password: 'password-8',
      displayName: '사용자',
      birthDate: '1990-01-23',
      phoneNumber: '010-1234-5678'
    })

    expect(postSpy).toHaveBeenCalledWith(
      '/auth/signup',
      {
        email: 'user@example.com',
        password: 'password-8',
        displayName: '사용자',
        birthDate: '1990-01-23',
        phoneNumber: '010-1234-5678'
      },
      { withCredentials: true }
    )
  })

  it('gets pending social signup state with credentials included', async () => {
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: {
        provider: 'NAVER',
        email: 'user@example.com',
        displayName: '사용자',
        matchingEmailAccountExists: true
      }
    })

    await authApi.getPendingSocialSignup()

    expect(getSpy).toHaveBeenCalledWith('/auth/social/pending', { withCredentials: true })
  })

  it('posts social signup with credentials included', async () => {
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({ data: sessionResponse })

    await authApi.completeSocialSignup({
      email: 'user@example.com',
      password: 'password-8',
      displayName: '사용자'
    })

    expect(postSpy).toHaveBeenCalledWith(
      '/auth/social/signup',
      { email: 'user@example.com', password: 'password-8', displayName: '사용자' },
      { withCredentials: true }
    )
  })

  it('posts social link with credentials included', async () => {
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({ data: sessionResponse })

    await authApi.linkPendingSocialAccount({ email: 'user@example.com', password: 'password-8' })

    expect(postSpy).toHaveBeenCalledWith(
      '/auth/social/link',
      { email: 'user@example.com', password: 'password-8' },
      { withCredentials: true }
    )
  })

  it('posts account recovery requests without credentials', async () => {
    const postSpy = vi
      .spyOn(apiClient, 'post')
      .mockResolvedValueOnce({ data: { message: '가입 이메일 안내를 확인해 주세요.' } })
      .mockResolvedValueOnce({ data: { message: '비밀번호 재설정 안내를 이메일로 보낼 준비가 되었습니다.' } })
      .mockResolvedValueOnce({ data: { message: '정보가 일치하면 비밀번호가 변경되었습니다.' } })

    await authApi.recoverIdentifier({ displayName: '사용자' })
    await authApi.requestPasswordRecovery({ email: 'user@example.com' })
    await authApi.directPasswordReset({
      email: 'user@example.com',
      displayName: '사용자',
      newPassword: 'new-valid-credential-1'
    })

    expect(postSpy).toHaveBeenNthCalledWith(1, '/auth/recovery/identifier', { displayName: '사용자' })
    expect(postSpy).toHaveBeenNthCalledWith(2, '/auth/recovery/password', { email: 'user@example.com' })
    expect(postSpy).toHaveBeenNthCalledWith(3, '/auth/recovery/password/direct', {
      email: 'user@example.com',
      displayName: '사용자',
      newPassword: 'new-valid-credential-1'
    })
  })
})
