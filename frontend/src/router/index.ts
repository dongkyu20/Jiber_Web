import { createRouter, createWebHistory } from 'vue-router'

import { canAccessRoute, type ProtectedRouteMeta } from './guards'
import { useAuthStore } from '@/stores/auth'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/map',
      name: 'map',
      component: () => import('@/views/MapView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/properties/:propertyId',
      name: 'property-detail',
      component: () => import('@/views/PropertyDetailView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/favorites',
      name: 'favorites',
      component: () => import('@/views/FavoritesView.vue'),
      meta: { requiresAuth: true, roles: ['USER', 'ADMIN'] }
    },
    {
      path: '/mypage',
      name: 'mypage',
      component: () => import('@/views/MyPageView.vue'),
      meta: { requiresAuth: true, roles: ['USER', 'ADMIN'] }
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('@/views/ChatView.vue'),
      meta: { requiresAuth: true, roles: ['USER', 'ADMIN'] }
    },
    {
      path: '/community',
      name: 'community',
      component: () => import('@/views/CommunityView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/community/write',
      name: 'community-write',
      component: () => import('@/views/CommunityWriteView.vue'),
      meta: { requiresAuth: true, roles: ['USER', 'ADMIN'] }
    },
    {
      path: '/news',
      name: 'news',
      component: () => import('@/views/NewsView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/community/:id',
      name: 'community-post',
      component: () => import('@/views/CommunityPostView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/new-analysis',
      name: 'new-analysis',
      component: () => import('@/views/NewApartmentAnalysisView.vue'),
      meta: { requiresAuth: true, roles: ['USER', 'ADMIN'] }
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { requiresAuth: false, guestOnly: true }
    },
    {
      path: '/signup',
      name: 'signup',
      component: () => import('@/views/SignupView.vue'),
      meta: { requiresAuth: false, guestOnly: true }
    },
    {
      path: '/signup/social',
      name: 'social-signup',
      component: () => import('@/views/SocialSignupView.vue'),
      meta: { requiresAuth: false, guestOnly: true }
    },
    {
      path: '/account-recovery',
      name: 'account-recovery',
      component: () => import('@/views/AccountRecoveryView.vue'),
      meta: { requiresAuth: false, guestOnly: true }
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('@/views/AdminView.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] }
    },
    {
      path: '/login/callback',
      name: 'login-callback',
      component: () => import('@/views/LoginCallbackView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { requiresAuth: false }
    }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  const meta = to.meta as ProtectedRouteMeta

  if (to.name !== 'login-callback') {
    await authStore.bootstrapSessionFromCookie()
  }

  const access = canAccessRoute(meta, authStore.user?.roles ?? null, authStore.isAuthenticated)
  if (access.allowed) {
    return true
  }

  if (access.reason === 'ALREADY_AUTHENTICATED') {
    return { name: 'map' }
  }

  if (access.reason === 'AUTH_REQUIRED') {
    return {
      name: 'login',
      query: {
        auth: access.reason,
        redirect: to.fullPath
      }
    }
  }

  return {
    name: 'home',
    query: {
      auth: access.reason,
      redirect: to.fullPath
    }
  }
})
