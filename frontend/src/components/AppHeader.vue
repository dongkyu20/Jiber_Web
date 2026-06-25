<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import brandLogoUrl from '@/assets/brand/jiper-estate-real-logo-cropped.png'
import ThemeToggle from '@/components/ThemeToggle.vue'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

const authStore = useAuthStore()
const uiStore = useUiStore()
const router = useRouter()

const displayInitial = computed(() => (authStore.user?.displayName ?? '?')[0].toUpperCase())
const displayName = computed(() => authStore.user?.displayName ?? '')

async function logout() {
  await authStore.logout()
  await router.push('/')
}
</script>

<template>
  <header class="site-header">
    <RouterLink to="/" class="brand">
      <img class="brand-logo-img brand-logo-img--header" :src="brandLogoUrl" alt="Jiber Estate Real" />
    </RouterLink>

    <nav class="main-nav" aria-label="주요 메뉴">
      <RouterLink to="/map">지도 검색</RouterLink>
      <RouterLink to="/new-analysis">신규매물 분석</RouterLink>
      <RouterLink to="/chat">AI 챗봇</RouterLink>
      <RouterLink to="/community">커뮤니티</RouterLink>
      <RouterLink to="/news">뉴스</RouterLink>
      <RouterLink to="/favorites">관심목록</RouterLink>
      <RouterLink v-if="authStore.isAdmin" to="/admin">관리자</RouterLink>
    </nav>

    <div class="auth-actions">
      <ThemeToggle />
      <template v-if="authStore.isAuthenticated">
        <div class="user-avatar" :title="displayName">{{ displayInitial }}</div>
        <span class="user-label">{{ displayName }}님</span>
        <RouterLink class="hdr-btn hdr-btn--ghost" to="/mypage">마이페이지</RouterLink>
        <button class="hdr-btn hdr-btn--ghost" type="button" @click="logout">로그아웃</button>
      </template>
      <template v-else>
        <button class="hdr-btn hdr-btn--ghost" type="button" @click="uiStore.openLogin()">로그인</button>
        <button class="hdr-btn hdr-btn--solid" type="button" @click="uiStore.openSignup()">회원가입</button>
      </template>
    </div>
  </header>
</template>

<style scoped>
.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--gold);
  color: #1a1208;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.88rem;
  font-weight: 700;
  flex-shrink: 0;
}

.hdr-btn {
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 0.86rem;
  font-weight: 600;
  white-space: nowrap;
  transition: all 0.2s;
}

.hdr-btn--ghost {
  border: 1px solid var(--border-strong);
  background: transparent;
  color: var(--cream-muted);
}

.hdr-btn--ghost:hover { color: var(--cream); border-color: rgba(200,160,100,0.5); }

.hdr-btn--solid {
  border: 1px solid var(--gold);
  background: var(--gold);
  color: #1a1208;
}

.hdr-btn--solid:hover { background: var(--gold-light); border-color: var(--gold-light); }
</style>
