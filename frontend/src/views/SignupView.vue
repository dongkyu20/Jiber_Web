<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUiStore } from '@/stores/ui'

const router = useRouter()
const uiStore = useUiStore()

onMounted(async () => {
  uiStore.openSignup()
  await router.replace('/')
})
</script>

<template>
  <div></div>
</template>
