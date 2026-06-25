import { apiClient, getBackendPublicBaseUrl } from './client'
import type {
  AccountIdentifierRecoveryRequest,
  AccountMutationResponse,
  AccountRecoveryResponse,
  AuthUser,
  AuthLoginRequest,
  AuthLogoutResponse,
  AuthMeResponse,
  AuthRefreshResponse,
  AuthSessionResponse,
  AuthSignupRequest,
  ChangePasswordRequest,
  DeactivateAccountRequest,
  DirectPasswordResetRequest,
  PasswordRecoveryRequest,
  PendingSocialSignupResponse,
  SocialLinkRequest,
  SocialSignupRequest,
  UpdateProfileRequest
} from './types'

export type OAuthProvider = 'google' | 'kakao' | 'naver'

export const authApi = {
  async getMe(): Promise<AuthMeResponse> {
    const { data } = await apiClient.get<AuthMeResponse>('/auth/me')
    return data
  },

  async login(payload: AuthLoginRequest): Promise<AuthSessionResponse> {
    const { data } = await apiClient.post<AuthSessionResponse>('/auth/login', payload, {
      withCredentials: true
    })
    return data
  },

  async signup(payload: AuthSignupRequest): Promise<AuthSessionResponse> {
    const { data } = await apiClient.post<AuthSessionResponse>('/auth/signup', payload, {
      withCredentials: true
    })
    return data
  },

  async refresh(): Promise<AuthRefreshResponse> {
    const { data } = await apiClient.post<AuthRefreshResponse>('/auth/refresh', null, {
      withCredentials: true
    })
    return data
  },

  async logout(logoutAllDevices = false): Promise<AuthLogoutResponse> {
    const { data } = await apiClient.post<AuthLogoutResponse>(
      '/auth/logout',
      { logoutAllDevices },
      { withCredentials: true }
    )
    return data
  },

  async recoverIdentifier(payload: AccountIdentifierRecoveryRequest): Promise<AccountRecoveryResponse> {
    const { data } = await apiClient.post<AccountRecoveryResponse>('/auth/recovery/identifier', payload)
    return data
  },

  async requestPasswordRecovery(payload: PasswordRecoveryRequest): Promise<AccountRecoveryResponse> {
    const { data } = await apiClient.post<AccountRecoveryResponse>('/auth/recovery/password', payload)
    return data
  },

  async directPasswordReset(payload: DirectPasswordResetRequest): Promise<AccountRecoveryResponse> {
    const { data } = await apiClient.post<AccountRecoveryResponse>('/auth/recovery/password/direct', payload)
    return data
  },

  async getPendingSocialSignup(): Promise<PendingSocialSignupResponse> {
    const { data } = await apiClient.get<PendingSocialSignupResponse>('/auth/social/pending', {
      withCredentials: true
    })
    return data
  },

  async completeSocialSignup(payload: SocialSignupRequest): Promise<AuthSessionResponse> {
    const { data } = await apiClient.post<AuthSessionResponse>('/auth/social/signup', payload, {
      withCredentials: true
    })
    return data
  },

  async linkPendingSocialAccount(payload: SocialLinkRequest): Promise<AuthSessionResponse> {
    const { data } = await apiClient.post<AuthSessionResponse>('/auth/social/link', payload, {
      withCredentials: true
    })
    return data
  },

  async updateProfile(payload: UpdateProfileRequest): Promise<AuthUser> {
    const { data } = await apiClient.patch<AuthUser>('/auth/account/profile', payload)
    return data
  },

  async changePassword(payload: ChangePasswordRequest): Promise<AccountMutationResponse> {
    const { data } = await apiClient.patch<AccountMutationResponse>('/auth/account/password', payload, {
      withCredentials: true
    })
    return data
  },

  async deactivateAccount(payload: DeactivateAccountRequest): Promise<AccountMutationResponse> {
    const { data } = await apiClient.delete<AccountMutationResponse>('/auth/account/deactivate', {
      data: payload,
      withCredentials: true
    })
    return data
  }
}

export function getOAuthStartUrl(provider: OAuthProvider): string {
  return `${getBackendPublicBaseUrl()}/oauth2/authorization/${provider}`
}
