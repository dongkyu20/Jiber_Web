<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { getApiErrorMessage } from '@/api/client'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const displayName = ref(authStore.user?.displayName ?? '')
const currentPassword = ref('')
const newPassword = ref('')
const newPasswordConfirm = ref('')
const withdrawPassword = ref('')
const withdrawConfirmed = ref(false)

const profileSaving = ref(false)
const passwordSaving = ref(false)
const withdrawSaving = ref(false)
const profileStatus = ref('')
const profileError = ref('')
const passwordError = ref('')
const withdrawError = ref('')

const accountEmail = computed(() => authStore.user?.email ?? '')

watch(
  () => authStore.user?.displayName,
  (nextDisplayName) => {
    if (nextDisplayName) {
      displayName.value = nextDisplayName
    }
  }
)

async function submitProfile() {
  profileStatus.value = ''
  profileError.value = ''
  const trimmedDisplayName = displayName.value.trim()

  if (!trimmedDisplayName) {
    profileError.value = '닉네임을 입력해 주세요.'
    return
  }

  profileSaving.value = true
  try {
    await authStore.updateProfile({ displayName: trimmedDisplayName })
    displayName.value = trimmedDisplayName
    profileStatus.value = '닉네임이 변경되었습니다.'
  } catch (error) {
    profileError.value = authStore.errorMessage ?? getApiErrorMessage(error, '닉네임을 변경하지 못했습니다.')
  } finally {
    profileSaving.value = false
  }
}

async function submitPassword() {
  passwordError.value = ''

  if (!currentPassword.value || !newPassword.value || !newPasswordConfirm.value) {
    passwordError.value = '모든 비밀번호 항목을 입력해 주세요.'
    return
  }

  if (newPassword.value.length < 8) {
    passwordError.value = '새 비밀번호는 8자 이상이어야 합니다.'
    return
  }

  if (newPassword.value !== newPasswordConfirm.value) {
    passwordError.value = '새 비밀번호가 일치하지 않습니다.'
    return
  }

  passwordSaving.value = true
  try {
    await authStore.changePassword({
      currentPassword: currentPassword.value,
      newPassword: newPassword.value
    })
    currentPassword.value = ''
    newPassword.value = ''
    newPasswordConfirm.value = ''
    await router.push('/login')
  } catch (error) {
    passwordError.value = authStore.errorMessage ?? getApiErrorMessage(error, '비밀번호를 변경하지 못했습니다.')
  } finally {
    passwordSaving.value = false
  }
}

async function submitWithdraw() {
  withdrawError.value = ''

  if (!withdrawConfirmed.value) {
    withdrawError.value = '회원탈퇴 확인에 동의해 주세요.'
    return
  }

  if (!withdrawPassword.value) {
    withdrawError.value = '현재 비밀번호를 입력해 주세요.'
    return
  }

  withdrawSaving.value = true
  try {
    await authStore.deactivateAccount({ password: withdrawPassword.value })
    withdrawPassword.value = ''
    withdrawConfirmed.value = false
    await router.push('/')
  } catch (error) {
    withdrawError.value = authStore.errorMessage ?? getApiErrorMessage(error, '회원탈퇴를 처리하지 못했습니다.')
  } finally {
    withdrawSaving.value = false
  }
}
</script>

<template>
  <div class="mypage-page">
    <section class="mypage-header">
      <div>
        <p class="eyebrow">계정</p>
        <h1 class="mypage-title">마이페이지</h1>
      </div>
      <div class="summary-strip" aria-label="현재 계정">
        <span>{{ accountEmail }}</span>
      </div>
    </section>

    <div class="mypage-grid">
      <section class="mypage-panel account-panel">
        <div class="panel-head">
          <h2 class="panel-heading">내 정보</h2>
        </div>

        <dl class="account-list">
          <div>
            <dt>이메일</dt>
            <dd>{{ accountEmail }}</dd>
          </div>
          <div>
            <dt>닉네임</dt>
            <dd>{{ authStore.user?.displayName }}</dd>
          </div>
        </dl>
      </section>

      <section class="mypage-panel">
        <div class="panel-head">
          <h2 class="panel-heading">닉네임 변경</h2>
        </div>

        <form class="mypage-form" data-test="profile-form" @submit.prevent="submitProfile">
          <div class="form-group">
            <label class="form-label" for="mypage-display-name">닉네임</label>
            <input
              id="mypage-display-name"
              v-model="displayName"
              class="form-input"
              maxlength="100"
              required
              type="text"
            />
          </div>

          <button class="submit-btn" type="submit" :disabled="profileSaving">
            {{ profileSaving ? '저장 중...' : '닉네임 저장' }}
          </button>
          <p v-if="profileStatus" class="status-ok">{{ profileStatus }}</p>
          <p v-if="profileError" class="status-error">{{ profileError }}</p>
        </form>
      </section>

      <section class="mypage-panel">
        <div class="panel-head">
          <h2 class="panel-heading">비밀번호 변경</h2>
        </div>

        <form class="mypage-form" data-test="password-form" @submit.prevent="submitPassword">
          <div class="form-group">
            <label class="form-label" for="mypage-current-password">현재 비밀번호</label>
            <input
              id="mypage-current-password"
              v-model="currentPassword"
              class="form-input"
              autocomplete="current-password"
              type="password"
            />
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label" for="mypage-new-password">새 비밀번호</label>
              <input
                id="mypage-new-password"
                v-model="newPassword"
                class="form-input"
                autocomplete="new-password"
                minlength="8"
                type="password"
              />
            </div>

            <div class="form-group">
              <label class="form-label" for="mypage-new-password-confirm">새 비밀번호 확인</label>
              <input
                id="mypage-new-password-confirm"
                v-model="newPasswordConfirm"
                class="form-input"
                autocomplete="new-password"
                minlength="8"
                type="password"
              />
            </div>
          </div>

          <button class="submit-btn" type="submit" :disabled="passwordSaving">
            {{ passwordSaving ? '변경 중...' : '비밀번호 변경' }}
          </button>
          <p v-if="passwordError" class="status-error">{{ passwordError }}</p>
        </form>
      </section>

      <section class="mypage-panel danger-panel">
        <div class="panel-head">
          <h2 class="panel-heading">회원탈퇴</h2>
        </div>

        <form class="mypage-form" data-test="withdraw-form" @submit.prevent="submitWithdraw">
          <div class="form-group">
            <label class="form-label" for="mypage-withdraw-password">현재 비밀번호</label>
            <input
              id="mypage-withdraw-password"
              v-model="withdrawPassword"
              class="form-input"
              autocomplete="current-password"
              type="password"
            />
          </div>

          <label class="check-row" for="mypage-withdraw-confirm">
            <input id="mypage-withdraw-confirm" v-model="withdrawConfirmed" type="checkbox" />
            <span>계정을 비활성화하고 로그아웃합니다.</span>
          </label>

          <button class="danger-btn" type="submit" :disabled="withdrawSaving">
            {{ withdrawSaving ? '처리 중...' : '회원탈퇴' }}
          </button>
          <p v-if="withdrawError" class="status-error">{{ withdrawError }}</p>
        </form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.mypage-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.mypage-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.mypage-title {
  margin: 4px 0 0;
  color: var(--cream);
  font-size: 1.75rem;
  font-weight: 700;
}

.summary-strip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--cream-muted);
  font-size: 0.86rem;
}

.mypage-grid {
  display: grid;
  grid-template-columns: minmax(260px, 0.8fr) minmax(0, 1.2fr);
  gap: 20px;
  align-items: start;
}

.mypage-panel {
  min-width: 0;
  padding: 22px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
}

.account-panel {
  grid-row: span 3;
}

.panel-head {
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
}

.panel-heading {
  margin: 0;
  color: var(--cream);
  font-size: 1rem;
  font-weight: 700;
}

.account-list {
  display: grid;
  gap: 14px;
  margin: 0;
}

.account-list div {
  display: grid;
  gap: 5px;
}

.account-list dt {
  color: var(--cream-muted);
  font-size: 0.78rem;
  font-weight: 700;
}

.account-list dd {
  margin: 0;
  color: var(--cream);
  overflow-wrap: anywhere;
  font-size: 0.94rem;
}

.mypage-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.form-label {
  color: var(--cream-muted);
  font-size: 0.82rem;
  font-weight: 700;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--cream);
  font-family: inherit;
  font-size: 0.9rem;
}

.form-input:focus {
  outline: none;
  border-color: var(--gold);
}

.submit-btn,
.danger-btn {
  align-self: flex-start;
  min-height: 38px;
  padding: 0 18px;
  border-radius: 8px;
  font-family: inherit;
  font-weight: 700;
  cursor: pointer;
}

.submit-btn {
  border: 1px solid var(--gold);
  background: var(--gold);
  color: #1a1208;
}

.danger-btn {
  border: 1px solid rgba(232, 80, 80, 0.45);
  background: rgba(232, 80, 80, 0.12);
  color: #ef9a9a;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.check-row {
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--cream-muted);
  font-size: 0.86rem;
}

.check-row input {
  width: 16px;
  height: 16px;
  accent-color: #e87a7a;
}

.danger-panel {
  border-color: rgba(232, 80, 80, 0.22);
}

.status-ok,
.status-error {
  margin: 0;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 0.86rem;
}

.status-ok {
  border: 1px solid rgba(74, 222, 128, 0.2);
  background: rgba(74, 222, 128, 0.08);
  color: #4ade80;
}

.status-error {
  border: 1px solid rgba(232, 80, 80, 0.2);
  background: rgba(232, 80, 80, 0.08);
  color: #e87a7a;
}

@media (max-width: 860px) {
  .mypage-header,
  .check-row {
    align-items: flex-start;
  }

  .mypage-header,
  .mypage-grid,
  .form-row {
    grid-template-columns: 1fr;
  }

  .mypage-header {
    flex-direction: column;
  }

  .account-panel {
    grid-row: auto;
  }
}
</style>
