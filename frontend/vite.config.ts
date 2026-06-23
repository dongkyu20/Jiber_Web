import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

const rootEnvDir = fileURLToPath(new URL('..', import.meta.url))

export default defineConfig({
  envDir: rootEnvDir,
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
})
