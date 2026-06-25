import { afterEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/api/client'
import { communityApi } from '@/api/community'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('communityApi', () => {
  it('uses community post list and detail endpoints', async () => {
    const getSpy = vi
      .spyOn(apiClient, 'get')
      .mockResolvedValueOnce({ data: { items: [], page: null } })
      .mockResolvedValueOnce({ data: { postId: 1 } })

    await communityApi.listPosts({ page: 0, size: 15, category: 'FREE', keyword: '후기' })
    await communityApi.getPost(1)

    expect(getSpy).toHaveBeenNthCalledWith(1, '/community/posts', {
      params: { page: 0, size: 15, category: 'FREE', keyword: '후기' }
    })
    expect(getSpy).toHaveBeenNthCalledWith(2, '/community/posts/1')
  })

  it('uses community mutation endpoints', async () => {
    const postSpy = vi
      .spyOn(apiClient, 'post')
      .mockResolvedValueOnce({ data: { id: 1, message: 'created' } })
      .mockResolvedValueOnce({ data: { id: 2, message: 'commented' } })

    await communityApi.createPost({ category: 'QNA', title: '질문', content: '내용' })
    await communityApi.createComment(1, { content: '댓글' })

    expect(postSpy).toHaveBeenNthCalledWith(1, '/community/posts', { category: 'QNA', title: '질문', content: '내용' })
    expect(postSpy).toHaveBeenNthCalledWith(2, '/community/posts/1/comments', { content: '댓글' })
  })
})
