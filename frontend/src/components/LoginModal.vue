<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { getOAuthStartUrl, type OAuthProvider } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

const authStore = useAuthStore()
const uiStore = useUiStore()
const router = useRouter()

const email = ref('')
const password = ref('')
const rememberMe = ref(false)
const errorMessage = ref('')

function close() {
  uiStore.closeAll()
}

function goSignup() {
  uiStore.switchToSignup()
}

function startSocial(provider: OAuthProvider) {
  window.location.href = getOAuthStartUrl(provider)
}

async function submit() {
  errorMessage.value = ''
  if (!email.value || !password.value) {
    errorMessage.value = '아이디와 비밀번호를 모두 입력해 주세요.'
    return
  }

  try {
    await authStore.loginWithPassword({ email: email.value.trim(), password: password.value })
    uiStore.closeAll()
    const redirect = uiStore.loginRedirect
    if (redirect) await router.push(redirect)
  } catch {
    errorMessage.value = '아이디 또는 비밀번호가 올바르지 않습니다.'
  }
}
</script>

<template>
  <div class="modal-overlay" @click.self="close">
    <div class="modal-card" role="dialog" aria-modal="true" aria-label="로그인">
      <button class="modal-close" type="button" @click="close" aria-label="닫기">✕</button>

      <div class="modal-logo">
        <span class="brand-ko">집</span><span class="brand-en">ER</span>
        <span class="brand-sub">ESTATE REAL</span>
      </div>

      <h2 class="modal-title">다시 오신 것을 환영합니다</h2>
      <p class="modal-subtitle">데이터로 검증하는 부동산, 집er</p>

      <form @submit.prevent="submit" class="modal-form">
        <div class="modal-field">
          <label for="login-id">아이디</label>
          <input id="login-id" v-model="email" type="email" placeholder="이메일 주소" autocomplete="email" />
        </div>
        <div class="modal-field">
          <label for="login-pw">비밀번호</label>
          <input id="login-pw" v-model="password" type="password" placeholder="비밀번호" autocomplete="current-password" />
        </div>

        <div class="modal-options">
          <label class="modal-check">
            <input type="checkbox" v-model="rememberMe" />
            <span>로그인 유지</span>
          </label>
          <a href="#" class="modal-link">아이디 · 비밀번호 찾기</a>
        </div>

        <p v-if="errorMessage" class="modal-error">{{ errorMessage }}</p>

        <button class="modal-btn-primary" type="submit" :disabled="authStore.loading">
          {{ authStore.loading ? '로그인 중...' : '로그인' }}
        </button>
      </form>

      <button class="modal-btn-secondary" type="button" @click="goSignup">회원가입</button>

      <div class="modal-divider"><span>간편 로그인</span></div>

      <div class="modal-social">
        <button class="social-btn kakao" type="button" @click="startSocial('kakao')">K</button>
        <button class="social-btn naver" type="button" @click="startSocial('naver')">N</button>
        <button class="social-btn google" type="button" @click="startSocial('google')">G</button>
      </div>

      <p class="modal-terms">
        로그인 시 집er의 <a href="#">이용약관</a> 및 <a href="#">개인정보처리방침</a>에 동의하게 됩니다.
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
  gap: 0;
  color: #1a1208;
  box-shadow: 0 24px 60px rgba(0,0,0,0.5);
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
  font-family: var(--font-logo);
}

.modal-logo .brand-ko { color: #1a1208; font-size: 1.6rem; font-weight: 700; letter-spacing: -0.02em; }
.modal-logo .brand-en { color: #1a1208; font-size: 1.3rem; font-weight: 700; font-style: italic; }
.modal-logo .brand-sub { color: #8a7255; font-size: 0.62rem; letter-spacing: 0.18em; margin-left: 6px; }

.modal-title {
  margin: 0 0 6px;
  font-size: 1.35rem;
  font-weight: 700;
  color: #1a1208;
  text-align: center;
}

.modal-subtitle {
  margin: 0 0 24px;
  font-size: 0.85rem;
  color: #8a7255;
  text-align: center;
}

.modal-form { display: flex; flex-direction: column; gap: 14px; margin-bottom: 12px; }

.modal-field { display: flex; flex-direction: column; gap: 6px; }

.modal-field label {
  font-size: 0.82rem;
  font-weight: 700;
  color: #4a3520;
  letter-spacing: 0.02em;
}

.modal-field input {
  border: 1px solid rgba(100, 80, 50, 0.25);
  border-radius: 10px;
  background: rgba(255,255,255,0.7);
  padding: 12px 14px;
  color: #1a1208;
  font-size: 0.92rem;
  transition: border-color 0.2s;
  width: 100%;
}

.modal-field input:focus {
  border-color: rgba(100, 80, 50, 0.5);
  outline: none;
  background: white;
}

.modal-field input::placeholder { color: #b09070; }

.modal-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: -4px;
}

.modal-check {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 0.82rem;
  color: #6a5035;
  cursor: pointer;
}

.modal-check input[type="checkbox"] { accent-color: #1a1208; width: auto; }

.modal-link {
  font-size: 0.8rem;
  color: #8a7255;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.modal-error {
  margin: 0;
  padding: 10px 12px;
  background: rgba(220, 80, 80, 0.1);
  border: 1px solid rgba(220, 80, 80, 0.3);
  border-radius: 8px;
  color: #c04040;
  font-size: 0.85rem;
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

.modal-btn-secondary {
  width: 100%;
  padding: 13px;
  background: transparent;
  color: #4a3520;
  border: 1px solid rgba(100, 80, 50, 0.3);
  border-radius: 10px;
  font-size: 0.92rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 20px;
}
.modal-btn-secondary:hover { background: rgba(0,0,0,0.05); }

.modal-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #9a8060;
  font-size: 0.8rem;
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
  display: flex;
  align-items: center;
  justify-content: center;
}
.social-btn:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
.social-btn.kakao { background: #FEE500; color: #1a1208; }
.social-btn.naver { background: #03C75A; color: white; }
.social-btn.google { background: white; color: #444; border: 1px solid #ddd; }

.modal-terms {
  margin: 0;
  font-size: 0.76rem;
  color: #9a8060;
  text-align: center;
  line-height: 1.6;
}
.modal-terms a { color: #6a5035; text-decoration: underline; text-underline-offset: 2px; }
</style>
