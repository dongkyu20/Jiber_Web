import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'

import AppHeader from '@/components/AppHeader.vue'
import { useAuthStore } from '@/stores/auth'

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<main />' } },
      { path: '/map', component: { template: '<main />' } },
      { path: '/favorites', component: { template: '<main />' } },
      { path: '/notices', component: { template: '<main />' } },
      { path: '/admin', component: { template: '<main />' } }
    ]
  })
}

async function mountHeader(authenticated = false) {
  const pinia = createPinia()
  setActivePinia(pinia)

  const router = createTestRouter()
  await router.push('/')
  await router.isReady()

  const authStore = useAuthStore()
  if (authenticated) {
    authStore.setSession({
      accessToken: 'memory-only-token',
      user: {
        userId: 1,
        email: 'user@example.com',
        displayName: '테스터',
        roles: ['USER']
      }
    })
  }

  return mount(AppHeader, {
    global: {
      plugins: [pinia, router]
    }
  })
}

describe('AppHeader', () => {
  it('renders Kakao, Google, and Naver login buttons for guests', async () => {
    const wrapper = await mountHeader()
    const buttonTexts = wrapper.findAll('button').map((button) => button.text())

    expect(buttonTexts).toEqual(expect.arrayContaining(['카카오 로그인', '구글 로그인', '네이버 로그인']))
    expect(buttonTexts).not.toContain('로그아웃')
  })

  it('hides provider login buttons after login', async () => {
    const wrapper = await mountHeader(true)
    const buttonTexts = wrapper.findAll('button').map((button) => button.text())

    expect(wrapper.text()).toContain('테스터님')
    expect(buttonTexts).toEqual(['로그아웃'])
    expect(buttonTexts).not.toEqual(expect.arrayContaining(['카카오 로그인', '구글 로그인', '네이버 로그인']))
  })
})
