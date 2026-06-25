<script setup lang="ts">
import { useUiStore } from '@/stores/ui'

const uiStore = useUiStore()
</script>

<template>
  <label class="theme-toggle" :title="uiStore.isDarkMode ? '다크모드 사용 중' : '라이트모드 사용 중'">
    <input v-model="uiStore.isDarkMode" type="checkbox" aria-label="다크모드" />
    <span class="theme-toggle-track" aria-hidden="true">
      <span class="theme-toggle-thumb" />
    </span>
    <span class="theme-toggle-label">다크모드</span>
  </label>
</template>

<style scoped>
.theme-toggle {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--cream-muted);
  font-size: 0.82rem;
  font-weight: 700;
  white-space: nowrap;
  cursor: pointer;
  user-select: none;
}

.theme-toggle input {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
}

.theme-toggle-track {
  position: relative;
  width: 42px;
  height: 24px;
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  background: color-mix(in srgb, var(--bg-card) 82%, var(--cream) 18%);
  transition:
    background 0.2s ease,
    border-color 0.2s ease;
}

.theme-toggle-thumb {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--cream-muted);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.24);
  transition:
    background 0.2s ease,
    transform 0.2s ease;
}

.theme-toggle input:checked + .theme-toggle-track {
  border-color: var(--gold);
  background: color-mix(in srgb, var(--gold) 34%, var(--bg-card) 66%);
}

.theme-toggle input:checked + .theme-toggle-track .theme-toggle-thumb {
  background: var(--gold-light);
  transform: translateX(18px);
}

.theme-toggle input:focus-visible + .theme-toggle-track {
  outline: 3px solid rgba(201, 165, 110, 0.26);
  outline-offset: 3px;
}

.theme-toggle-label {
  color: var(--cream-muted);
}

@media (max-width: 760px) {
  .theme-toggle-label {
    display: none;
  }
}
</style>
