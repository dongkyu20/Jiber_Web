import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { NoticeSummary } from '@/api/types'
import CommunityView from '@/views/CommunityView.vue'

const noticesApiMock = vi.hoisted(() => ({
  list: vi.fn()
}))

vi.mock('@/api/notices', () => ({
  noticesApi: noticesApiMock
}))

const publishedNotice: NoticeSummary = {
  noticeId: 501,
  title: '커뮤니티 공지 노출 확인',
  summary: '공지 본문 요약입니다.',
  pinned: true,
  publishedAt: '2026-06-24T10:30:00+09:00',
  createdAt: '2026-06-24T10:00:00+09:00'
}

function noticePage(items: NoticeSummary[] = [publishedNotice]) {
  return {
    items,
    page: {
      number: 0,
      size: 10,
      totalElements: items.length,
      totalPages: items.length ? 1 : 0
    }
  }
}

async function mountCommunityView() {
  const pinia = createPinia()
  setActivePinia(pinia)

  const wrapper = mount(CommunityView, {
    global: {
      plugins: [pinia],
      stubs: {
        RouterLink: {
          props: ['to'],
          template: '<a :href="typeof to === `string` ? to : to.path"><slot /></a>'
        }
      }
    }
  })
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  noticesApiMock.list.mockReset().mockResolvedValue(noticePage())
})

describe('CommunityView', () => {
  it('renders published notices from the public notice API at the top of the board', async () => {
    const wrapper = await mountCommunityView()

    expect(noticesApiMock.list).toHaveBeenCalledWith({ sort: 'publishedAt,desc', page: 0, size: 10 })
    expect(wrapper.text()).toContain('공지')
    expect(wrapper.text()).toContain('고정')
    expect(wrapper.text()).toContain('커뮤니티 공지 노출 확인')
    expect(wrapper.html()).toContain('/community/n-501')
  })

  it('shows a notice loading error instead of silently hiding notices', async () => {
    noticesApiMock.list.mockRejectedValueOnce(new Error('notice api unavailable'))

    const wrapper = await mountCommunityView()

    expect(wrapper.text()).toContain('공지사항을 불러오지 못했습니다.')
  })
})
