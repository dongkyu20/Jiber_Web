<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { chatApi } from '@/api/chat'
import type { ChatContext } from '@/api/types'
import { useChatContextStore } from '@/stores/chatContext'
import { formatKrw } from '@/utils/format'

interface ChatMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  contexts?: ChatContext[]
}

interface SourceSummary {
  source: string
  label: string
}

const messages = ref<ChatMessage[]>([
  {
    id: 0,
    role: 'assistant',
    content: '안녕하세요! 부동산 AI 챗봇입니다.\n전세 계약, 실거래, 가격 예측과 XAI 설명 등 궁금한 점을 물어보세요.'
  }
])
const question = ref('')
const loading = ref(false)
const errorMessage = ref('')
const messagesEl = ref<HTMLElement | null>(null)
let idCtr = 1

const route = useRoute()
const chatContextStore = useChatContextStore()

const canSubmit = computed(() => question.value.trim().length > 0 && !loading.value)
const activeContext = computed(() => chatContextStore.runtimeContext)
const estimatedPriceText = computed(() => formatKrw(activeContext.value?.valuation?.estimatedPrice))

function renderMessage(content: string): string {
  return content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*([^*\n]+)\*\*/g, '<strong>$1</strong>')
}

function sourceLabel(source: string): string {
  const normalizedSource = source.split('#')[0]
  const fileName = normalizedSource.split('/').pop() ?? normalizedSource
  return fileName.replace(/\.[^.]+$/, '')
}

function sourceKey(source: string): string {
  return source.split('#')[0]
}

function sourceSummaries(contexts?: ChatContext[]): SourceSummary[] {
  if (!contexts?.length) return []
  const seen = new Set<string>()
  return contexts
    .filter((ctx) => {
      const key = sourceKey(ctx.source)
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
    .map((ctx) => ({ source: sourceKey(ctx.source), label: sourceLabel(ctx.source) }))
}

async function scrollToBottom() {
  await nextTick()
  if (messagesEl.value) messagesEl.value.scrollTop = messagesEl.value.scrollHeight
}

async function submitQuestion() {
  const trimmed = question.value.trim()
  if (!trimmed || loading.value) return

  messages.value.push({ id: idCtr++, role: 'user', content: trimmed })
  question.value = ''
  loading.value = true
  errorMessage.value = ''
  await scrollToBottom()

  try {
    const response = await chatApi.askRealEstate({
      question: trimmed,
      runtimeContext: activeContext.value ?? undefined
    })
    messages.value.push({
      id: idCtr++,
      role: 'assistant',
      content: response.answer,
      contexts: response.contexts
    })
  } catch {
    errorMessage.value = '챗봇 답변을 불러오지 못했습니다. 백엔드 연결을 확인해 주세요.'
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

onMounted(() => {
  const queryQuestion = typeof route.query.q === 'string' ? route.query.q.trim() : ''
  if (queryQuestion) {
    question.value = queryQuestion
    void submitQuestion()
  }
})
</script>

<template>
  <div class="chat-page">
    <div class="chat-header">
      <p class="eyebrow">AI 챗봇</p>
      <h1 class="chat-title">부동산 AI 어시스턴트</h1>
      <p class="chat-desc">전세 계약, 실거래, 가격예측과 XAI 설명에 관한 질문을 남겨보세요.</p>
    </div>

    <div class="chat-shell">
      <!-- Context bar -->
      <div v-if="activeContext" class="ctx-bar">
        <div class="ctx-info">
          <span class="ctx-dot" />
          <div>
            <p class="ctx-name">{{ activeContext.property.name }}</p>
            <p class="ctx-sub">AI 추정가 {{ estimatedPriceText }}</p>
          </div>
        </div>
        <button class="ctx-clear" type="button" @click="chatContextStore.clearRuntimeContext()">컨텍스트 해제</button>
      </div>

      <!-- Disclaimer -->
      <div class="chat-notice">
        챗봇 답변은 참고용 정보입니다. 실제 계약, 매수·매도, 법률·세무 판단은 전문가와 공식 자료를 함께 확인하세요.
      </div>

      <!-- Messages -->
      <div class="chat-messages" ref="messagesEl" aria-live="polite">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="msg"
          :class="`msg-${msg.role}`"
        >
          <p v-html="renderMessage(msg.content)" />
          <details v-if="msg.contexts?.length" class="msg-sources">
            <summary>참고 문서 {{ sourceSummaries(msg.contexts).length }}개</summary>
            <ol>
              <li v-for="src in sourceSummaries(msg.contexts)" :key="`${msg.id}-${src.source}`">
                {{ src.label }}
              </li>
            </ol>
          </details>
        </div>
        <div v-if="loading" class="msg msg-assistant">
          <span class="typing"><span /><span /><span /></span>
        </div>
        <p v-if="errorMessage" class="chat-error">{{ errorMessage }}</p>
      </div>

      <!-- Input -->
      <form class="chat-form" @submit.prevent="submitQuestion">
        <textarea
          v-model="question"
          class="chat-input"
          rows="3"
          placeholder="예: 전세 계약 전에 확인해야 할 핵심 사항을 알려줘"
          @keydown.enter.exact.prevent="submitQuestion"
        ></textarea>
        <button class="chat-send" type="submit" :disabled="!canSubmit">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="none" viewBox="0 0 24 24">
            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19V5M5 12l7-7 7 7"/>
          </svg>
          질문하기
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.chat-page {
  max-width: 780px;
  margin: 0 auto;
}

.chat-header { margin-bottom: 24px; }

.chat-title {
  margin: 4px 0 6px;
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--cream);
}

.chat-desc {
  margin: 0;
  font-size: 0.88rem;
  color: var(--cream-muted);
}

.chat-shell {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* Context bar */
.ctx-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(200, 160, 100, 0.08);
  border: 1px solid rgba(200, 160, 100, 0.2);
  border-radius: 10px;
}

.ctx-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ctx-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4ade80;
  box-shadow: 0 0 6px rgba(74, 222, 128, 0.6);
  flex-shrink: 0;
}

.ctx-name {
  margin: 0 0 2px;
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--cream);
}

.ctx-sub {
  margin: 0;
  font-size: 0.78rem;
  color: var(--gold);
}

.ctx-clear {
  padding: 6px 12px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 6px;
  color: var(--cream-muted);
  font-size: 0.8rem;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
}
.ctx-clear:hover { border-color: var(--border-strong); color: var(--cream); }

/* Disclaimer */
.chat-notice {
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 0.78rem;
  color: var(--cream-muted);
  line-height: 1.5;
}

/* Messages */
.chat-messages {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 20px;
  min-height: 320px;
  max-height: 480px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  scrollbar-width: thin;
  scrollbar-color: rgba(200, 160, 100, 0.2) transparent;
}

.msg {
  max-width: 80%;
  border-radius: 12px;
  padding: 12px 15px;
  font-size: 0.88rem;
  line-height: 1.65;
}

.msg p { margin: 0; white-space: pre-wrap; }

.msg-user {
  align-self: flex-end;
  background: rgba(200, 160, 100, 0.15);
  border: 1px solid rgba(200, 160, 100, 0.2);
  color: var(--cream);
}

.msg-assistant {
  align-self: flex-start;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border);
  color: var(--cream);
}

.msg-sources {
  margin-top: 8px;
  font-size: 0.76rem;
  color: var(--cream-muted);
}

.msg-sources summary {
  cursor: pointer;
  color: var(--gold);
  font-weight: 600;
}

.msg-sources ol {
  margin: 6px 0 0 16px;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
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

.chat-error {
  margin: 0;
  font-size: 0.84rem;
  color: #e87a7a;
}

/* Form */
.chat-form {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.chat-input {
  flex: 1;
  padding: 12px 14px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  color: var(--cream);
  font-size: 0.9rem;
  font-family: inherit;
  resize: none;
  transition: border-color 0.2s;
}
.chat-input:focus { outline: none; border-color: var(--gold); }
.chat-input::placeholder { color: rgba(154, 128, 96, 0.5); }

.chat-send {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 12px 20px;
  background: var(--gold);
  color: #1a1208;
  border: none;
  border-radius: 10px;
  font-size: 0.9rem;
  font-weight: 700;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s;
  flex-shrink: 0;
}
.chat-send:hover:not(:disabled) { background: var(--gold-light); }
.chat-send:disabled { opacity: 0.45; cursor: not-allowed; }
</style>
