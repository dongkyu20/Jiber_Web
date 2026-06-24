<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { getOAuthStartUrl, type OAuthProvider } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

const authStore = useAuthStore()
const uiStore = useUiStore()
const router = useRouter()

const email = ref('')
const password = ref('')
const passwordConfirm = ref('')
const displayName = ref('')
const agreeTerms = ref(false)
const agreePrivacy = ref(false)
const agreeMarketing = ref(false)
const errorMessage = ref('')

const agreeAll = computed(() => agreeTerms.value && agreePrivacy.value && agreeMarketing.value)

function toggleAll(e: Event) {
  const checked = (e.target as HTMLInputElement).checked
  agreeTerms.value = checked
  agreePrivacy.value = checked
  agreeMarketing.value = checked
}

watch([agreeTerms, agreePrivacy, agreeMarketing], () => {
  // computed agreeAll handles display
})

function close() { uiStore.closeAll() }
function goLogin() { uiStore.switchToLogin() }

function startSocial(provider: OAuthProvider) {
  window.location.href = getOAuthStartUrl(provider)
}

async function submit() {
  errorMessage.value = ''

  if (!email.value || !password.value || !passwordConfirm.value || !displayName.value) {
    errorMessage.value = '모든 항목을 입력해 주세요.'
    return
  }
  if (password.value !== passwordConfirm.value) {
    errorMessage.value = '비밀번호가 일치하지 않습니다.'
    return
  }
  if (password.value.length < 8) {
    errorMessage.value = '비밀번호는 8자 이상이어야 합니다.'
    return
  }
  if (!agreeTerms.value || !agreePrivacy.value) {
    errorMessage.value = '필수 약관에 동의해 주세요.'
    return
  }

  try {
    await authStore.signupWithPassword({
      email: email.value.trim(),
      displayName: displayName.value.trim(),
      password: password.value
    })
    uiStore.closeAll()
    await router.push('/map')
  } catch {
    errorMessage.value = '회원가입을 완료하지 못했습니다. 이미 사용 중인 이메일일 수 있습니다.'
  }
}
</script>

<template>
  <div class="modal-overlay" @click.self="close">
    <div class="modal-card" role="dialog" aria-modal="true" aria-label="회원가입">
      <button class="modal-close" type="button" @click="close" aria-label="닫기">✕</button>

      <div class="modal-logo">
        <span class="brand-ko">집</span><span class="brand-en">ER</span>
        <span class="brand-sub">ESTATE REAL</span>
      </div>

      <h2 class="modal-title">회원가입</h2>
      <p class="modal-subtitle">집er와 함께 데이터로 검증하는 부동산 탐색을 시작하세요</p>

      <form @submit.prevent="submit" class="modal-form">
        <div class="modal-field">
          <label for="su-email">아이디</label>
          <input id="su-email" v-model="email" type="email" placeholder="이메일 주소" autocomplete="email" />
        </div>
        <div class="modal-field">
          <label for="su-pw">비밀번호</label>
          <input id="su-pw" v-model="password" type="password" placeholder="8자 이상, 영문+숫자 조합" autocomplete="new-password" />
        </div>
        <div class="modal-field">
          <label for="su-pw2">비밀번호 확인</label>
          <input id="su-pw2" v-model="passwordConfirm" type="password" placeholder="비밀번호를 한 번 더 입력하세요" autocomplete="new-password" />
        </div>
        <div class="modal-field">
          <label for="su-name">닉네임</label>
          <input id="su-name" v-model="displayName" type="text" placeholder="2~10자" autocomplete="name" />
        </div>

        <div class="modal-agree">
          <label class="agree-all">
            <input type="checkbox" :checked="agreeAll" @change="toggleAll" />
            <span>전체 동의</span>
          </label>
          <label class="agree-item">
            <input type="checkbox" v-model="agreeTerms" />
            <span>[필수] 이용약관 동의 <a href="#">보기</a></span>
          </label>
          <label class="agree-item">
            <input type="checkbox" v-model="agreePrivacy" />
            <span>[필수] 개인정보 처리방침 동의 <a href="#">보기</a></span>
          </label>
          <label class="agree-item">
            <input type="checkbox" v-model="agreeMarketing" />
            <span>[선택] 마케팅 정보 수신 동의</span>
          </label>
        </div>

        <p v-if="errorMessage" class="modal-error">{{ errorMessage }}</p>

        <button class="modal-btn-primary" type="submit" :disabled="authStore.loading">
          {{ authStore.loading ? '가입 중...' : '가입 완료' }}
        </button>
      </form>

      <div class="modal-divider"><span>간편 가입</span></div>

      <div class="modal-social">
        <button class="social-btn kakao" type="button" @click="startSocial('kakao')">K</button>
        <button class="social-btn naver" type="button" @click="startSocial('naver')">N</button>
        <button class="social-btn google" type="button" @click="startSocial('google')">G</button>
      </div>

      <p class="modal-footer">
        이미 계정이 있으신가요?
        <button type="button" class="inline-link" @click="goLogin">로그인</button>
      </p>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 500;
  background: rgba(8, 4, 1, 0.85);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  overflow-y: auto;
}

.modal-card {
  position: relative;
  width: 100%;
  max-width: 420px;
  background: var(--cream-card);
  border-radius: 18px;
  padding: 40px 36px 32px;
  display: flex;
  flex-direction: column;
  color: #1a1208;
  box-shadow: 0 24px 60px rgba(0,0,0,0.5);
  margin: auto;
}

.modal-close {
  position: absolute;
  top: 16px;
  right: 18px;
  width: 32px;
  height: 32px;
  background: none;
  border: none;
  font-size: 1.1rem;
  color: #8a7255;
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}
.modal-close:hover { background: rgba(0,0,0,0.08); }

.modal-logo {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
  margin-bottom: 20px;
}
.modal-logo .brand-ko { color: #1a1208; font-size: 1.6rem; font-weight: 700; letter-spacing: -0.02em; }
.modal-logo .brand-en { color: #1a1208; font-size: 1.3rem; font-weight: 700; font-style: italic; }
.modal-logo .brand-sub { color: #8a7255; font-size: 0.62rem; letter-spacing: 0.18em; margin-left: 6px; }

.modal-title {
  margin: 0 0 6px;
  font-size: 1.3rem;
  font-weight: 700;
  color: #1a1208;
  text-align: left;
}

.modal-subtitle {
  margin: 0 0 22px;
  font-size: 0.83rem;
  color: #8a7255;
  text-align: left;
  line-height: 1.5;
}

.modal-form { display: flex; flex-direction: column; gap: 12px; margin-bottom: 16px; }

.modal-field { display: flex; flex-direction: column; gap: 5px; }

.modal-field label {
  font-size: 0.8rem;
  font-weight: 700;
  color: #4a3520;
  letter-spacing: 0.02em;
}

.modal-field input {
  border: 1px solid rgba(100, 80, 50, 0.25);
  border-radius: 10px;
  background: rgba(255,255,255,0.7);
  padding: 11px 14px;
  color: #1a1208;
  font-size: 0.9rem;
  transition: border-color 0.2s;
  width: 100%;
}
.modal-field input:focus { border-color: rgba(100,80,50,0.5); outline: none; background: white; }
.modal-field input::placeholder { color: #b09070; }

.modal-agree {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  border: 1px solid rgba(100,80,50,0.2);
  border-radius: 10px;
  background: rgba(255,255,255,0.4);
  margin-top: 4px;
}

.agree-all {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  font-weight: 700;
  color: #2a1a0a;
  cursor: pointer;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(100,80,50,0.15);
}

.agree-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8rem;
  color: #6a5035;
  cursor: pointer;
}
.agree-item a { color: #4a3520; text-decoration: underline; text-underline-offset: 2px; }

.agree-all input, .agree-item input { accent-color: #1a1208; width: auto; margin: 0; }

.modal-error {
  margin: 0;
  padding: 10px 12px;
  background: rgba(220,80,80,0.1);
  border: 1px solid rgba(220,80,80,0.3);
  border-radius: 8px;
  color: #c04040;
  font-size: 0.84rem;
}

.modal-btn-primary {
  width: 100%;
  padding: 14px;
  background: #1a1208;
  color: #f0e6d0;
  border: none;
  border-radius: 10px;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s;
  margin-top: 4px;
}
.modal-btn-primary:hover:not(:disabled) { background: #0d0804; }
.modal-btn-primary:disabled { opacity: 0.6; cursor: wait; }

.modal-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #9a8060;
  font-size: 0.78rem;
  margin-bottom: 16px;
}
.modal-divider::before, .modal-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: rgba(100,80,50,0.2);
}

.modal-social {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 20px;
}

.social-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: none;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}
.social-btn:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
.social-btn.kakao { background: #FEE500; color: #1a1208; }
.social-btn.naver { background: #03C75A; color: white; }
.social-btn.google { background: white; color: #444; border: 1px solid #ddd; }

.modal-footer {
  margin: 0;
  font-size: 0.82rem;
  color: #8a7255;
  text-align: center;
}

.inline-link {
  color: #4a3520;
  font-weight: 700;
  text-decoration: underline;
  text-underline-offset: 2px;
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  font-size: inherit;
  font-family: inherit;
}
</style>
