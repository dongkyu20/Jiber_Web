import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import MyPageView from '@/views/MyPageView.vue'

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<main />' } },
      { path: '/login', component: { template: '<main />' } },
      { path: '/mypage', component: MyPageView }
    ]
  })
}

async function mountMyPage() {
  const pinia = createPinia()
  setActivePinia(pinia)

  const router = createTestRouter()
  await router.push('/mypage')
  await router.isReady()

  const authStore = useAuthStore()
  authStore.setSession({
    accessToken: 'memory-only-token',
    user: {
      userId: 1,
      email: 'user@example.com',
      displayName: 'Old Nickname',
      roles: ['USER']
    }
  })

  const wrapper = mount(MyPageView, {
    global: {
      plugins: [pinia, router]
    }
  })

  return { wrapper, router, authStore }
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('MyPageView', () => {
  it('shows account summary and updates display name', async () => {
    const updateProfileSpy = vi.spyOn(authApi, 'updateProfile').mockResolvedValueOnce({
      userId: 1,
      email: 'user@example.com',
      displayName: 'New Nickname',
      roles: ['USER']
    })
    const { wrapper, authStore } = await mountMyPage()

    expect(wrapper.text()).toContain('user@example.com')
    expect(wrapper.get<HTMLInputElement>('#mypage-display-name').element.value).toBe('Old Nickname')

    await wrapper.get('#mypage-display-name').setValue('New Nickname')
    await wrapper.get('[data-test="profile-form"]').trigger('submit')
    await flushPromises()

    expect(updateProfileSpy).toHaveBeenCalledWith({ displayName: 'New Nickname' })
    expect(authStore.user?.displayName).toBe('New Nickname')
    expect(wrapper.text()).toContain('닉네임이 변경되었습니다.')
  })

  it('does not submit password change when confirmation differs', async () => {
    const changePasswordSpy = vi.spyOn(authApi, 'changePassword')
    const { wrapper } = await mountMyPage()

    await wrapper.get('#mypage-current-password').setValue('password-8')
    await wrapper.get('#mypage-new-password').setValue('new-password-8')
    await wrapper.get('#mypage-new-password-confirm').setValue('different-password-8')
    await wrapper.get('[data-test="password-form"]').trigger('submit')

    expect(changePasswordSpy).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('새 비밀번호가 일치하지 않습니다.')
  })

  it('clears the session and moves to login after password change', async () => {
    const changePasswordSpy = vi.spyOn(authApi, 'changePassword').mockResolvedValueOnce({
      message: '비밀번호가 변경되었습니다. 다시 로그인해 주세요.'
    })
    const { wrapper, router, authStore } = await mountMyPage()

    await wrapper.get('#mypage-current-password').setValue('password-8')
    await wrapper.get('#mypage-new-password').setValue('new-password-8')
    await wrapper.get('#mypage-new-password-confirm').setValue('new-password-8')
    await wrapper.get('[data-test="password-form"]').trigger('submit')
    await flushPromises()

    expect(changePasswordSpy).toHaveBeenCalledWith({
      currentPassword: 'password-8',
      newPassword: 'new-password-8'
    })
    expect(authStore.user).toBeNull()
    expect(router.currentRoute.value.fullPath).toBe('/login')
  })

  it('deactivates the account only after confirmation', async () => {
    const deactivateSpy = vi.spyOn(authApi, 'deactivateAccount').mockResolvedValueOnce({
      message: '회원탈퇴가 완료되었습니다.'
    })
    const { wrapper, router, authStore } = await mountMyPage()

    await wrapper.get('#mypage-withdraw-password').setValue('password-8')
    await wrapper.get('#mypage-withdraw-confirm').setValue(true)
    await wrapper.get('[data-test="withdraw-form"]').trigger('submit')
    await flushPromises()

    expect(deactivateSpy).toHaveBeenCalledWith({ password: 'password-8' })
    expect(authStore.user).toBeNull()
    expect(router.currentRoute.value.fullPath).toBe('/')
  })
})
