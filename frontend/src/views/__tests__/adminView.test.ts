import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { NoticeDetail, NoticeSummary } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import AdminView from '@/views/AdminView.vue'

const adminUsersApiMock = vi.hoisted(() => ({
  list: vi.fn(),
  updateRole: vi.fn(),
  updateEnabled: vi.fn()
}))

const noticesApiMock = vi.hoisted(() => ({
  adminList: vi.fn(),
  adminGet: vi.fn(),
  create: vi.fn(),
  update: vi.fn(),
  remove: vi.fn()
}))

vi.mock('@/api/notices', () => ({
  noticesApi: noticesApiMock
}))

vi.mock('@/api/adminUsers', () => ({
  adminUsersApi: adminUsersApiMock
}))

const noticeSummary: NoticeSummary = {
  noticeId: 301,
  title: '서비스 점검 안내',
  summary: '서비스 점검 일정을 안내드립니다.',
  pinned: true,
  publishedAt: '2026-06-24T10:30:00+09:00',
  createdAt: '2026-06-24T09:00:00+09:00'
}

const noticeDetail: NoticeDetail = {
  ...noticeSummary,
  content: '서비스 점검 일정을 안내드립니다.',
  updatedAt: '2026-06-24T09:30:00+09:00'
}

function page(items: NoticeSummary[] = [noticeSummary]) {
  return {
    items,
    page: {
      number: 0,
      size: 20,
      totalElements: items.length,
      totalPages: items.length ? 1 : 0
    }
  }
}

const adminUser = {
  userId: 1,
  email: 'admin@example.com',
  displayName: '관리자',
  role: 'ADMIN' as const,
  enabled: true,
  lastLoginAt: '2026-06-24T09:00:00+09:00',
  createdAt: '2026-06-20T09:00:00+09:00',
  updatedAt: '2026-06-24T09:00:00+09:00'
}

const regularUser = {
  userId: 2,
  email: 'user@example.com',
  displayName: '일반 사용자',
  role: 'USER' as const,
  enabled: true,
  lastLoginAt: null,
  createdAt: '2026-06-21T09:00:00+09:00',
  updatedAt: '2026-06-21T09:00:00+09:00'
}

function userPage(items = [adminUser, regularUser]) {
  return {
    items,
    page: {
      number: 0,
      size: 20,
      totalElements: items.length,
      totalPages: items.length ? 1 : 0
    }
  }
}

function createApiError(code: string, message: string) {
  return {
    isAxiosError: true,
    response: {
      data: {
        code,
        message,
        path: '/api/v1/admin/notices',
        timestamp: '2026-06-24T12:00:00+09:00'
      }
    }
  }
}

async function mountAdminView() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.setSession({
    accessToken: 'memory-only-token',
    user: {
      userId: 1,
      email: 'admin@example.com',
      displayName: '관리자',
      roles: ['ADMIN']
    }
  })

  const wrapper = mount(AdminView, {
    global: {
      plugins: [pinia]
    }
  })
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  adminUsersApiMock.list.mockReset().mockResolvedValue(userPage())
  adminUsersApiMock.updateRole.mockReset().mockResolvedValue({
    user: { ...regularUser, role: 'ADMIN' },
    message: '회원 권한을 변경했습니다.'
  })
  adminUsersApiMock.updateEnabled.mockReset().mockResolvedValue({
    user: { ...regularUser, enabled: false },
    message: '회원 상태를 변경했습니다.'
  })
  noticesApiMock.adminList.mockReset().mockResolvedValue(page())
  noticesApiMock.adminGet.mockReset().mockResolvedValue(noticeDetail)
  noticesApiMock.create.mockReset().mockResolvedValue({ noticeId: 302, message: '공지사항을 등록했습니다.' })
  noticesApiMock.update.mockReset().mockResolvedValue({ noticeId: 301, message: '공지사항을 수정했습니다.' })
  noticesApiMock.remove.mockReset().mockResolvedValue({ noticeId: 301, message: '공지사항을 삭제했습니다.' })
})

describe('AdminView', () => {
  it('loads admin notices and renders management dashboard cards', async () => {
    const wrapper = await mountAdminView()

    expect(noticesApiMock.adminList).toHaveBeenCalledWith({ page: 0, size: 50, sort: 'publishedAt,desc' })
    expect(adminUsersApiMock.list).toHaveBeenCalledWith({ page: 0, size: 20, sort: 'createdAt,desc' })
    expect(wrapper.text()).toContain('서비스 점검 안내')
    expect(wrapper.text()).toContain('admin@example.com')
    expect(wrapper.text()).toContain('user@example.com')
    expect(wrapper.text()).toContain('등록 공지')
    expect(wrapper.text()).toContain('상단 고정')
  })

  it('promotes a regular user from the member management table', async () => {
    const wrapper = await mountAdminView()

    await wrapper.get('[data-test="user-role-2"]').trigger('click')
    await flushPromises()

    expect(adminUsersApiMock.updateRole).toHaveBeenCalledWith(2, { role: 'ADMIN' })
    expect(wrapper.text()).toContain('회원 권한을 변경했습니다.')
    expect(adminUsersApiMock.list).toHaveBeenCalledTimes(2)
  })

  it('disables another user but keeps self-lockout actions disabled', async () => {
    const wrapper = await mountAdminView()

    expect(wrapper.get('[data-test="user-role-1"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-test="user-enabled-1"]').attributes('disabled')).toBeDefined()

    await wrapper.get('[data-test="user-enabled-2"]').trigger('click')
    await flushPromises()

    expect(adminUsersApiMock.updateEnabled).toHaveBeenCalledWith(2, { enabled: false })
    expect(wrapper.text()).toContain('회원 상태를 변경했습니다.')
  })

  it('edits an existing notice from the management list', async () => {
    const wrapper = await mountAdminView()

    await wrapper.get('[data-test="notice-edit-301"]').trigger('click')
    await flushPromises()
    await wrapper.get('#notice-title').setValue('서비스 점검 변경 안내')
    await wrapper.get('form[data-test="notice-form"]').trigger('submit')
    await flushPromises()

    expect(noticesApiMock.adminGet).toHaveBeenCalledWith(301)
    expect(noticesApiMock.update).toHaveBeenCalledWith(
      301,
      expect.objectContaining({
        title: '서비스 점검 변경 안내',
        content: '서비스 점검 일정을 안내드립니다.',
        pinned: true
      })
    )
    expect(noticesApiMock.adminList).toHaveBeenCalledTimes(2)
  })

  it('creates a new notice and refreshes the management list', async () => {
    noticesApiMock.adminList.mockResolvedValueOnce(page([])).mockResolvedValueOnce(page([noticeSummary]))
    const wrapper = await mountAdminView()

    await wrapper.get('#notice-title').setValue('새 공지')
    await wrapper.get('#notice-content').setValue('새 공지 내용')
    await wrapper.get('form[data-test="notice-form"]').trigger('submit')
    await flushPromises()

    expect(noticesApiMock.create).toHaveBeenCalledWith(
      expect.objectContaining({
        title: '새 공지',
        content: '새 공지 내용',
        pinned: false
      })
    )
    expect(wrapper.text()).toContain('공지사항을 등록했습니다.')
    expect(noticesApiMock.adminList).toHaveBeenCalledTimes(2)
  })

  it('deletes a notice from the management list and refreshes notices', async () => {
    const wrapper = await mountAdminView()

    await wrapper.get('[data-test="notice-delete-301"]').trigger('click')
    await flushPromises()

    expect(noticesApiMock.remove).toHaveBeenCalledWith(301)
    expect(noticesApiMock.adminList).toHaveBeenCalledTimes(2)
  })

  it('shows backend save failures without also showing the empty notice state', async () => {
    noticesApiMock.adminList.mockResolvedValueOnce(page([]))
    noticesApiMock.create.mockRejectedValueOnce(createApiError('ACCESS_DENIED', '관리자 권한이 필요합니다.'))

    const wrapper = await mountAdminView()
    await wrapper.get('#notice-title').setValue('새 공지')
    await wrapper.get('#notice-content').setValue('새 공지 내용')
    await wrapper.get('form[data-test="notice-form"]').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('관리자 권한이 필요합니다.')
    expect(wrapper.text()).not.toContain('등록된 공지사항이 없습니다.첫 공지를 작성하면 커뮤니티 상단에 노출됩니다.')
  })

  it('does not treat legacy skeleton notice create responses as persisted notices', async () => {
    noticesApiMock.adminList.mockResolvedValueOnce(page([]))
    noticesApiMock.create.mockResolvedValueOnce({ noticeId: 0, message: '공지사항을 등록했습니다.' })

    const wrapper = await mountAdminView()
    await wrapper.get('#notice-title').setValue('새 공지')
    await wrapper.get('#notice-content').setValue('새 공지 내용')
    await wrapper.get('form[data-test="notice-form"]').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('공지사항 저장 결과를 확인하지 못했습니다.')
    expect(noticesApiMock.adminList).toHaveBeenCalledTimes(1)
  })

  it('renders the empty notice message with readable spacing', async () => {
    noticesApiMock.adminList.mockResolvedValueOnce(page([]))

    const wrapper = await mountAdminView()

    expect(wrapper.text()).toContain('등록된 공지사항이 없습니다. 첫 공지를 작성하면 커뮤니티 상단에 노출됩니다.')
  })
})
