import { apiClient, compactParams } from './client'
import type {
  AdminUserEnabledUpdateRequest,
  AdminUserListParams,
  AdminUserMutationResponse,
  AdminUserRoleUpdateRequest,
  AdminUserSummary,
  PagedResponse
} from './types'

export const adminUsersApi = {
  async list(params: AdminUserListParams = {}): Promise<PagedResponse<AdminUserSummary>> {
    const { data } = await apiClient.get<PagedResponse<AdminUserSummary>>('/admin/users', {
      params: compactParams(params)
    })
    return data
  },

  async updateRole(userId: string | number, payload: AdminUserRoleUpdateRequest): Promise<AdminUserMutationResponse> {
    const { data } = await apiClient.patch<AdminUserMutationResponse>(`/admin/users/${userId}/role`, payload)
    return data
  },

  async updateEnabled(
    userId: string | number,
    payload: AdminUserEnabledUpdateRequest
  ): Promise<AdminUserMutationResponse> {
    const { data } = await apiClient.patch<AdminUserMutationResponse>(`/admin/users/${userId}/enabled`, payload)
    return data
  }
}
