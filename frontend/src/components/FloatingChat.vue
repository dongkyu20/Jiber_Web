<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'

import { chatApi } from '@/api/chat'
import type { ChatContext } from '@/api/types'
import { useChatContextStore } from '@/stores/chatContext'
import { useAuthStore } from '@/stores/auth'
import { formatKrw } from '@/utils/format'
import { renderMarkdown } from '@/utils/markdown'

interface Message {
  id: number
  role: 'user' | 'assistant'
  content: string
  contexts?: ChatContext[]
}

const authStore = useAuthStore()
const chatContextStore = useChatContextStore()
const isOpen = ref(false)
const messages = ref<Message[]>([
  { id: 0, role: 'assistant', content: '안녕하세요! 부동산에 대해 궁금한 점을 물어보세요. 현재 매물 분석 결과를 바탕으로 답변해 드립니다.' }
])
const question = ref('')
const loading = ref(false)
const messagesEl = ref<HTMLElement | null>(null)
let idCtr = 1

const activeContext = computed(() => chatContextStore.runtimeContext)
const estimatedPriceText = computed(() => formatKrw(activeContext.value?.valuation?.estimatedPrice))

async function scrollToBottom() {
  await nextTick()
  if (messagesEl.value) {
    messagesEl.value.scrollTop = messagesEl.value.scrollHeight
  }
}

async function submit() {
  const q = question.value.trim()
  if (!q || loading.value) return

  messages.value.push({ id: idCtr++, role: 'user', content: q })
  question.value = ''
  loading.value = true
  await scrollToBottom()

  try {
    const res = await chatApi.askRealEstate({ question: q, runtimeContext: activeContext.value ?? undefined })
    messages.value.push({ id: idCtr++, role: 'assistant', content: res.answer, contexts: res.contexts })
  } catch {
    messages.value.push({ id: idCtr++, role: 'assistant', content: '답변을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.' })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}
</script>

<template>
  <div class="float-wrap">
    <!-- Floating button -->
    <button class="float-btn" :class="{ open: isOpen }" @click="isOpen = !isOpen" aria-label="AI 챗봇">
      <svg v-if="!isOpen" xmlns="http://www.w3.org/2000/svg" width="22" height="22" fill="none" viewBox="0 0 24 24">
        <path stroke="currentColor" stroke-linejoin="round" stroke-width="1.8" d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
      </svg>
      <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="none" viewBox="0 0 24 24">
        <path stroke="currentColor" stroke-linecap="round" stroke-width="2" d="M18 6 6 18M6 6l12 12"/>
      </svg>
    </button>

    <!-- Chat panel -->
    <Transition name="panel">
      <div v-if="isOpen" class="float-panel">
        <div class="panel-header">
          <div class="panel-title">
            <span class="panel-dot" />
            <span>집er AI 챗봇</span>
          </div>
          <!-- Context badge -->
          <div v-if="activeContext" class="ctx-badge">
            <span>{{ activeContext.property.name }} · {{ estimatedPriceText }}</span>
            <button class="ctx-clear" @click="chatContextStore.clearRuntimeContext()">×</button>
          </div>
        </div>

        <div class="panel-messages" ref="messagesEl">
          <div
            v-for="msg in messages"
            :key="msg.id"
            class="msg"
            :class="`msg-${msg.role}`"
          >
            <div v-html="renderMarkdown(msg.content)" />
          </div>
          <div v-if="loading" class="msg msg-assistant">
            <span class="typing"><span /><span /><span /></span>
          </div>
        </div>

        <form class="panel-form" @submit.prevent="submit">
          <input
            v-model="question"
            type="text"
            :placeholder="authStore.isAuthenticated ? '질문을 입력하세요' : '로그인 후 이용 가능합니다'"
            :disabled="!authStore.isAuthenticated || loading"
            class="panel-input"
            @keydown.enter.prevent="submit"
          />
          <button class="panel-send" type="submit" :disabled="!question.trim() || loading || !authStore.isAuthenticated">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" viewBox="0 0 24 24">
              <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19V5M5 12l7-7 7 7"/>
            </svg>
          </button>
        </form>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.float-wrap {
  position: fixed;
  bottom: 32px;
  right: 32px;
  z-index: 200;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.float-btn {
  width: 54px;
  height: 54px;
  border-radius: 50%;
  background: var(--gold);
  color: #1a1208;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 20px rgba(201, 165, 110, 0.45);
  transition: all 0.2s;
  flex-shrink: 0;
}
.float-btn:hover { transform: scale(1.06); box-shadow: 0 8px 24px rgba(201, 165, 110, 0.55); }
.float-btn.open { background: var(--bg-card); color: var(--cream-muted); box-shadow: 0 4px 14px rgba(0,0,0,0.3); }

/* Panel */
.float-panel {
  width: 340px;
  height: 480px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.45);
}

.panel-header {
  padding: 14px 16px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
  background: rgba(200,160,100,0.05);
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--cream);
}

.panel-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4ade80;
  box-shadow: 0 0 6px rgba(74,222,128,0.6);
}

.ctx-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: space-between;
  margin-top: 8px;
  padding: 7px 10px;
  background: rgba(200,160,100,0.1);
  border-radius: 6px;
  font-size: 0.76rem;
  color: var(--gold);
}

.ctx-clear {
  background: none;
  border: none;
  color: var(--cream-muted);
  cursor: pointer;
  padding: 0;
  font-size: 1rem;
  line-height: 1;
}

.panel-messages {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  scrollbar-width: thin;
  scrollbar-color: rgba(200,160,100,0.2) transparent;
}

.msg {
  max-width: 86%;
  border-radius: 12px;
  padding: 10px 13px;
  font-size: 0.85rem;
  line-height: 1.65;
}

.msg :deep(.markdown-body) {
  display: grid;
  gap: 7px;
}

.msg :deep(.markdown-body > *:first-child) { margin-top: 0; }
.msg :deep(.markdown-body > *:last-child) { margin-bottom: 0; }
.msg :deep(p) { margin: 0; }
.msg :deep(ul),
.msg :deep(ol) {
  display: grid;
  gap: 3px;
  margin: 0;
  padding-left: 17px;
}
.msg :deep(h3),
.msg :deep(h4),
.msg :deep(h5) {
  margin: 2px 0 0;
  color: var(--cream);
  font-size: 0.9rem;
}
.msg :deep(code) {
  border: 1px solid var(--border);
  border-radius: 5px;
  background: rgba(255,255,255,0.05);
  padding: 1px 5px;
  color: var(--gold-light);
  font-family: Consolas, 'Courier New', monospace;
  font-size: 0.84em;
}
.msg :deep(pre) {
  margin: 0;
  overflow-x: auto;
}
.msg :deep(pre code) {
  display: block;
  padding: 9px;
  white-space: pre;
}
.msg :deep(a) {
  color: var(--gold-light);
  font-weight: 700;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.msg-user {
  align-self: flex-end;
  background: rgba(200,160,100,0.15);
  border: 1px solid rgba(200,160,100,0.2);
  color: var(--cream);
}

.msg-assistant {
  align-self: flex-start;
  background: rgba(255,255,255,0.04);
  border: 1px solid var(--border);
  color: var(--cream);
}

.typing {
  display: flex;
  gap: 4px;
  align-items: center;
  height: 20px;
}

.typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--cream-muted);
  animation: bounce 1.2s infinite;
}

.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 80%, 100% { transform: translateY(0); opacity: 0.5; }
  40% { transform: translateY(-6px); opacity: 1; }
}

.panel-form {
  display: flex;
  gap: 8px;
  padding: 12px 14px;
  border-top: 1px solid var(--border);
  flex-shrink: 0;
}

.panel-input {
  flex: 1;
  padding: 9px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: rgba(255,255,255,0.04);
  color: var(--cream);
  font-size: 0.84rem;
  transition: border-color 0.2s;
  width: 100%;
}
.panel-input:focus { border-color: var(--border-strong); outline: none; }
.panel-input::placeholder { color: rgba(154,128,96,0.6); font-size: 0.8rem; }
.panel-input:disabled { opacity: 0.5; }

.panel-send {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--gold);
  color: #1a1208;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.2s;
}
.panel-send:hover:not(:disabled) { background: var(--gold-light); }
.panel-send:disabled { opacity: 0.45; cursor: not-allowed; }

/* Transition */
.panel-enter-active, .panel-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}
.panel-enter-from, .panel-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.97);
}
</style>
