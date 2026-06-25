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

    await communityApi.listPosts({ page: 0, size: 15, category: 'FREE', keyword: 'review' })
    await communityApi.getPost(1)

    expect(getSpy).toHaveBeenNthCalledWith(1, '/community/posts', {
      params: { page: 0, size: 15, category: 'FREE', keyword: 'review' }
    })
    expect(getSpy).toHaveBeenNthCalledWith(2, '/community/posts/1')
  })

  it('uses community mutation endpoints', async () => {
    const postSpy = vi
      .spyOn(apiClient, 'post')
      .mockResolvedValueOnce({ data: { id: 1, message: 'created' } })
      .mockResolvedValueOnce({ data: { id: 2, message: 'commented' } })
    const putSpy = vi
      .spyOn(apiClient, 'put')
      .mockResolvedValueOnce({ data: { id: 1, message: 'updated' } })
      .mockResolvedValueOnce({ data: { id: 2, message: 'comment updated' } })
    const deleteSpy = vi
      .spyOn(apiClient, 'delete')
      .mockResolvedValueOnce({ data: { id: 1, message: 'deleted' } })
      .mockResolvedValueOnce({ data: { id: 2, message: 'comment deleted' } })

    await communityApi.createPost({ category: 'QNA', title: 'question', content: 'body' })
    await communityApi.updatePost(1, { category: 'FREE', title: 'updated', content: 'updated body' })
    await communityApi.deletePost(1)
    await communityApi.createComment(1, { content: 'comment' })
    await communityApi.updateComment(1, 2, { content: 'edited comment' })
    await communityApi.deleteComment(1, 2)

    expect(postSpy).toHaveBeenNthCalledWith(1, '/community/posts', { category: 'QNA', title: 'question', content: 'body' })
    expect(postSpy).toHaveBeenNthCalledWith(2, '/community/posts/1/comments', { content: 'comment' })
    expect(putSpy).toHaveBeenNthCalledWith(1, '/community/posts/1', { category: 'FREE', title: 'updated', content: 'updated body' })
    expect(putSpy).toHaveBeenNthCalledWith(2, '/community/posts/1/comments/2', { content: 'edited comment' })
    expect(deleteSpy).toHaveBeenNthCalledWith(1, '/community/posts/1')
    expect(deleteSpy).toHaveBeenNthCalledWith(2, '/community/posts/1/comments/2')
  })
})
