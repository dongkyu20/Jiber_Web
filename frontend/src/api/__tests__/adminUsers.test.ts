import { afterEach, describe, expect, it, vi } from 'vitest'

import { adminUsersApi } from '@/api/adminUsers'
import { apiClient } from '@/api/client'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('adminUsersApi', () => {
  it('uses admin user list and mutation endpoints', async () => {
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({ data: { items: [], page: null } })
    const patchSpy = vi
      .spyOn(apiClient, 'patch')
      .mockResolvedValueOnce({ data: { message: 'role changed' } })
      .mockResolvedValueOnce({ data: { message: 'enabled changed' } })

    await adminUsersApi.list({ page: 0, size: 20, keyword: 'admin', role: 'ADMIN', enabled: true })
    await adminUsersApi.updateRole(10, { role: 'USER' })
    await adminUsersApi.updateEnabled(10, { enabled: false })

    expect(getSpy).toHaveBeenCalledWith('/admin/users', {
      params: {
        page: 0,
        size: 20,
        keyword: 'admin',
        role: 'ADMIN',
        enabled: true
      }
    })
    expect(patchSpy).toHaveBeenNthCalledWith(1, '/admin/users/10/role', { role: 'USER' })
    expect(patchSpy).toHaveBeenNthCalledWith(2, '/admin/users/10/enabled', { enabled: false })
  })
})
