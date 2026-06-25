<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { adminUsersApi } from '@/api/adminUsers'
import { getApiErrorMessage } from '@/api/client'
import { communityApi } from '@/api/community'
import type { AdminUserSummary, CommunityPostDetail, CommunityPostSummary, UserRole } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { formatDate } from '@/utils/format'

const authStore = useAuthStore()

const noticePosts = ref<CommunityPostSummary[]>([])
const title = ref('')
const content = ref('')
const editingNoticeId = ref<number | null>(null)
const statusMessage = ref('')
const statusIsError = ref(false)
const listError = ref('')
const loadingNotices = ref(false)
const saving = ref(false)
const deletingNoticeId = ref<number | null>(null)
const users = ref<AdminUserSummary[]>([])
const userKeyword = ref('')
const userRoleFilter = ref<UserRole | ''>('')
const userEnabledFilter = ref<'all' | 'enabled' | 'disabled'>('all')
const loadingUsers = ref(false)
const userError = ref('')
const userStatusMessage = ref('')
const userStatusIsError = ref(false)
const updatingUserId = ref<number | null>(null)

const totalNoticeCount = computed(() => noticePosts.value.length)
const totalUserCount = computed(() => users.value.length)
const activeUserCount = computed(() => users.value.filter((user) => user.enabled).length)
const showEmptyNotices = computed(
  () => !loadingNotices.value && !listError.value && !statusIsError.value && noticePosts.value.length === 0
)
const showEmptyUsers = computed(
  () => !loadingUsers.value && !userError.value && users.value.length === 0
)
const formTitle = computed(() => (editingNoticeId.value ? '공지사항 수정' : '공지사항 등록'))
const submitLabel = computed(() => {
  if (saving.value) {
    return editingNoticeId.value ? '수정 중...' : '등록 중...'
  }
  return editingNoticeId.value ? '공지사항 수정' : '공지사항 등록'
})

async function fetchAdminNotices() {
  loadingNotices.value = true
  listError.value = ''

  try {
    const response = await communityApi.listPosts({ page: 0, size: 50, sort: 'createdAt,desc', category: 'NOTICE' })
    noticePosts.value = response.items
  } catch (error) {
    noticePosts.value = []
    listError.value = getApiErrorMessage(error, '공지사항 목록을 불러오지 못했습니다.')
  } finally {
    loadingNotices.value = false
  }
}

async function fetchAdminUsers() {
  loadingUsers.value = true
  userError.value = ''

  try {
    const response = await adminUsersApi.list({
      page: 0,
      size: 20,
      sort: 'createdAt,desc',
      keyword: userKeyword.value.trim() || undefined,
      role: userRoleFilter.value || undefined,
      enabled: enabledFilterValue()
    })
    users.value = response.items
  } catch (error) {
    users.value = []
    userError.value = getApiErrorMessage(error, '회원 목록을 불러오지 못했습니다.')
  } finally {
    loadingUsers.value = false
  }
}

async function saveNotice() {
  saving.value = true
  statusMessage.value = ''
  statusIsError.value = false

  try {
    const payload = {
      category: 'NOTICE' as const,
      title: title.value.trim(),
      content: content.value.trim(),
      relatedPropertyId: null
    }
    const response = editingNoticeId.value
      ? await communityApi.updatePost(editingNoticeId.value, payload)
      : await communityApi.createPost(payload)

    statusMessage.value = response.message
    resetForm()
    await fetchAdminNotices()
  } catch (error) {
    statusMessage.value = getApiErrorMessage(error, '공지사항을 저장하지 못했습니다. 입력값과 관리자 권한을 확인해 주세요.')
    statusIsError.value = true
  } finally {
    saving.value = false
  }
}

async function startEdit(notice: CommunityPostSummary) {
  statusMessage.value = ''
  statusIsError.value = false

  try {
    const detail: CommunityPostDetail = await communityApi.getPost(notice.postId)
    editingNoticeId.value = detail.postId
    title.value = detail.title
    content.value = detail.content
  } catch (error) {
    statusMessage.value = getApiErrorMessage(error, '공지사항 상세를 불러오지 못했습니다.')
    statusIsError.value = true
  }
}

async function deleteNotice(noticeId: number) {
  deletingNoticeId.value = noticeId
  statusMessage.value = ''
  statusIsError.value = false

  try {
    const response = await communityApi.deletePost(noticeId)
    statusMessage.value = response.message
    if (editingNoticeId.value === noticeId) {
      resetForm()
    }
    await fetchAdminNotices()
  } catch (error) {
    statusMessage.value = getApiErrorMessage(error, '공지사항을 삭제하지 못했습니다.')
    statusIsError.value = true
  } finally {
    deletingNoticeId.value = null
  }
}

async function updateUserRole(user: AdminUserSummary) {
  updatingUserId.value = user.userId
  userStatusMessage.value = ''
  userStatusIsError.value = false

  try {
    const nextRole: UserRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN'
    const response = await adminUsersApi.updateRole(user.userId, { role: nextRole })
    userStatusMessage.value = response.message
    await fetchAdminUsers()
  } catch (error) {
    userStatusMessage.value = getApiErrorMessage(error, '회원 권한을 변경하지 못했습니다.')
    userStatusIsError.value = true
  } finally {
    updatingUserId.value = null
  }
}

async function updateUserEnabled(user: AdminUserSummary) {
  updatingUserId.value = user.userId
  userStatusMessage.value = ''
  userStatusIsError.value = false

  try {
    const response = await adminUsersApi.updateEnabled(user.userId, { enabled: !user.enabled })
    userStatusMessage.value = response.message
    await fetchAdminUsers()
  } catch (error) {
    userStatusMessage.value = getApiErrorMessage(error, '회원 상태를 변경하지 못했습니다.')
    userStatusIsError.value = true
  } finally {
    updatingUserId.value = null
  }
}

function resetForm() {
  editingNoticeId.value = null
  title.value = ''
  content.value = ''
}

function isSelf(user: AdminUserSummary) {
  return authStore.user?.userId === user.userId
}

function enabledFilterValue() {
  if (userEnabledFilter.value === 'enabled') {
    return true
  }
  if (userEnabledFilter.value === 'disabled') {
    return false
  }
  return undefined
}

onMounted(() => {
  fetchAdminNotices()
  fetchAdminUsers()
})
</script>

<template>
  <div class="admin-page">
    <section class="admin-header">
      <div>
        <p class="eyebrow">관리자</p>
        <h1 class="admin-title">웹 운영 관리</h1>
        <p class="admin-desc">공지사항과 운영 상태를 한 곳에서 확인합니다.</p>
      </div>
      <p v-if="!authStore.isAdmin" class="perm-warn">관리자 권한이 없습니다.</p>
    </section>

    <section class="admin-metrics" aria-label="관리 지표">
      <article class="metric-card">
        <span class="metric-label">등록 공지</span>
        <strong>{{ totalNoticeCount }}</strong>
      </article>
      <article class="metric-card">
        <span class="metric-label">등록 회원</span>
        <strong>{{ totalUserCount }}</strong>
      </article>
      <article class="metric-card">
        <span class="metric-label">활성 회원</span>
        <strong>{{ activeUserCount }}</strong>
      </article>
      <article class="metric-card">
        <span class="metric-label">권한 상태</span>
        <strong>{{ authStore.isAdmin ? 'ADMIN' : '제한' }}</strong>
      </article>
    </section>

    <div class="admin-grid">
      <article class="admin-panel">
        <div class="panel-head">
          <h2 class="panel-heading">{{ formTitle }}</h2>
          <button v-if="editingNoticeId" class="text-btn" type="button" @click="resetForm">새 공지</button>
        </div>

        <form class="notice-form" data-test="notice-form" @submit.prevent="saveNotice">
          <div class="form-group">
            <label class="form-label" for="notice-title">제목</label>
            <input
              id="notice-title"
              v-model="title"
              class="form-input"
              required
              maxlength="200"
              type="text"
              placeholder="공지사항 제목"
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
              placeholder="공지사항 내용"
            />
          </div>

          <div class="form-actions">
            <button class="submit-btn" type="submit" :disabled="saving || !authStore.isAdmin">
              {{ submitLabel }}
            </button>
            <button v-if="editingNoticeId" class="secondary-btn" type="button" @click="resetForm">취소</button>
          </div>

          <p v-if="statusMessage" :class="statusIsError ? 'status-error' : 'status-ok'">
            {{ statusMessage }}
          </p>
        </form>
      </article>

      <section class="admin-panel notice-panel">
        <div class="panel-head">
          <h2 class="panel-heading">공지사항 목록</h2>
          <button class="text-btn" type="button" :disabled="loadingNotices" @click="fetchAdminNotices">
            {{ loadingNotices ? '새로고침 중' : '새로고침' }}
          </button>
        </div>

        <p v-if="listError" class="status-error">{{ listError }}</p>
        <p v-else-if="loadingNotices" class="muted-text">공지사항을 불러오고 있습니다.</p>

        <div v-if="noticePosts.length" class="notice-table">
          <div class="notice-table-head">
            <span>공지</span>
            <span>작성일</span>
            <span>상태</span>
            <span>작업</span>
          </div>

          <div v-for="notice in noticePosts" :key="notice.postId" class="notice-row">
            <div class="notice-main">
              <strong>{{ notice.title }}</strong>
              <p>{{ notice.authorDisplayName ?? '관리자' }}</p>
            </div>
            <span>{{ formatDate(notice.createdAt) }}</span>
            <span class="badge pinned">공지</span>
            <div class="row-actions">
              <button
                class="row-btn"
                type="button"
                :data-test="`notice-edit-${notice.postId}`"
                @click="startEdit(notice)"
              >
                수정
              </button>
              <button
                class="row-btn danger"
                type="button"
                :disabled="deletingNoticeId === notice.postId"
                :data-test="`notice-delete-${notice.postId}`"
                @click="deleteNotice(notice.postId)"
              >
                {{ deletingNoticeId === notice.postId ? '삭제 중' : '삭제' }}
              </button>
            </div>
          </div>
        </div>

        <div v-else-if="showEmptyNotices" class="empty-admin">
          <strong>등록된 공지사항이 없습니다. 첫 공지를 작성하면 커뮤니티 상단에 노출됩니다.</strong>
        </div>
      </section>
    </div>

    <section class="admin-panel user-panel">
      <div class="panel-head">
        <h2 class="panel-heading">회원 관리</h2>
        <button class="text-btn" type="button" :disabled="loadingUsers" @click="fetchAdminUsers">
          {{ loadingUsers ? '새로고침 중' : '새로고침' }}
        </button>
      </div>

      <form class="user-filter" data-test="user-filter-form" @submit.prevent="fetchAdminUsers">
        <label class="visually-hidden" for="user-keyword">회원 검색</label>
        <input
          id="user-keyword"
          v-model="userKeyword"
          class="form-input"
          type="search"
          placeholder="이메일 또는 이름 검색"
        />

        <label class="visually-hidden" for="user-role">권한 필터</label>
        <select id="user-role" v-model="userRoleFilter" class="form-input">
          <option value="">전체 권한</option>
          <option value="USER">USER</option>
          <option value="ADMIN">ADMIN</option>
        </select>

        <label class="visually-hidden" for="user-enabled">상태 필터</label>
        <select id="user-enabled" v-model="userEnabledFilter" class="form-input">
          <option value="all">전체 상태</option>
          <option value="enabled">활성</option>
          <option value="disabled">비활성</option>
        </select>

        <button class="submit-btn" type="submit" :disabled="loadingUsers">검색</button>
      </form>

      <p v-if="userStatusMessage" :class="userStatusIsError ? 'status-error' : 'status-ok'">
        {{ userStatusMessage }}
      </p>
      <p v-if="userError" class="status-error">{{ userError }}</p>
      <p v-else-if="loadingUsers" class="muted-text">회원 목록을 불러오고 있습니다.</p>

      <div v-if="users.length" class="user-table">
        <div class="user-table-head">
          <span>회원</span>
          <span>권한</span>
          <span>상태</span>
          <span>가입일</span>
          <span>작업</span>
        </div>

        <div v-for="user in users" :key="user.userId" class="user-row">
          <div class="user-main">
            <strong>{{ user.email }}</strong>
            <p>{{ user.displayName || '이름 없음' }}<span v-if="isSelf(user)"> · 본인</span></p>
          </div>
          <span class="badge" :class="{ pinned: user.role === 'ADMIN' }">{{ user.role }}</span>
          <span class="badge" :class="{ active: user.enabled }">{{ user.enabled ? '활성' : '비활성' }}</span>
          <span>{{ formatDate(user.createdAt) }}</span>
          <div class="row-actions">
            <button
              class="row-btn"
              type="button"
              :disabled="updatingUserId === user.userId || isSelf(user)"
              :data-test="`user-role-${user.userId}`"
              @click="updateUserRole(user)"
            >
              {{ user.role === 'ADMIN' ? 'USER로 변경' : 'ADMIN으로 변경' }}
            </button>
            <button
              class="row-btn"
              :class="{ danger: user.enabled }"
              type="button"
              :disabled="updatingUserId === user.userId || isSelf(user)"
              :data-test="`user-enabled-${user.userId}`"
              @click="updateUserEnabled(user)"
            >
              {{ user.enabled ? '비활성화' : '활성화' }}
            </button>
          </div>
        </div>
      </div>

      <div v-else-if="showEmptyUsers" class="empty-admin">
        <strong>조건에 맞는 회원이 없습니다.</strong>
      </div>
    </section>
  </div>
</template>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.admin-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.admin-title {
  margin: 4px 0 6px;
  color: var(--cream);
  font-size: 1.75rem;
  font-weight: 700;
}

.admin-desc,
.muted-text {
  margin: 0;
  color: var(--cream-muted);
  font-size: 0.88rem;
}

.perm-warn {
  margin: 0;
  padding: 10px 14px;
  border: 1px solid rgba(232, 80, 80, 0.25);
  border-radius: 8px;
  background: rgba(232, 80, 80, 0.08);
  color: #e87a7a;
  font-size: 0.86rem;
}

.admin-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.metric-card,
.admin-panel {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
}

.metric-card {
  display: grid;
  gap: 8px;
  min-height: 86px;
  padding: 18px;
}

.metric-label {
  color: var(--cream-muted);
  font-size: 0.78rem;
  font-weight: 700;
}

.metric-card strong {
  color: var(--cream);
  font-size: 1.55rem;
}

.admin-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.8fr) minmax(0, 1.2fr);
  gap: 20px;
  align-items: start;
}

.admin-panel {
  padding: 22px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
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

.notice-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: end;
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

.form-input,
.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--cream);
  font-family: inherit;
  font-size: 0.9rem;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: var(--gold);
}

.form-textarea {
  min-height: 180px;
  resize: vertical;
}

.pin-row {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  min-height: 40px;
  color: var(--cream-muted);
  cursor: pointer;
  font-size: 0.86rem;
}

.pin-check {
  width: 16px;
  height: 16px;
  accent-color: var(--gold);
}

.form-actions,
.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.submit-btn,
.secondary-btn,
.text-btn,
.row-btn {
  min-height: 36px;
  border-radius: 8px;
  font-family: inherit;
  font-weight: 700;
  cursor: pointer;
}

.submit-btn {
  padding: 0 18px;
  border: 1px solid var(--gold);
  background: var(--gold);
  color: #1a1208;
}

.secondary-btn,
.row-btn,
.text-btn {
  border: 1px solid var(--border-strong);
  background: transparent;
  color: var(--cream-muted);
}

.text-btn,
.row-btn {
  padding: 0 12px;
  font-size: 0.82rem;
}

.secondary-btn {
  padding: 0 16px;
}

.danger {
  border-color: rgba(232, 80, 80, 0.38);
  color: #e87a7a;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
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

.notice-panel,
.user-panel {
  min-width: 0;
}

.notice-table,
.user-table {
  display: grid;
  gap: 8px;
}

.user-filter {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 130px 130px auto;
  gap: 10px;
  margin-bottom: 14px;
}

.notice-table-head,
.notice-row,
.user-table-head,
.user-row {
  display: grid;
  gap: 12px;
  align-items: center;
}

.notice-table-head,
.notice-row {
  grid-template-columns: minmax(220px, 1fr) 110px 72px 116px;
}

.user-table-head,
.user-row {
  grid-template-columns: minmax(220px, 1fr) 72px 72px 110px 240px;
}

.notice-table-head,
.user-table-head {
  color: var(--cream-muted);
  font-size: 0.78rem;
  font-weight: 700;
}

.notice-row,
.user-row {
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
  color: var(--cream-muted);
  font-size: 0.82rem;
}

.notice-main,
.user-main {
  min-width: 0;
}

.notice-main strong,
.user-main strong,
.empty-admin strong {
  display: block;
  color: var(--cream);
  font-size: 0.92rem;
}

.notice-main p,
.user-main p,
.empty-admin span {
  margin: 4px 0 0;
  color: var(--cream-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.badge {
  display: inline-flex;
  justify-content: center;
  width: 52px;
  padding: 4px 0;
  border: 1px solid var(--border);
  border-radius: 999px;
  color: var(--cream-muted);
  font-size: 0.76rem;
  font-weight: 700;
}

.badge.pinned {
  border-color: rgba(200, 160, 100, 0.45);
  color: var(--gold);
}

.badge.active {
  border-color: rgba(74, 222, 128, 0.25);
  color: #4ade80;
}

.empty-admin {
  display: grid;
  gap: 5px;
  padding: 22px;
  border: 1px dashed var(--border-strong);
  border-radius: 8px;
}

@media (max-width: 980px) {
  .admin-metrics,
  .admin-grid {
    grid-template-columns: 1fr;
  }

  .notice-table-head {
    display: none;
  }

  .user-filter,
  .notice-row,
  .user-row {
    grid-template-columns: 1fr;
    align-items: start;
  }

  .user-table-head {
    display: none;
  }
}

@media (max-width: 560px) {
  .admin-header,
  .form-row {
    display: flex;
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
