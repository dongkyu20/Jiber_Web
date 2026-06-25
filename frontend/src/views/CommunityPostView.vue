<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import { communityApi } from '@/api/community'
import { getApiErrorMessage } from '@/api/client'
import type { CommunityCategory, CommunityComment, CommunityPostDetail } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { formatDate } from '@/utils/format'

const route = useRoute()
const authStore = useAuthStore()

const postId = computed(() => String(route.params.id))
const post = ref<CommunityPostDetail | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const newComment = ref('')
const commentMessage = ref('')
const commentSubmitting = ref(false)

const categoryLabels: Record<CommunityCategory, string> = {
  FREE: '자유게시판',
  DEAL_REVIEW: '매물 후기',
  QNA: 'Q&A'
}

const commentCount = computed(() => {
  const comments = post.value?.comments ?? []
  return comments.reduce((count, comment) => count + 1 + comment.replies.length, 0)
})

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
      <div class="post-meta-top">
        <span :class="['cat-badge', `cat-${post.category.toLowerCase()}`]">{{ categoryLabels[post.category] }}</span>
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
          </div>
          <p class="comment-content">{{ comment.content }}</p>

          <div v-for="reply in comment.replies" :key="reply.commentId" class="reply-item">
            <div class="comment-avatar sm">{{ authorName(reply)[0] }}</div>
            <div class="comment-body">
              <div class="comment-header">
                <span class="comment-author">{{ authorName(reply) }}</span>
                <span class="comment-date">{{ formatDate(reply.createdAt) }}</span>
              </div>
              <p class="comment-content">{{ reply.content }}</p>
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
}

.comment-author { font-size: 0.88rem; font-weight: 700; color: var(--cream); }
.comment-date { font-size: 0.76rem; color: var(--cream-muted); }

.comment-content {
  margin: 0;
  font-size: 0.88rem;
  line-height: 1.65;
  color: var(--cream);
  white-space: pre-wrap;
}

.reply-item {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}
</style>
