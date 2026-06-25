import { apiClient, compactParams } from './client'
import type {
  CommunityCommentCreateRequest,
  CommunityMutationResponse,
  CommunityPostCreateRequest,
  CommunityPostDetail,
  CommunityPostListParams,
  CommunityPostSummary,
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

  async createComment(
    postId: string | number,
    payload: CommunityCommentCreateRequest
  ): Promise<CommunityMutationResponse> {
    const { data } = await apiClient.post<CommunityMutationResponse>(`/community/posts/${postId}/comments`, payload)
    return data
  }
}
