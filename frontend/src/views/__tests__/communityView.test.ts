import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { CommunityPostSummary } from '@/api/types'
import CommunityView from '@/views/CommunityView.vue'

const communityApiMock = vi.hoisted(() => ({
  listPosts: vi.fn()
}))

vi.mock('@/api/community', () => ({
  communityApi: communityApiMock
}))

const post: CommunityPostSummary = {
  postId: 101,
  category: 'DEAL_REVIEW',
  title: 'Banpo review',
  authorUserId: 7,
  authorDisplayName: 'Reviewer',
  viewCount: 42,
  commentCount: 3,
  createdAt: '2026-06-24T10:30:00+09:00',
  updatedAt: '2026-06-24T10:30:00+09:00'
}

function postPage(items: CommunityPostSummary[] = [post]) {
  return {
    items,
    page: {
      number: 0,
      size: 15,
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
  communityApiMock.listPosts.mockReset().mockResolvedValue(postPage())
})

describe('CommunityView', () => {
  it('renders community posts from the community API', async () => {
    const wrapper = await mountCommunityView()

    expect(communityApiMock.listPosts).toHaveBeenCalledWith({
      page: 0,
      size: 15,
      sort: 'createdAt,desc',
      keyword: '',
      category: undefined
    })
    expect(wrapper.text()).toContain('Banpo review')
    expect(wrapper.text()).toContain('Reviewer')
    expect(wrapper.html()).toContain('/community/101')
  })

  it('shows a community loading error', async () => {
    communityApiMock.listPosts.mockRejectedValueOnce(new Error('community api unavailable'))

    const wrapper = await mountCommunityView()

    expect(wrapper.text()).toContain('커뮤니티 게시글을 불러오지 못했습니다.')
  })
})
