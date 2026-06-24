<script setup lang="ts">
import { ref } from 'vue'

import { noticesApi } from '@/api/notices'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const title = ref('')
const content = ref('')
const pinned = ref(false)
const publishedAt = ref(new Date().toISOString().slice(0, 16))
const statusMessage = ref('')
const statusIsError = ref(false)
const saving = ref(false)

async function createNotice() {
  saving.value = true
  statusMessage.value = ''
  statusIsError.value = false

  try {
    const response = await noticesApi.create({
      title: title.value,
      content: content.value,
      pinned: pinned.value,
      publishedAt: new Date(publishedAt.value).toISOString()
    })
    statusMessage.value = response.message
    title.value = ''
    content.value = ''
    pinned.value = false
  } catch {
    statusMessage.value = '공지사항을 저장하지 못했습니다. 관리자 권한과 백엔드 API를 확인해 주세요.'
    statusIsError.value = true
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="admin-page">
    <div class="admin-header">
      <p class="eyebrow">관리자</p>
      <h1 class="admin-title">공지사항 관리</h1>
      <p class="admin-desc">커뮤니티 최상단에 공지로 표시될 공지사항을 등록합니다.</p>
      <p v-if="!authStore.isAdmin" class="perm-warn">관리자 권한이 없습니다. 이 기능을 사용할 수 없습니다.</p>
    </div>

    <div class="admin-grid">
      <!-- Notice form -->
      <article class="admin-panel">
        <h2 class="panel-heading">공지사항 등록</h2>
        <form class="notice-form" @submit.prevent="createNotice">
          <div class="form-group">
            <label class="form-label" for="notice-title">제목</label>
            <input
              id="notice-title"
              v-model="title"
              class="form-input"
              required
              maxlength="120"
              type="text"
              placeholder="공지사항 제목을 입력하세요"
            />
          </div>

          <div class="form-group">
            <label class="form-label" for="notice-content">내용</label>
            <textarea
              id="notice-content"
              v-model="content"
              class="form-textarea"
              required
              rows="10"
              placeholder="공지사항 내용을 입력하세요."
            ></textarea>
          </div>

          <div class="form-group">
            <label class="form-label" for="notice-date">게시 일시</label>
            <input
              id="notice-date"
              v-model="publishedAt"
              class="form-input"
              required
              type="datetime-local"
            />
          </div>

          <label class="pin-row">
            <input v-model="pinned" type="checkbox" class="pin-check" />
            <span class="pin-label">상단 고정 (커뮤니티 최상단에 핀으로 표시)</span>
          </label>

          <button class="submit-btn" type="submit" :disabled="saving || !authStore.isAdmin">
            {{ saving ? '등록 중...' : '공지사항 등록' }}
          </button>

          <p v-if="statusMessage" :class="statusIsError ? 'status-error' : 'status-ok'">
            {{ statusMessage }}
          </p>
        </form>
      </article>

      <!-- Info panel -->
      <aside class="admin-sidebar">
        <article class="admin-panel info-panel">
          <h2 class="panel-heading">안내</h2>
          <ul class="info-list">
            <li>등록된 공지사항은 커뮤니티 게시판 상단에 <strong>공지</strong> 태그와 함께 표시됩니다.</li>
            <li>상단 고정을 선택하면 일반 공지보다 더 위에 핀 배지로 노출됩니다.</li>
            <li>게시 일시를 미래로 설정하면 예약 게시됩니다.</li>
            <li>제목은 최대 120자, 내용은 길이 제한이 없습니다.</li>
          </ul>
        </article>

        <article class="admin-panel status-panel">
          <h2 class="panel-heading">시스템 상태</h2>
          <div class="status-row">
            <span class="status-dot" :class="{ active: authStore.isAdmin }" />
            <span>관리자 권한 {{ authStore.isAdmin ? '확인됨' : '없음' }}</span>
          </div>
          <div class="status-row">
            <span class="status-dot active" />
            <span>공지 API 연결됨</span>
          </div>
        </article>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.admin-page {}

.admin-header {
  margin-bottom: 28px;
}

.admin-title {
  margin: 4px 0 6px;
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--cream);
}

.admin-desc {
  margin: 0;
  font-size: 0.88rem;
  color: var(--cream-muted);
}

.perm-warn {
  margin-top: 10px;
  padding: 10px 14px;
  background: rgba(232, 80, 80, 0.08);
  border: 1px solid rgba(232, 80, 80, 0.25);
  border-radius: 8px;
  color: #e87a7a;
  font-size: 0.86rem;
}

.admin-grid {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 20px;
  align-items: start;
}

.admin-panel {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 24px;
}

.panel-heading {
  margin: 0 0 20px;
  font-size: 1rem;
  font-weight: 700;
  color: var(--cream);
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
}

/* Form */
.notice-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.form-label {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--cream-muted);
  letter-spacing: 0.05em;
}

.form-input,
.form-textarea {
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--cream);
  font-size: 0.9rem;
  font-family: inherit;
  transition: border-color 0.2s;
  resize: none;
}
.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: var(--gold);
}
.form-input::placeholder,
.form-textarea::placeholder {
  color: rgba(154, 128, 96, 0.5);
}

.form-textarea { resize: vertical; min-height: 180px; }

.pin-row {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.pin-check {
  width: 16px;
  height: 16px;
  accent-color: var(--gold);
  cursor: pointer;
}

.pin-label {
  font-size: 0.86rem;
  color: var(--cream-muted);
}

.submit-btn {
  padding: 13px 24px;
  background: var(--gold);
  color: #1a1208;
  border: none;
  border-radius: 10px;
  font-size: 0.95rem;
  font-weight: 700;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.2s;
}
.submit-btn:hover:not(:disabled) { background: var(--gold-light); }
.submit-btn:disabled { opacity: 0.45; cursor: not-allowed; }

.status-ok {
  padding: 10px 14px;
  background: rgba(74, 222, 128, 0.08);
  border: 1px solid rgba(74, 222, 128, 0.2);
  border-radius: 8px;
  color: #4ade80;
  font-size: 0.86rem;
  margin: 0;
}

.status-error {
  padding: 10px 14px;
  background: rgba(232, 80, 80, 0.08);
  border: 1px solid rgba(232, 80, 80, 0.2);
  border-radius: 8px;
  color: #e87a7a;
  font-size: 0.86rem;
  margin: 0;
}

/* Sidebar */
.admin-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-list {
  margin: 0;
  padding: 0 0 0 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  color: var(--cream-muted);
  font-size: 0.84rem;
  line-height: 1.6;
}

.info-list strong { color: var(--gold); }

.status-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.84rem;
  color: var(--cream-muted);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  flex-shrink: 0;
}

.status-dot.active {
  background: #4ade80;
  box-shadow: 0 0 6px rgba(74, 222, 128, 0.5);
}

@media (max-width: 900px) {
  .admin-grid { grid-template-columns: 1fr; }
}
</style>
