import { afterEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/api/client'
import { noticesApi } from '@/api/notices'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('noticesApi', () => {
  it('uses public notice read endpoints', async () => {
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({ data: { items: [], page: null } })

    await noticesApi.list({ page: 0, size: 20, sort: 'publishedAt,desc' })

    expect(getSpy).toHaveBeenCalledWith('/notices', {
      params: {
        page: 0,
        size: 20,
        sort: 'publishedAt,desc'
      }
    })
  })

  it('uses admin-only notice management read endpoints', async () => {
    const getSpy = vi
      .spyOn(apiClient, 'get')
      .mockResolvedValueOnce({ data: { items: [], page: null } })
      .mockResolvedValueOnce({ data: { noticeId: 1 } })

    await noticesApi.adminList({ page: 0, size: 50, keyword: '점검' })
    await noticesApi.adminGet(1)

    expect(getSpy).toHaveBeenNthCalledWith(1, '/admin/notices', {
      params: {
        page: 0,
        size: 50,
        keyword: '점검'
      }
    })
    expect(getSpy).toHaveBeenNthCalledWith(2, '/admin/notices/1')
  })

  it('uses admin-only notice mutation endpoints', async () => {
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({ data: { noticeId: 1, message: 'created' } })
    const putSpy = vi.spyOn(apiClient, 'put').mockResolvedValueOnce({ data: { noticeId: 1, message: 'updated' } })
    const deleteSpy = vi.spyOn(apiClient, 'delete').mockResolvedValueOnce({ data: { noticeId: 1, message: 'deleted' } })
    const payload = {
      title: '공지',
      content: '내용',
      pinned: false,
      publishedAt: '2026-06-24T12:00:00+09:00'
    }

    await noticesApi.create(payload)
    await noticesApi.update(1, payload)
    await noticesApi.remove(1)

    expect(postSpy).toHaveBeenCalledWith('/admin/notices', payload)
    expect(putSpy).toHaveBeenCalledWith('/admin/notices/1', payload)
    expect(deleteSpy).toHaveBeenCalledWith('/admin/notices/1')
  })
})
