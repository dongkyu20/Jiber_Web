<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUiStore } from '@/stores/ui'

const route = useRoute()
const router = useRouter()
const uiStore = useUiStore()

onMounted(async () => {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  uiStore.openLogin(redirect)
  await router.replace('/')
})
</script>

<template>
  <div></div>
</template>
