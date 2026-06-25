<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'

import brandLogoUrl from '@/assets/brand/jiper-estate-real-logo-cropped.png'
import { authApi } from '@/api/auth'
import { getApiErrorMessage } from '@/api/client'

type RecoveryTab = 'identifier' | 'password'

const directPasswordResetSafeMessage =
  '입력한 정보가 가입 정보와 일치하면 비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.'

const activeTab = ref<RecoveryTab>('identifier')
const displayName = ref('')
const email = ref('')
const passwordDisplayName = ref('')
const newPassword = ref('')
const newPasswordConfirm = ref('')
const identifierMessage = ref('')
const passwordMessage = ref('')
const errorMessage = ref('')
const loading = ref(false)

async function submitIdentifierRecovery() {
  identifierMessage.value = ''
  errorMessage.value = ''

  if (!displayName.value.trim()) {
    errorMessage.value = '이름을 입력해 주세요.'
    return
  }

  loading.value = true
  try {
    const response = await authApi.recoverIdentifier({ displayName: displayName.value.trim() })
    identifierMessage.value = response.message
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, '아이디 찾기 요청을 처리하지 못했습니다.')
  } finally {
    loading.value = false
  }
}

async function submitPasswordRecovery() {
  passwordMessage.value = ''
  errorMessage.value = ''

  if (!email.value.trim()) {
    errorMessage.value = '가입 이메일을 입력해 주세요.'
    return
  }
  if (!passwordDisplayName.value.trim()) {
    errorMessage.value = '가입 시 입력한 이름을 입력해 주세요.'
    return
  }
  if (newPassword.value.length < 8) {
    errorMessage.value = '새 비밀번호는 8자 이상이어야 합니다.'
    return
  }
  if (newPassword.value !== newPasswordConfirm.value) {
    errorMessage.value = '새 비밀번호가 일치하지 않습니다.'
    return
  }

  loading.value = true
  try {
    const response = await authApi.directPasswordReset({
      email: email.value.trim(),
      displayName: passwordDisplayName.value.trim(),
      newPassword: newPassword.value
    })
    passwordMessage.value = response.message
  } catch (error) {
    passwordMessage.value = getApiErrorMessage(error, directPasswordResetSafeMessage)
  } finally {
    loading.value = false
  }
}

function selectTab(tab: RecoveryTab) {
  activeTab.value = tab
  errorMessage.value = ''
}
</script>

<template>
  <main class="recovery-page">
    <section class="recovery-card" aria-labelledby="recovery-title">
      <RouterLink class="brand-link" to="/" aria-label="집er 홈으로 이동">
        <img class="brand-logo" :src="brandLogoUrl" alt="집er estate real" />
      </RouterLink>

      <div class="recovery-head">
        <p class="eyebrow">계정 찾기</p>
        <h1 id="recovery-title">아이디 · 비밀번호 찾기</h1>
        <p>집er 아이디는 가입할 때 사용한 이메일 주소입니다.</p>
      </div>

      <div class="tab-list" role="tablist" aria-label="계정 찾기 유형">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'identifier' }"
          type="button"
          role="tab"
          :aria-selected="activeTab === 'identifier'"
          @click="selectTab('identifier')"
        >
          아이디 찾기
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'password' }"
          type="button"
          role="tab"
          :aria-selected="activeTab === 'password'"
          data-test="password-tab"
          @click="selectTab('password')"
        >
          비밀번호 찾기
        </button>
      </div>

      <form
        v-if="activeTab === 'identifier'"
        class="recovery-form"
        data-test="identifier-recovery-form"
        @submit.prevent="submitIdentifierRecovery"
      >
        <div class="form-field">
          <label for="recovery-display-name">이름</label>
          <input
            id="recovery-display-name"
            v-model="displayName"
            maxlength="100"
            type="text"
            autocomplete="name"
            placeholder="가입 시 입력한 이름"
          />
        </div>
        <button class="primary-btn" type="submit" :disabled="loading">
          {{ loading ? '확인 중...' : '아이디 확인' }}
        </button>
        <p v-if="identifierMessage" class="success-message">{{ identifierMessage }}</p>
      </form>

      <form
        v-else
        class="recovery-form"
        data-test="password-recovery-form"
        @submit.prevent="submitPasswordRecovery"
      >
        <div class="form-field">
          <label for="recovery-email">가입 이메일</label>
          <input
            id="recovery-email"
            v-model="email"
            maxlength="320"
            type="email"
            autocomplete="email"
            placeholder="user@example.com"
          />
        </div>
        <div class="form-field">
          <label for="recovery-password-display-name">이름</label>
          <input
            id="recovery-password-display-name"
            v-model="passwordDisplayName"
            maxlength="100"
            type="text"
            autocomplete="name"
            placeholder="가입 시 입력한 이름"
          />
        </div>
        <div class="form-field">
          <label for="recovery-new-password">새 비밀번호</label>
          <input
            id="recovery-new-password"
            v-model="newPassword"
            type="password"
            autocomplete="new-password"
            placeholder="8자 이상"
          />
        </div>
        <div class="form-field">
          <label for="recovery-new-password-confirm">새 비밀번호 확인</label>
          <input
            id="recovery-new-password-confirm"
            v-model="newPasswordConfirm"
            type="password"
            autocomplete="new-password"
            placeholder="한 번 더 입력"
          />
        </div>
        <button class="primary-btn" type="submit" :disabled="loading">
          {{ loading ? '변경 중...' : '비밀번호 변경' }}
        </button>
        <p v-if="passwordMessage" class="success-message">{{ passwordMessage }}</p>
      </form>

      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

      <div class="recovery-actions">
        <RouterLink to="/login">로그인으로 돌아가기</RouterLink>
        <RouterLink to="/signup">회원가입</RouterLink>
      </div>
    </section>
  </main>
</template>

<style scoped>
.recovery-page {
  min-height: calc(100vh - 72px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 18px;
}

.recovery-card {
  width: min(100%, 460px);
  border: 1px solid rgba(100, 80, 50, 0.18);
  border-radius: 8px;
  background: var(--cream-card);
  color: #1a1208;
  padding: 36px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.34);
}

.brand-link {
  display: flex;
  justify-content: center;
  margin-bottom: 22px;
}

.brand-logo {
  width: 128px;
  height: auto;
  object-fit: contain;
}

.recovery-head {
  text-align: center;
}

.eyebrow {
  margin: 0 0 8px;
  color: #8a7255;
  font-size: 0.78rem;
  font-weight: 800;
}

.recovery-head h1 {
  margin: 0 0 8px;
  font-size: 1.45rem;
  color: #1a1208;
}

.recovery-head p {
  margin: 0;
  color: #6a5035;
  font-size: 0.88rem;
}

.tab-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  margin: 26px 0 20px;
  padding: 4px;
  border: 1px solid rgba(100, 80, 50, 0.18);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.45);
}

.tab-btn {
  min-height: 42px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #6a5035;
  font-weight: 800;
  cursor: pointer;
}

.tab-btn.active {
  background: #1a1208;
  color: #f0e6d0;
}

.recovery-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.form-field label {
  color: #4a3520;
  font-size: 0.82rem;
  font-weight: 800;
}

.form-field input {
  width: 100%;
  border: 1px solid rgba(100, 80, 50, 0.25);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.75);
  color: #1a1208;
  padding: 12px 14px;
  font-size: 0.92rem;
}

.form-field input:focus {
  outline: none;
  border-color: rgba(100, 80, 50, 0.55);
  background: #fff;
}

.primary-btn {
  width: 100%;
  min-height: 46px;
  border: 0;
  border-radius: 8px;
  background: #1a1208;
  color: #f0e6d0;
  font-weight: 800;
  cursor: pointer;
}

.primary-btn:disabled {
  cursor: wait;
  opacity: 0.65;
}

.success-message,
.error-message {
  margin: 0;
  padding: 11px 12px;
  border-radius: 8px;
  font-size: 0.86rem;
  line-height: 1.55;
}

.success-message {
  border: 1px solid rgba(38, 121, 78, 0.22);
  background: rgba(38, 121, 78, 0.08);
  color: #23623f;
}

.error-message {
  margin-top: 14px;
  border: 1px solid rgba(220, 80, 80, 0.25);
  background: rgba(220, 80, 80, 0.09);
  color: #b53838;
}

.recovery-actions {
  display: flex;
  justify-content: center;
  gap: 18px;
  margin-top: 22px;
  font-size: 0.84rem;
}

.recovery-actions a {
  color: #6a5035;
  font-weight: 800;
  text-decoration: underline;
  text-underline-offset: 3px;
}

@media (max-width: 540px) {
  .recovery-page {
    align-items: stretch;
    padding: 24px 14px;
  }

  .recovery-card {
    padding: 28px 20px;
  }
}
</style>
