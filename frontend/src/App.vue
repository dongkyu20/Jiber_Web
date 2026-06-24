<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'

import AppHeader from '@/components/AppHeader.vue'
import LoginModal from '@/components/LoginModal.vue'
import SignupModal from '@/components/SignupModal.vue'
import { useUiStore } from '@/stores/ui'

const route = useRoute()
const uiStore = useUiStore()

const isHome = computed(() => route.name === 'home')
const isMap = computed(() => route.name === 'map')
const isFullscreen = computed(() => isHome.value || isMap.value)

const guardMessage = computed(() => {
  if (route.query.auth === 'AUTH_REQUIRED') return '로그인이 필요한 화면입니다.'
  if (route.query.auth === 'ACCESS_DENIED') return '관리자 권한이 필요한 화면입니다.'
  return ''
})
</script>

<template>
  <div class="app-shell">
    <AppHeader v-if="!isHome" />
    <main :class="isFullscreen ? '' : 'app-main'" aria-live="polite">
      <p v-if="guardMessage && !isHome" class="app-alert">{{ guardMessage }}</p>
      <RouterView />
    </main>
    <LoginModal v-if="uiStore.loginOpen" />
    <SignupModal v-if="uiStore.signupOpen" />
  </div>
</template>
