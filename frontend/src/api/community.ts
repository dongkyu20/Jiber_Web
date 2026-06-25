import { apiClient, compactParams } from './client'
import type {
  CommunityCommentCreateRequest,
  CommunityCommentUpdateRequest,
  CommunityMutationResponse,
  CommunityPostCreateRequest,
  CommunityPostDetail,
  CommunityPostListParams,
  CommunityPostSummary,
  CommunityPostUpdateRequest,
  PagedResponse
} from './types'

export const communityApi = {
  async listPosts(params: CommunityPostListParams = {}): Promise<PagedResponse<CommunityPostSummary>> {
    const { data } = await apiClient.get<PagedResponse<CommunityPostSummary>>('/community/posts', {
      params: compactParams(params)
    })
    return data
  },

  async getPost(postId: string | number): Promise<CommunityPostDetail> {
    const { data } = await apiClient.get<CommunityPostDetail>(`/community/posts/${postId}`)
    return data
  },

  async createPost(payload: CommunityPostCreateRequest): Promise<CommunityMutationResponse> {
    const { data } = await apiClient.post<CommunityMutationResponse>('/community/posts', payload)
    return data
  },

  async updatePost(postId: string | number, payload: CommunityPostUpdateRequest): Promise<CommunityMutationResponse> {
    const { data } = await apiClient.put<CommunityMutationResponse>(`/community/posts/${postId}`, payload)
    return data
  },

  async deletePost(postId: string | number): Promise<CommunityMutationResponse> {
    const { data } = await apiClient.delete<CommunityMutationResponse>(`/community/posts/${postId}`)
    return data
  },

  async createComment(
    postId: string | number,
    payload: CommunityCommentCreateRequest
  ): Promise<CommunityMutationResponse> {
    const { data } = await apiClient.post<CommunityMutationResponse>(`/community/posts/${postId}/comments`, payload)
    return data
  },

  async updateComment(
    postId: string | number,
    commentId: string | number,
    payload: CommunityCommentUpdateRequest
  ): Promise<CommunityMutationResponse> {
    const { data } = await apiClient.put<CommunityMutationResponse>(
      `/community/posts/${postId}/comments/${commentId}`,
      payload
    )
    return data
  },

  async deleteComment(postId: string | number, commentId: string | number): Promise<CommunityMutationResponse> {
    const { data } = await apiClient.delete<CommunityMutationResponse>(`/community/posts/${postId}/comments/${commentId}`)
    return data
  }
}
