<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import { getOAuthStartUrl, type OAuthProvider } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const loginProviders: { provider: OAuthProvider; label: string; secondary?: boolean }[] = [
  { provider: 'kakao', label: '카카오 로그인' },
  { provider: 'google', label: '구글 로그인', secondary: true },
  { provider: 'naver', label: '네이버 로그인', secondary: true }
]

const displayName = computed(() => authStore.user?.displayName ?? '방문자')

function startLogin(provider: OAuthProvider) {
  window.location.href = getOAuthStartUrl(provider)
}

async function logout() {
  await authStore.logout()
  await router.push('/')
}
</script>

<template>
  <header class="site-header">
    <RouterLink class="brand" to="/" aria-label="Jiber 홈">
      <span class="brand-mark" aria-hidden="true">J</span>
      <span>Jiber</span>
    </RouterLink>

    <nav class="main-nav" aria-label="주요 메뉴">
      <RouterLink to="/map">지도 검색</RouterLink>
      <RouterLink to="/favorites">즐겨찾기</RouterLink>
      <RouterLink to="/notices">공지사항</RouterLink>
      <RouterLink to="/admin">관리자</RouterLink>
    </nav>

    <div class="auth-actions">
      <span v-if="authStore.isAuthenticated" class="user-label">{{ displayName }}님</span>
      <template v-else>
        <button
          v-for="loginProvider in loginProviders"
          :key="loginProvider.provider"
          class="text-button"
          :class="{ secondary: loginProvider.secondary }"
          type="button"
          @click="startLogin(loginProvider.provider)"
        >
          {{ loginProvider.label }}
        </button>
      </template>
      <button v-if="authStore.isAuthenticated" class="text-button secondary" type="button" @click="logout">
        로그아웃
      </button>
    </div>
  </header>
</template>
