import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { NewsFeedResponse } from '@/api/types'
import NewsView from '@/views/NewsView.vue'

const newsApiMock = vi.hoisted(() => ({
  search: vi.fn()
}))

vi.mock('@/api/news', () => ({
  newsApi: newsApiMock
}))

const feedResponse: NewsFeedResponse = {
  available: true,
  keyword: '부동산',
  message: 'Google 뉴스 RSS 검색 결과입니다.',
  items: [
    {
      title: '서울 아파트 거래량 증가',
      summary: '부동산 시장 최신 흐름을 정리한 기사입니다.',
      link: 'https://news.google.com/rss/articles/example',
      originalLink: 'https://example.com/news/real-estate',
      publishedAt: '2026-06-25T08:30:00+09:00',
      source: 'example.com'
    }
  ]
}

async function mountNewsView() {
  const wrapper = mount(NewsView)
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  newsApiMock.search.mockReset().mockResolvedValue(feedResponse)
})

describe('NewsView', () => {
  it('loads latest real estate news and links each feed item to Google News RSS', async () => {
    const wrapper = await mountNewsView()

    expect(newsApiMock.search).toHaveBeenCalledWith({ query: '부동산', display: 20 })
    expect(wrapper.text()).toContain('최신 부동산 뉴스')
    expect(wrapper.text()).toContain('Google 뉴스 RSS')
    expect(wrapper.text()).toContain('서울 아파트 거래량 증가')
    expect(wrapper.text()).toContain('example.com')
    expect(wrapper.text()).toContain('뉴스 원문 보기')

    const link = wrapper.get('a.news-card')
    expect(link.attributes('href')).toBe('https://news.google.com/rss/articles/example')
    expect(link.attributes('target')).toBe('_blank')
    expect(link.attributes('rel')).toContain('noreferrer')
  })

  it('searches a user-entered real estate keyword', async () => {
    const wrapper = await mountNewsView()

    await wrapper.get('input[type="search"]').setValue('재건축')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(newsApiMock.search).toHaveBeenLastCalledWith({ query: '재건축', display: 20 })
  })
})
