import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Component } from 'vue'

import { authApi } from '@/api/auth'
import LoginModal from '@/components/LoginModal.vue'
import SignupModal from '@/components/SignupModal.vue'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import LoginView from '@/views/LoginView.vue'
import SignupView from '@/views/SignupView.vue'
import SocialSignupView from '@/views/SocialSignupView.vue'

const sessionResponse = {
  accessToken: 'memory-only-token',
  tokenType: 'Bearer' as const,
  expiresIn: 900,
  user: {
    userId: 1,
    email: 'user@example.com',
    displayName: 'Test User',
    roles: ['USER' as const]
  }
}

function createApiError(code: string) {
  return {
    isAxiosError: true,
    response: {
      data: {
        code,
        message: 'Request failed.',
        path: '/api/v1/auth',
        timestamp: '2026-06-18T00:00:00+09:00'
      }
    }
  }
}

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<main />' } },
      { path: '/map', component: { template: '<main />' } },
      { path: '/favorites', component: { template: '<main />' } },
      { path: '/login', component: LoginView },
      { path: '/signup', component: SignupView },
      { path: '/signup/social', component: SocialSignupView }
    ]
  })
}

async function mountWithRouter(component: Component, initialPath = '/') {
  const pinia = createPinia()
  setActivePinia(pinia)

  const router = createTestRouter()
  await router.push(initialPath)
  await router.isReady()

  const wrapper = mount(component, {
    global: {
      plugins: [pinia, router]
    }
  })

  return { wrapper, router, authStore: useAuthStore(), uiStore: useUiStore() }
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('LoginView', () => {
  it('opens the login modal with redirect state and returns to the landing page', async () => {
    const { router, uiStore } = await mountWithRouter(LoginView, '/login?redirect=/favorites')
    await flushPromises()

    expect(uiStore.loginOpen).toBe(true)
    expect(uiStore.loginRedirect).toBe('/favorites')
    expect(router.currentRoute.value.fullPath).toBe('/')
  })
})

describe('SignupView', () => {
  it('opens the signup modal and returns to the landing page', async () => {
    const { router, uiStore } = await mountWithRouter(SignupView, '/signup')
    await flushPromises()

    expect(uiStore.signupOpen).toBe(true)
    expect(router.currentRoute.value.fullPath).toBe('/')
  })
})

describe('LoginModal', () => {
  it('logs in with email and follows the stored redirect', async () => {
    const loginSpy = vi.spyOn(authApi, 'login').mockResolvedValueOnce(sessionResponse)
    const { wrapper, router, authStore, uiStore } = await mountWithRouter(LoginModal)
    uiStore.openLogin('/favorites')

    await wrapper.get('#login-id').setValue('user@example.com')
    await wrapper.get('#login-pw').setValue('password-8')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(loginSpy).toHaveBeenCalledWith({ email: 'user@example.com', password: 'password-8' })
    expect(authStore.accessToken).toBe('memory-only-token')
    expect(uiStore.loginOpen).toBe(false)
    expect(router.currentRoute.value.fullPath).toBe('/favorites')
  })

  it('shows an error for invalid credentials', async () => {
    vi.spyOn(authApi, 'login').mockRejectedValueOnce(createApiError('INVALID_CREDENTIALS'))
    const { wrapper } = await mountWithRouter(LoginModal)

    await wrapper.get('#login-id').setValue('user@example.com')
    await wrapper.get('#login-pw').setValue('wrong-pass')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('.modal-error').exists()).toBe(true)
  })
})

describe('SignupModal', () => {
  it('signs up with email and starts a memory-only session', async () => {
    const signupSpy = vi.spyOn(authApi, 'signup').mockResolvedValueOnce(sessionResponse)
    const { wrapper, router, authStore } = await mountWithRouter(SignupModal)

    await wrapper.get('#su-email').setValue('user@example.com')
    await wrapper.get('#su-name').setValue('Test User')
    await wrapper.get('#su-pw').setValue('password-8')
    await wrapper.get('#su-pw2').setValue('password-8')
    const agreements = wrapper.findAll('.agree-item input')
    await agreements[0].setValue(true)
    await agreements[1].setValue(true)
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(signupSpy).toHaveBeenCalledWith({
      email: 'user@example.com',
      displayName: 'Test User',
      password: 'password-8'
    })
    expect(authStore.accessToken).toBe('memory-only-token')
    expect(router.currentRoute.value.fullPath).toBe('/map')
  })

  it('shows duplicate email errors', async () => {
    vi.spyOn(authApi, 'signup').mockRejectedValueOnce(createApiError('EMAIL_ALREADY_EXISTS'))
    const { wrapper } = await mountWithRouter(SignupModal)

    await wrapper.get('#su-email').setValue('user@example.com')
    await wrapper.get('#su-name').setValue('Test User')
    await wrapper.get('#su-pw').setValue('password-8')
    await wrapper.get('#su-pw2').setValue('password-8')
    const agreements = wrapper.findAll('.agree-item input')
    await agreements[0].setValue(true)
    await agreements[1].setValue(true)
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('.modal-error').exists()).toBe(true)
  })

  it('validates required fields before signup', async () => {
    const signupSpy = vi.spyOn(authApi, 'signup')
    const { wrapper } = await mountWithRouter(SignupModal)

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(signupSpy).not.toHaveBeenCalled()
    expect(wrapper.find('.modal-error').exists()).toBe(true)
  })
})

describe('SocialSignupView', () => {
  it('loads pending social preview and prioritizes existing account linking when email matches', async () => {
    const linkSpy = vi.spyOn(authApi, 'linkPendingSocialAccount')
    vi.spyOn(authApi, 'getPendingSocialSignup').mockResolvedValueOnce({
      provider: 'NAVER',
      email: 'user@example.com',
      displayName: 'Naver User',
      matchingEmailAccountExists: true
    })

    const { wrapper } = await mountWithRouter(SocialSignupView, '/signup/social')
    await flushPromises()

    expect(wrapper.text()).toContain('user@example.com')
    expect(wrapper.find('[data-test="social-link-form"]').exists()).toBe(true)
    expect(linkSpy).not.toHaveBeenCalled()
  })

  it('shows a safe empty state when pending social signup is missing', async () => {
    const pendingSpy = vi
      .spyOn(authApi, 'getPendingSocialSignup')
      .mockRejectedValueOnce(createApiError('SOCIAL_PENDING_NOT_FOUND'))

    const { wrapper } = await mountWithRouter(SocialSignupView, '/signup/social')
    await flushPromises()

    expect(pendingSpy).toHaveBeenCalledTimes(1)
    expect(wrapper.find('form').exists()).toBe(false)
  })

  it('completes social signup and starts a memory-only session', async () => {
    const socialSignupSpy = vi.spyOn(authApi, 'completeSocialSignup').mockResolvedValueOnce(sessionResponse)
    vi.spyOn(authApi, 'getPendingSocialSignup').mockResolvedValueOnce({
      provider: 'KAKAO',
      email: 'user@example.com',
      displayName: 'Kakao User',
      matchingEmailAccountExists: false
    })
    const { wrapper, router, authStore } = await mountWithRouter(SocialSignupView, '/signup/social')
    await flushPromises()

    await wrapper.get('[data-test="social-signup-display-name"]').setValue('Kakao User')
    await wrapper.get('[data-test="social-signup-password"]').setValue('password-8')
    await wrapper.get('[data-test="social-signup-form"]').trigger('submit')
    await flushPromises()

    expect(socialSignupSpy).toHaveBeenCalledWith({
      email: 'user@example.com',
      displayName: 'Kakao User',
      password: 'password-8'
    })
    expect(authStore.accessToken).toBe('memory-only-token')
    expect(router.currentRoute.value.fullPath).toBe('/map')
  })

  it('switches to existing account guidance when social signup email already exists', async () => {
    vi.spyOn(authApi, 'completeSocialSignup').mockRejectedValueOnce(createApiError('EMAIL_ALREADY_EXISTS'))
    vi.spyOn(authApi, 'getPendingSocialSignup').mockResolvedValueOnce({
      provider: 'GOOGLE',
      email: 'user@example.com',
      displayName: 'Google User',
      matchingEmailAccountExists: false
    })
    const { wrapper } = await mountWithRouter(SocialSignupView, '/signup/social')
    await flushPromises()

    await wrapper.get('[data-test="social-signup-display-name"]').setValue('Google User')
    await wrapper.get('[data-test="social-signup-password"]').setValue('password-8')
    await wrapper.get('[data-test="social-signup-form"]').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[data-test="social-link-form"]').exists()).toBe(true)
  })

  it('links a pending social account after existing account password verification', async () => {
    const linkSpy = vi.spyOn(authApi, 'linkPendingSocialAccount').mockResolvedValueOnce(sessionResponse)
    vi.spyOn(authApi, 'getPendingSocialSignup').mockResolvedValueOnce({
      provider: 'NAVER',
      email: 'user@example.com',
      displayName: 'Naver User',
      matchingEmailAccountExists: true
    })
    const { wrapper, router, authStore } = await mountWithRouter(SocialSignupView, '/signup/social')
    await flushPromises()

    await wrapper.get('[data-test="social-link-email"]').setValue('user@example.com')
    await wrapper.get('[data-test="social-link-password"]').setValue('password-8')
    await wrapper.get('[data-test="social-link-form"]').trigger('submit')
    await flushPromises()

    expect(linkSpy).toHaveBeenCalledWith({ email: 'user@example.com', password: 'password-8' })
    expect(authStore.accessToken).toBe('memory-only-token')
    expect(router.currentRoute.value.fullPath).toBe('/map')
  })

  it('shows safe messages for social link failures', async () => {
    vi.spyOn(authApi, 'linkPendingSocialAccount').mockRejectedValueOnce(createApiError('SOCIAL_ACCOUNT_ALREADY_LINKED'))
    vi.spyOn(authApi, 'getPendingSocialSignup').mockResolvedValueOnce({
      provider: 'NAVER',
      email: 'user@example.com',
      displayName: 'Naver User',
      matchingEmailAccountExists: true
    })
    const { wrapper } = await mountWithRouter(SocialSignupView, '/signup/social')
    await flushPromises()

    await wrapper.get('[data-test="social-link-email"]').setValue('user@example.com')
    await wrapper.get('[data-test="social-link-password"]').setValue('password-8')
    await wrapper.get('[data-test="social-link-form"]').trigger('submit')
    await flushPromises()

    expect(wrapper.find('.form-error').exists()).toBe(true)
  })
})
