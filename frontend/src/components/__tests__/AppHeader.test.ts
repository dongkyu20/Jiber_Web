import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'

import AppHeader from '@/components/AppHeader.vue'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<main />' } },
      { path: '/map', component: { template: '<main />' } },
      { path: '/community', component: { template: '<main />' } },
      { path: '/favorites', component: { template: '<main />' } },
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
        displayName: 'Test User',
        roles: ['USER']
      }
    })
  }

  const wrapper = mount(AppHeader, {
    global: {
      plugins: [pinia, router]
    }
  })

  return { wrapper, uiStore: useUiStore() }
}

describe('AppHeader', () => {
  it('opens login and signup modals for guests', async () => {
    const { wrapper, uiStore } = await mountHeader()
    const buttons = wrapper.findAll('.auth-actions button')

    expect(buttons).toHaveLength(2)

    await buttons[0].trigger('click')
    expect(uiStore.loginOpen).toBe(true)
    expect(uiStore.signupOpen).toBe(false)

    await buttons[1].trigger('click')
    expect(uiStore.loginOpen).toBe(false)
    expect(uiStore.signupOpen).toBe(true)
  })

  it('hides guest auth buttons after login', async () => {
    const { wrapper } = await mountHeader(true)
    const buttons = wrapper.findAll('.auth-actions button')

    expect(wrapper.text()).toContain('Test User')
    expect(buttons).toHaveLength(1)
    expect(wrapper.find('.user-avatar').exists()).toBe(true)
  })
})
