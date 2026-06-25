import { apiClient, compactParams } from './client'
import type { NewsFeedResponse, NewsSearchParams } from './types'

export const newsApi = {
  async search(params: NewsSearchParams = {}): Promise<NewsFeedResponse> {
    const { data } = await apiClient.get<NewsFeedResponse>('/news', {
      params: compactParams(params)
    })
    return data
  }
}
