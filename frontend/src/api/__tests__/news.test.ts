import { afterEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/api/client'
import { newsApi } from '@/api/news'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('newsApi', () => {
  it('uses the public latest news search endpoint', async () => {
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: {
        available: true,
        keyword: '부동산',
        message: '네이버 뉴스 검색 결과입니다.',
        items: []
      }
    })

    await newsApi.search({ query: '부동산', display: 20 })

    expect(getSpy).toHaveBeenCalledWith('/news', {
      params: {
        query: '부동산',
        display: 20
      }
    })
  })
})
