<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { authApi } from '@/api/auth'
import type { PendingSocialSignupResponse } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import {
  authErrorMessage,
  providerLabel,
  validateDisplayName,
  validateEmail,
  validatePassword
} from './authHelpers'

const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUiStore()

const pending = ref<PendingSocialSignupResponse | null>(null)
const loadingPending = ref(true)
const pendingErrorMessage = ref('')
const signupEmail = ref('')
const signupDisplayName = ref('')
const signupPassword = ref('')
const signupErrors = ref<string[]>([])
const signupMessage = ref('')
const linkEmail = ref('')
const linkPassword = ref('')
const linkMessage = ref('')
const showLinkGuidance = ref(false)

const pendingProviderLabel = computed(() => (pending.value ? providerLabel(pending.value.provider) : '소셜'))
const shouldPrioritizeLink = computed(() => Boolean(pending.value?.matchingEmailAccountExists || showLinkGuidance.value))

function syncPendingForm(response: PendingSocialSignupResponse) {
  signupEmail.value = response.email ?? ''
  signupDisplayName.value = response.displayName ?? ''
  linkEmail.value = response.email ?? ''
  showLinkGuidance.value = response.matchingEmailAccountExists
}

async function fetchPendingSocialSignup() {
  loadingPending.value = true
  pendingErrorMessage.value = ''
  try {
    const response = await authApi.getPendingSocialSignup()
    pending.value = response
    syncPendingForm(response)
  } catch (error) {
    pending.value = null
    pendingErrorMessage.value = authErrorMessage(error, '소셜 가입 정보가 만료되었거나 찾을 수 없습니다.')
  } finally {
    loadingPending.value = false
  }
}

function validateSignupForm(): string[] {
  return [
    validateEmail(signupEmail.value),
    validateDisplayName(signupDisplayName.value),
    validatePassword(signupPassword.value)
  ].filter((message): message is string => Boolean(message))
}

async function submitSocialSignup() {
  signupMessage.value = ''
  signupErrors.value = validateSignupForm()
  if (signupErrors.value.length) return

  try {
    await authStore.completeSocialSignup({
      email: signupEmail.value.trim(),
      displayName: signupDisplayName.value.trim(),
      password: signupPassword.value
    })
    await router.push('/map')
  } catch (error) {
    const message = authErrorMessage(error, '소셜 회원가입을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요.')
    signupMessage.value =
      message === '이미 가입된 이메일입니다.' ? `${message} 기존 계정 비밀번호로 연결해 주세요.` : message
    if (message === '이미 가입된 이메일입니다.') {
      showLinkGuidance.value = true
      linkEmail.value = signupEmail.value
    }
  }
}

async function submitSocialLink() {
  linkMessage.value = ''
  const emailError = validateEmail(linkEmail.value)
  if (emailError || !linkPassword.value) {
    linkMessage.value = emailError ?? '기존 계정 비밀번호를 입력해 주세요.'
    return
  }
  try {
    await authStore.linkPendingSocialAccount({
      email: linkEmail.value.trim(),
      password: linkPassword.value
    })
    await router.push('/map')
  } catch (error) {
    linkMessage.value = authErrorMessage(error, '소셜 계정을 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.')
  }
}

onMounted(fetchPendingSocialSignup)
</script>

<template>
  <div class="social-page">
    <div class="social-header">
      <p class="eyebrow">소셜 회원가입</p>
      <h1 class="social-title">소셜 계정 연결</h1>
      <p class="social-desc">기존 계정이 있다면 이메일과 비밀번호 확인 후에만 연결할 수 있습니다.</p>
    </div>

    <p v-if="loadingPending" class="loading-text">소셜 가입 정보를 확인하고 있습니다...</p>

    <template v-else-if="pending">
      <!-- Provider preview -->
      <div class="provider-badge">
        <span class="provider-tag">{{ pendingProviderLabel }}</span>
        <div>
          <p class="provider-email">{{ pending.email ?? '이메일 정보 없음' }}</p>
          <p class="provider-name">{{ pending.displayName ?? '이름 정보 없음' }}</p>
        </div>
      </div>
      <p v-if="pending.matchingEmailAccountExists" class="match-warn">
        이 이메일로 가입된 계정이 있습니다. 자동으로 연결되지 않으며, 기존 계정 비밀번호를 입력해야 연결할 수 있습니다.
      </p>

      <div class="social-grid">
        <!-- Link existing account -->
        <article v-if="shouldPrioritizeLink" class="social-panel">
          <h2 class="panel-heading">기존 계정에 연결</h2>
          <p class="panel-desc">Jiber 계정의 이메일과 비밀번호를 입력하면 이 소셜 계정을 연결합니다.</p>
          <form class="social-form" data-test="social-link-form" @submit.prevent="submitSocialLink">
            <div class="form-group">
              <label class="form-label">이메일</label>
              <input
                v-model="linkEmail"
                class="form-input"
                data-test="social-link-email"
                autocomplete="email"
                inputmode="email"
                type="email"
                placeholder="you@example.com"
              />
            </div>
            <div class="form-group">
              <label class="form-label">기존 계정 비밀번호</label>
              <input
                v-model="linkPassword"
                class="form-input"
                data-test="social-link-password"
                autocomplete="current-password"
                type="password"
                placeholder="비밀번호"
              />
            </div>
            <p v-if="linkMessage" class="form-error">{{ linkMessage }}</p>
            <button class="primary-btn" type="submit" :disabled="authStore.loading">
              {{ authStore.loading ? '연결 중...' : '기존 계정에 연결' }}
            </button>
          </form>
        </article>

        <!-- New signup -->
        <article class="social-panel">
          <h2 class="panel-heading">소셜 계정으로 새 회원가입</h2>
          <p v-if="shouldPrioritizeLink" class="panel-desc">새 계정으로 가입하려면 기존 계정과 다른 이메일을 사용해 주세요.</p>
          <form class="social-form" data-test="social-signup-form" @submit.prevent="submitSocialSignup">
            <div class="form-group">
              <label class="form-label">이메일</label>
              <input
                v-model="signupEmail"
                class="form-input"
                data-test="social-signup-email"
                autocomplete="email"
                inputmode="email"
                type="email"
                placeholder="you@example.com"
              />
            </div>
            <div class="form-group">
              <label class="form-label">닉네임</label>
              <input
                v-model="signupDisplayName"
                class="form-input"
                data-test="social-signup-display-name"
                autocomplete="name"
                type="text"
                placeholder="서비스에서 사용할 이름"
              />
            </div>
            <div class="form-group">
              <label class="form-label">비밀번호</label>
              <input
                v-model="signupPassword"
                class="form-input"
                data-test="social-signup-password"
                autocomplete="new-password"
                minlength="8"
                type="password"
                placeholder="8자 이상"
              />
            </div>
            <div v-if="signupErrors.length" class="form-errors">
              <p v-for="msg in signupErrors" :key="msg" class="form-error">{{ msg }}</p>
            </div>
            <p v-if="signupMessage" class="form-error">{{ signupMessage }}</p>
            <button class="secondary-btn" type="submit" :disabled="authStore.loading">
              {{ authStore.loading ? '가입 중...' : '새 계정으로 가입' }}
            </button>
          </form>
        </article>

        <!-- Link account (lower priority) -->
        <article v-if="!shouldPrioritizeLink" class="social-panel">
          <h2 class="panel-heading">기존 계정에 연결</h2>
          <p class="panel-desc">이미 Jiber 계정이 있다면 이메일과 비밀번호 확인 후 연결할 수 있습니다.</p>
          <form class="social-form" data-test="social-link-form" @submit.prevent="submitSocialLink">
            <div class="form-group">
              <label class="form-label">이메일</label>
              <input
                v-model="linkEmail"
                class="form-input"
                data-test="social-link-email"
                autocomplete="email"
                inputmode="email"
                type="email"
                placeholder="you@example.com"
              />
            </div>
            <div class="form-group">
              <label class="form-label">기존 계정 비밀번호</label>
              <input
                v-model="linkPassword"
                class="form-input"
                data-test="social-link-password"
                autocomplete="current-password"
                type="password"
                placeholder="비밀번호"
              />
            </div>
            <p v-if="linkMessage" class="form-error">{{ linkMessage }}</p>
            <button class="primary-btn" type="submit" :disabled="authStore.loading">
              {{ authStore.loading ? '연결 중...' : '기존 계정에 연결' }}
            </button>
          </form>
        </article>
      </div>
    </template>

    <!-- Error / expired state -->
    <div v-else class="expired-panel">
      <p class="form-error">{{ pendingErrorMessage }}</p>
      <p class="expired-desc">브라우저에 저장된 소셜 가입 세션을 확인하지 못했습니다. 다시 시작해 주세요.</p>
      <div class="expired-actions">
        <button class="primary-btn" type="button" @click="uiStore.openLogin()">로그인으로 돌아가기</button>
        <button class="secondary-btn" type="button" @click="uiStore.openSignup()">회원가입으로 돌아가기</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.social-page {
  max-width: 900px;
  margin: 0 auto;
}

.social-header { margin-bottom: 24px; }

.social-title {
  margin: 4px 0 6px;
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--cream);
}

.social-desc {
  margin: 0;
  font-size: 0.88rem;
  color: var(--cream-muted);
}

.provider-badge {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  background: rgba(200, 160, 100, 0.07);
  border: 1px solid rgba(200, 160, 100, 0.2);
  border-radius: 10px;
  margin-bottom: 12px;
}

.provider-tag {
  padding: 4px 10px;
  background: rgba(200, 160, 100, 0.15);
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--gold);
  white-space: nowrap;
}

.provider-email {
  margin: 0 0 2px;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--cream);
}

.provider-name {
  margin: 0;
  font-size: 0.82rem;
  color: var(--cream-muted);
}

.match-warn {
  padding: 10px 14px;
  background: rgba(232, 160, 80, 0.08);
  border: 1px solid rgba(232, 160, 80, 0.2);
  border-radius: 8px;
  font-size: 0.84rem;
  color: #e8a050;
  margin-bottom: 20px;
}

.social-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
}

.social-panel {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 24px;
}

.panel-heading {
  margin: 0 0 8px;
  font-size: 1rem;
  font-weight: 700;
  color: var(--cream);
}

.panel-desc {
  margin: 0 0 18px;
  font-size: 0.82rem;
  color: var(--cream-muted);
  line-height: 1.5;
}

.social-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--cream-muted);
}

.form-input {
  padding: 10px 13px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--cream);
  font-size: 0.9rem;
  font-family: inherit;
  transition: border-color 0.2s;
}
.form-input:focus { outline: none; border-color: var(--gold); }
.form-input::placeholder { color: rgba(154, 128, 96, 0.5); }

.form-errors { display: flex; flex-direction: column; gap: 4px; }

.form-error {
  margin: 0;
  font-size: 0.82rem;
  color: #e87a7a;
}

.primary-btn {
  padding: 12px 20px;
  background: var(--gold);
  color: #1a1208;
  border: none;
  border-radius: 9px;
  font-size: 0.9rem;
  font-weight: 700;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.2s;
  width: 100%;
}
.primary-btn:hover:not(:disabled) { background: var(--gold-light); }
.primary-btn:disabled { opacity: 0.45; cursor: not-allowed; }

.secondary-btn {
  padding: 12px 20px;
  background: transparent;
  color: var(--cream);
  border: 1px solid var(--border-strong);
  border-radius: 9px;
  font-size: 0.9rem;
  font-weight: 700;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
  width: 100%;
}
.secondary-btn:hover:not(:disabled) { border-color: var(--gold); color: var(--gold); }
.secondary-btn:disabled { opacity: 0.45; cursor: not-allowed; }

.expired-panel {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 32px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.expired-desc {
  margin: 0;
  font-size: 0.86rem;
  color: var(--cream-muted);
}

.expired-actions {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}

.expired-actions .primary-btn,
.expired-actions .secondary-btn {
  width: auto;
  padding: 10px 20px;
}
</style>
