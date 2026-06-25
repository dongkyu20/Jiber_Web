<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { getApiErrorMessage } from '@/api/client'
import { communityApi } from '@/api/community'
import type { CommunityCategory, CommunityComment, CommunityPostDetail } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { formatDate } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const postId = computed(() => String(route.params.id))
const post = ref<CommunityPostDetail | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const newComment = ref('')
const commentMessage = ref('')
const postMessage = ref('')
const commentSubmitting = ref(false)
const postSubmitting = ref(false)
const deletingPost = ref(false)
const editingPost = ref(false)
const editingCommentId = ref<number | null>(null)
const editedCommentContent = ref('')
const commentMutatingId = ref<number | null>(null)

const categories: Array<{ value: CommunityCategory; label: string }> = [
  { value: 'FREE', label: '자유게시판' },
  { value: 'DEAL_REVIEW', label: '매물 후기' },
  { value: 'QNA', label: 'Q&A' }
]

const categoryLabels = categories.reduce(
  (labels, category) => ({ ...labels, [category.value]: category.label }),
  {} as Record<CommunityCategory, string>
)

const postEditForm = ref({
  category: 'FREE' as CommunityCategory,
  title: '',
  content: '',
  relatedPropertyId: null as number | null
})

const commentCount = computed(() => {
  const comments = post.value?.comments ?? []
  return comments.reduce((count, comment) => count + 1 + comment.replies.length, 0)
})

const isPostOwner = computed(() => Boolean(post.value && isOwner(post.value.authorUserId)))

function isOwner(authorUserId?: number | null) {
  return Boolean(authStore.user?.userId && authorUserId === authStore.user.userId)
}

async function loadPost() {
  loading.value = true
  errorMessage.value = ''
  try {
    post.value = await communityApi.getPost(postId.value)
  } catch (error) {
    post.value = null
    errorMessage.value = getApiErrorMessage(error, '게시글을 불러오지 못했습니다.')
  } finally {
    loading.value = false
  }
}

function startPostEdit() {
  if (!post.value) return
  postEditForm.value = {
    category: post.value.category,
    title: post.value.title,
    content: post.value.content,
    relatedPropertyId: post.value.relatedPropertyId ?? null
  }
  postMessage.value = ''
  editingPost.value = true
}

function cancelPostEdit() {
  editingPost.value = false
  postMessage.value = ''
}

async function savePostEdit() {
  if (!post.value || !postEditForm.value.title.trim() || !postEditForm.value.content.trim()) {
    return
  }

  postSubmitting.value = true
  postMessage.value = ''
  try {
    const response = await communityApi.updatePost(post.value.postId, {
      category: postEditForm.value.category,
      title: postEditForm.value.title.trim(),
      content: postEditForm.value.content.trim(),
      relatedPropertyId: postEditForm.value.relatedPropertyId
    })
    postMessage.value = response.message
    editingPost.value = false
    await loadPost()
  } catch (error) {
    postMessage.value = getApiErrorMessage(error, '게시글을 수정하지 못했습니다.')
  } finally {
    postSubmitting.value = false
  }
}

async function deleteCurrentPost() {
  if (!post.value || !window.confirm('게시글을 삭제하시겠습니까?')) {
    return
  }

  deletingPost.value = true
  postMessage.value = ''
  try {
    await communityApi.deletePost(post.value.postId)
    await router.push('/community')
  } catch (error) {
    postMessage.value = getApiErrorMessage(error, '게시글을 삭제하지 못했습니다.')
  } finally {
    deletingPost.value = false
  }
}

async function submitComment() {
  if (!post.value || !newComment.value.trim() || !authStore.isAuthenticated) {
    return
  }

  commentSubmitting.value = true
  commentMessage.value = ''
  try {
    const response = await communityApi.createComment(post.value.postId, { content: newComment.value.trim() })
    newComment.value = ''
    commentMessage.value = response.message
    await loadPost()
  } catch (error) {
    commentMessage.value = getApiErrorMessage(error, '댓글을 등록하지 못했습니다.')
  } finally {
    commentSubmitting.value = false
  }
}

function startCommentEdit(comment: CommunityComment) {
  editingCommentId.value = comment.commentId
  editedCommentContent.value = comment.content
  commentMessage.value = ''
}

function cancelCommentEdit() {
  editingCommentId.value = null
  editedCommentContent.value = ''
}

async function saveCommentEdit(comment: CommunityComment) {
  if (!post.value || !editedCommentContent.value.trim()) {
    return
  }

  commentMutatingId.value = comment.commentId
  commentMessage.value = ''
  try {
    const response = await communityApi.updateComment(post.value.postId, comment.commentId, {
      content: editedCommentContent.value.trim()
    })
    commentMessage.value = response.message
    cancelCommentEdit()
    await loadPost()
  } catch (error) {
    commentMessage.value = getApiErrorMessage(error, '댓글을 수정하지 못했습니다.')
  } finally {
    commentMutatingId.value = null
  }
}

async function deleteComment(comment: CommunityComment) {
  if (!post.value || !window.confirm('댓글을 삭제하시겠습니까?')) {
    return
  }

  commentMutatingId.value = comment.commentId
  commentMessage.value = ''
  try {
    const response = await communityApi.deleteComment(post.value.postId, comment.commentId)
    commentMessage.value = response.message
    if (editingCommentId.value === comment.commentId) {
      cancelCommentEdit()
    }
    await loadPost()
  } catch (error) {
    commentMessage.value = getApiErrorMessage(error, '댓글을 삭제하지 못했습니다.')
  } finally {
    commentMutatingId.value = null
  }
}

function authorName(comment: CommunityComment) {
  return comment.authorDisplayName ?? '탈퇴한 사용자'
}

onMounted(loadPost)
</script>

<template>
  <p v-if="loading" class="loading-text" style="padding: 40px 0;">게시글을 불러오는 중입니다.</p>

  <div v-else-if="post" class="post-wrap">
    <div class="breadcrumb">
      <RouterLink to="/community">커뮤니티</RouterLink>
      <span>/</span>
      <span>{{ categoryLabels[post.category] }}</span>
    </div>

    <article class="post-card">
      <div v-if="editingPost" class="post-edit-form">
        <label>
          <span>게시판</span>
          <select v-model="postEditForm.category">
            <option v-for="category in categories" :key="category.value" :value="category.value">
              {{ category.label }}
            </option>
          </select>
        </label>
        <label>
          <span>제목</span>
          <input v-model="postEditForm.title" maxlength="200" type="text" />
        </label>
        <label>
          <span>내용</span>
          <textarea v-model="postEditForm.content" maxlength="20000" rows="9" />
        </label>
        <div class="action-row">
          <button class="comment-submit" type="button" :disabled="postSubmitting" @click="savePostEdit">
            {{ postSubmitting ? '저장 중' : '저장' }}
          </button>
          <button class="text-button" type="button" :disabled="postSubmitting" @click="cancelPostEdit">취소</button>
        </div>
      </div>

      <template v-else>
        <div class="post-meta-top">
          <div class="post-title-row">
            <span :class="['cat-badge', `cat-${post.category.toLowerCase()}`]">{{ categoryLabels[post.category] }}</span>
            <div v-if="isPostOwner" class="owner-actions">
              <button class="text-button" type="button" @click="startPostEdit">수정</button>
              <button class="text-button danger" type="button" :disabled="deletingPost" @click="deleteCurrentPost">
                {{ deletingPost ? '삭제 중' : '삭제' }}
              </button>
            </div>
          </div>
          <h1 class="post-title">{{ post.title }}</h1>
          <div class="post-author-row">
            <div class="author-avatar">{{ (post.authorDisplayName ?? '익')[0] }}</div>
            <div>
              <p class="author-name">{{ post.authorDisplayName ?? '탈퇴한 사용자' }}</p>
              <p class="author-info">{{ formatDate(post.createdAt) }} · 조회 {{ post.viewCount.toLocaleString('ko-KR') }}</p>
            </div>
          </div>
        </div>

        <div class="post-body">{{ post.content }}</div>

        <div v-if="post.relatedPropertyId" class="related-prop">
          <p class="related-label">관련 매물</p>
          <RouterLink :to="`/properties/${post.relatedPropertyId}`" class="prop-card">
            <div>
              <p class="prop-name">{{ post.relatedPropertyName ?? `매물 #${post.relatedPropertyId}` }}</p>
              <p class="prop-addr">{{ post.relatedPropertyAddress ?? '주소 정보 없음' }}</p>
            </div>
            <span class="prop-link">상세 보기</span>
          </RouterLink>
        </div>
      </template>
      <p v-if="postMessage" class="helper-text">{{ postMessage }}</p>
    </article>

    <section class="comments-section">
      <h2 class="comments-title">댓글 {{ commentCount.toLocaleString('ko-KR') }}</h2>

      <div class="comment-form">
        <input
          v-model="newComment"
          class="comment-input"
          type="text"
          :placeholder="authStore.isAuthenticated ? '댓글을 입력하세요' : '로그인 후 댓글을 작성할 수 있습니다.'"
          :disabled="!authStore.isAuthenticated || commentSubmitting"
          @keyup.enter="submitComment"
        />
        <button class="comment-submit" type="button" :disabled="!authStore.isAuthenticated || commentSubmitting" @click="submitComment">
          {{ commentSubmitting ? '등록 중' : '등록' }}
        </button>
      </div>
      <p v-if="commentMessage" class="helper-text">{{ commentMessage }}</p>

      <div v-for="comment in post.comments" :key="comment.commentId" class="comment-item">
        <div class="comment-avatar">{{ authorName(comment)[0] }}</div>
        <div class="comment-body">
          <div class="comment-header">
            <span class="comment-author">{{ authorName(comment) }}</span>
            <span class="comment-date">{{ formatDate(comment.createdAt) }}</span>
            <div v-if="isOwner(comment.authorUserId)" class="comment-actions">
              <button class="text-button" type="button" @click="startCommentEdit(comment)">수정</button>
              <button
                class="text-button danger"
                type="button"
                :disabled="commentMutatingId === comment.commentId"
                @click="deleteComment(comment)"
              >
                삭제
              </button>
            </div>
          </div>
          <div v-if="editingCommentId === comment.commentId" class="comment-edit">
            <textarea v-model="editedCommentContent" maxlength="5000" rows="3" />
            <div class="action-row compact">
              <button
                class="comment-submit"
                type="button"
                :disabled="commentMutatingId === comment.commentId"
                @click="saveCommentEdit(comment)"
              >
                저장
              </button>
              <button class="text-button" type="button" @click="cancelCommentEdit">취소</button>
            </div>
          </div>
          <p v-else class="comment-content">{{ comment.content }}</p>

          <div v-for="reply in comment.replies" :key="reply.commentId" class="reply-item">
            <div class="comment-avatar sm">{{ authorName(reply)[0] }}</div>
            <div class="comment-body">
              <div class="comment-header">
                <span class="comment-author">{{ authorName(reply) }}</span>
                <span class="comment-date">{{ formatDate(reply.createdAt) }}</span>
                <div v-if="isOwner(reply.authorUserId)" class="comment-actions">
                  <button class="text-button" type="button" @click="startCommentEdit(reply)">수정</button>
                  <button
                    class="text-button danger"
                    type="button"
                    :disabled="commentMutatingId === reply.commentId"
                    @click="deleteComment(reply)"
                  >
                    삭제
                  </button>
                </div>
              </div>
              <div v-if="editingCommentId === reply.commentId" class="comment-edit">
                <textarea v-model="editedCommentContent" maxlength="5000" rows="3" />
                <div class="action-row compact">
                  <button
                    class="comment-submit"
                    type="button"
                    :disabled="commentMutatingId === reply.commentId"
                    @click="saveCommentEdit(reply)"
                  >
                    저장
                  </button>
                  <button class="text-button" type="button" @click="cancelCommentEdit">취소</button>
                </div>
              </div>
              <p v-else class="comment-content">{{ reply.content }}</p>
            </div>
          </div>
        </div>
      </div>

      <div v-if="!post.comments.length" class="empty-state">
        <p>첫 댓글을 남겨보세요.</p>
      </div>
    </section>
  </div>

  <div v-else class="empty-state">
    <p>{{ errorMessage || '게시글을 찾을 수 없습니다.' }}</p>
    <RouterLink to="/community" class="primary-button" style="margin-top:16px">커뮤니티로 돌아가기</RouterLink>
  </div>
</template>

<style scoped>
.post-wrap { max-width: 820px; }

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.82rem;
  color: var(--cream-muted);
  margin-bottom: 20px;
}
.breadcrumb a { color: var(--cream-muted); transition: color 0.2s; }
.breadcrumb a:hover { color: var(--gold); }

.post-card,
.comments-section {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 28px 32px;
}

.post-card { margin-bottom: 24px; }
.post-meta-top { margin-bottom: 24px; }

.post-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.cat-badge {
  display: inline-block;
  font-size: 0.72rem;
  padding: 3px 10px;
  border-radius: 4px;
  font-weight: 700;
  margin-bottom: 12px;
}
.cat-free { background: rgba(100,180,100,0.12); color: #7ac97a; }
.cat-deal_review { background: rgba(100,150,220,0.12); color: #7ab0e0; }
.cat-qna { background: rgba(180,100,200,0.12); color: #c07ad0; }

.post-title {
  margin: 0 0 18px;
  font-size: 1.45rem;
  font-weight: 700;
  color: var(--cream);
  line-height: 1.35;
}

.post-author-row,
.comment-item,
.reply-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.author-avatar,
.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gold);
  color: #1a1208;
  font-size: 0.9rem;
  font-weight: 700;
  flex-shrink: 0;
}

.comment-avatar.sm { width: 28px; height: 28px; font-size: 0.76rem; }

.author-name {
  margin: 0 0 2px;
  font-size: 0.92rem;
  font-weight: 700;
  color: var(--cream);
}

.author-info {
  margin: 0;
  font-size: 0.78rem;
  color: var(--cream-muted);
}

.post-body {
  font-size: 0.95rem;
  line-height: 1.8;
  color: var(--cream);
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
  padding: 24px 0;
  margin-bottom: 20px;
  white-space: pre-wrap;
}

.post-edit-form {
  display: grid;
  gap: 14px;
}

.post-edit-form label {
  display: grid;
  gap: 7px;
}

.post-edit-form label span {
  color: var(--cream-muted);
  font-size: 0.82rem;
  font-weight: 700;
}

.post-edit-form textarea,
.comment-edit textarea {
  resize: vertical;
}

.related-label {
  font-size: 0.76rem;
  color: var(--cream-muted);
  letter-spacing: 0.1em;
  margin: 0 0 10px;
}

.prop-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: rgba(200,160,100,0.06);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 16px 20px;
}
.prop-card:hover { border-color: var(--border-strong); }
.prop-name { margin: 0 0 3px; font-size: 1rem; font-weight: 700; color: var(--cream); }
.prop-addr { margin: 0; font-size: 0.8rem; color: var(--cream-muted); }
.prop-link { font-size: 0.78rem; color: var(--gold); }

.comments-title {
  margin: 0 0 20px;
  font-size: 1rem;
  font-weight: 700;
  color: var(--cream);
}

.comment-form {
  display: flex;
  gap: 10px;
  margin-bottom: 6px;
}

.comment-input {
  flex: 1;
}

.comment-submit {
  padding: 11px 20px;
  background: var(--gold);
  color: #1a1208;
  border: none;
  border-radius: 8px;
  font-size: 0.88rem;
  font-weight: 700;
  white-space: nowrap;
}

.comment-submit:disabled,
.text-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.owner-actions,
.comment-actions,
.action-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-row {
  justify-content: flex-end;
}

.action-row.compact {
  justify-content: flex-start;
  margin-top: 8px;
}

.text-button {
  border: 1px solid var(--border);
  background: transparent;
  color: var(--cream-muted);
  border-radius: 7px;
  padding: 7px 10px;
  font-size: 0.78rem;
  font-weight: 700;
}

.text-button:hover {
  color: var(--cream);
  border-color: var(--border-strong);
}

.text-button.danger {
  color: #e08c7a;
}

.comment-item {
  padding: 16px 0;
  border-bottom: 1px solid var(--border);
}
.comment-item:last-of-type { border-bottom: none; }

.comment-body { flex: 1; min-width: 0; }

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.comment-author { font-size: 0.88rem; font-weight: 700; color: var(--cream); }
.comment-date { font-size: 0.76rem; color: var(--cream-muted); }

.comment-actions {
  margin-left: auto;
}

.comment-content {
  margin: 0;
  font-size: 0.88rem;
  line-height: 1.65;
  color: var(--cream);
  white-space: pre-wrap;
}

.comment-edit {
  display: grid;
  gap: 8px;
}

.reply-item {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}
</style>
