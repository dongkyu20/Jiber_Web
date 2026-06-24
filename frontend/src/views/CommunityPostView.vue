<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import { noticesApi } from '@/api/notices'
import { useAuthStore } from '@/stores/auth'
import { formatDate } from '@/utils/format'

const route = useRoute()
const authStore = useAuthStore()

const postId = computed(() => String(route.params.id))
const isNotice = computed(() => postId.value.startsWith('n-'))

interface Comment {
  id: number
  author: string
  avatarColor: string
  content: string
  createdAt: string
  likes: number
  replies?: Comment[]
  isAdmin?: boolean
}

interface PostData {
  id: string
  category: string
  title: string
  author: string
  authorLevel?: string
  createdAt: string
  viewCount: number
  likes: number
  content: string
  relatedProperty?: { name: string; address: string; price: string }
  comments: Comment[]
}

const post = ref<PostData | null>(null)
const loading = ref(true)
const newComment = ref('')
const submitSuccess = ref(false)
const breadcrumb = ref('커뮤니티')

const mockPosts: Record<string, PostData> = {
  '1': {
    id: '1',
    category: '매매후기',
    title: '반포 아크로리버파크 84m² 실거주 1년 후기 (장단점 솔직 정리)',
    author: '리버뷰',
    authorLevel: 'Lv.5',
    createdAt: '2026-06-21',
    viewCount: 3201,
    likes: 48,
    content: `작년 6월에 84m²(34평) 매수해서 입주한 지 꼭 1년 됐습니다. 여수 근처에 카뮤니티 후기가 많지 않고 솔직하고 생생한 후기가 없어서, 저도 실거주하며 느낀 감상을 정직하게 공유해 보려 합니다.

🟡 좋았던 점

• 한강 조망, 거실 동에서도 보이는 배경 뷰도 안 질립니다. 마경은 특히 인도적이에요.
• 9호선 신반포역 도보 5분, 강남·어디도 즐따라기가 정말 편합니다.
• 단지 관리 상태가 좋습니다. 조경, 조명, 카뮤니티 시설(헬스·공원·의원실) 완성도 높습니다.

🔴 아쉬운 점

• 관리비가 꽤 많이 높습니다. 84m² 기준 월 35~45만 원 정도 나옵니다.
• 주변 단지 내 수직이 여진히 백백합니다. 제네3덕 1.4배인데 계산은 그보다 백백해요.

집er AI 적정가 보니 새로 오기가 추정가 대비 +4.2% 정도라 고평가 구간이긴 한데, 실거주 선호도까지 감안하면 부하는 없습니다. 궁금한 점 있던 더 년 주시면 아는 선에서 답변 드릴게요!`,
    relatedProperty: { name: '아크로리버파크', address: '서초 반포동 · 전용 84m² 34평', price: '35억 8,000' },
    comments: [
      { id: 1, author: '마로라', avatarColor: '#c9a56e', content: '관리비 정말 비싸죠? 지 월 70만원이요. 겨울에는 더 나온다고 알고있어요. 밝은 집 재지정 나오면 어떨지 기대합니다.', createdAt: '2026-06-21', likes: 12,
        replies: [{ id: 11, author: '리버뷰', avatarColor: '#7ac97a', isAdmin: false, content: '88m²보다 거진 저렴합니다! 지금도 기간보다 더 나옵니다요. 월 35~45만 원 범위 꽤 초과하게 됩니다!', createdAt: '2026-06-21', likes: 5 }]},
      { id: 2, author: '꽃달리면', avatarColor: '#7ab0e0', content: '솔직한 후기 정말 도움됩니다. 주차가 제가 가장 명이었는데 밝습니다.ㅜㅜ', createdAt: '2026-06-20', likes: 7 },
      { id: 3, author: '알이치해수', avatarColor: '#e87a7a', content: 'AI 적정가 +4.2% 고평가가 판단합니다. 실거주 기격가 확정된다면 괜찮을 단지인 것 같습니다.', createdAt: '2026-06-20', likes: 3 },
      { id: 4, author: '첫집도전', avatarColor: '#c07ad0', content: '한강뷰는 정말 부러워요. 저도 언젠가는...', createdAt: '2026-06-19', likes: 2 },
    ]
  },
}

async function loadPost() {
  loading.value = true
  try {
    if (isNotice.value) {
      const noticeId = Number(postId.value.replace('n-', ''))
      const res = await noticesApi.list({ page: 0, size: 50 })
      const found = res.items.find(n => n.noticeId === noticeId)
      if (found) {
        post.value = {
          id: postId.value,
          category: '공지',
          title: found.title,
          author: '운영자',
          createdAt: found.publishedAt,
          viewCount: 0,
          likes: 0,
          content: found.summary ?? '내용 없음',
          comments: []
        }
        breadcrumb.value = '공지사항'
      }
    } else {
      const found = mockPosts[postId.value]
      if (found) {
        post.value = found
        breadcrumb.value = '커뮤니티'
      }
    }
  } finally {
    loading.value = false
  }
}

function submitComment() {
  if (!newComment.value.trim()) return
  if (!authStore.isAuthenticated) return
  newComment.value = ''
  submitSuccess.value = true
  setTimeout(() => { submitSuccess.value = false }, 2000)
}

onMounted(loadPost)
</script>

<template>
  <div v-if="loading" class="loading-text" style="padding: 40px 0;">불러오는 중...</div>

  <div v-else-if="post" class="post-wrap">
    <!-- Breadcrumb -->
    <div class="breadcrumb">
      <RouterLink to="/community">{{ breadcrumb }}</RouterLink>
      <span>›</span>
      <span>{{ post.relatedProperty?.name ?? post.title.slice(0, 20) }}</span>
    </div>

    <!-- Post card -->
    <article class="post-card">
      <div class="post-meta-top">
        <span :class="['cat-badge', `cat-${post.category}`]">{{ post.category }}</span>
        <h1 class="post-title">{{ post.title }}</h1>
        <div class="post-author-row">
          <div class="author-avatar" :style="{ background: '#c9a56e' }">{{ post.author[0] }}</div>
          <div>
            <p class="author-name">{{ post.author }} <span v-if="post.authorLevel" class="author-level">{{ post.authorLevel }}</span></p>
            <p class="author-info">{{ formatDate(post.createdAt) }} · 조회 {{ post.viewCount.toLocaleString() }}</p>
          </div>
          <div class="post-actions-top">
            <button class="act-btn">좋아요</button>
            <button class="act-btn danger">신고</button>
          </div>
        </div>
      </div>

      <div class="post-body" v-html="post.content.replace(/\n/g, '<br/>')"></div>

      <!-- Related property card -->
      <div v-if="post.relatedProperty" class="related-prop">
        <p class="related-label">관련 단지</p>
        <RouterLink to="/map" class="prop-card">
          <div>
            <p class="prop-name">{{ post.relatedProperty.name }}</p>
            <p class="prop-addr">{{ post.relatedProperty.address }}</p>
          </div>
          <div class="prop-right">
            <p class="prop-price">{{ post.relatedProperty.price }}</p>
            <span class="prop-link">단지 정보 보기 →</span>
          </div>
        </RouterLink>
      </div>

      <!-- Like / Scrap -->
      <div class="post-actions">
        <button class="post-action-btn">
          <span>👍</span> 추천 {{ post.likes }}
        </button>
        <button class="post-action-btn">
          <span>🔖</span> 스크랩
        </button>
      </div>
    </article>

    <!-- Comments -->
    <section class="comments-section">
      <h2 class="comments-title">댓글 {{ post.comments.length }}</h2>

      <!-- Comment input -->
      <div class="comment-form">
        <input
          v-model="newComment"
          type="text"
          :placeholder="authStore.isAuthenticated ? '따뜻한 글로도 정보를 나눠주세요.' : '로그인 후 댓글을 작성할 수 있습니다.'"
          :disabled="!authStore.isAuthenticated"
          @keyup.enter="submitComment"
          class="comment-input"
        />
        <button class="comment-submit" @click="submitComment" :disabled="!authStore.isAuthenticated">
          등록
        </button>
      </div>
      <p v-if="submitSuccess" class="helper-text">댓글이 등록됐습니다.</p>

      <!-- Comment list -->
      <div v-for="comment in post.comments" :key="comment.id" class="comment-item">
        <div class="comment-avatar" :style="{ background: comment.avatarColor }">{{ comment.author[0] }}</div>
        <div class="comment-body">
          <div class="comment-header">
            <span class="comment-author">{{ comment.author }}</span>
            <span class="comment-date">{{ formatDate(comment.createdAt) }}</span>
          </div>
          <p class="comment-content">{{ comment.content }}</p>
          <div class="comment-foot">
            <button class="comment-act">♥ {{ comment.likes }}</button>
            <button class="comment-act">답글</button>
          </div>

          <!-- Replies -->
          <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
            <div class="comment-avatar sm" :style="{ background: reply.avatarColor }">{{ reply.author[0] }}</div>
            <div class="comment-body">
              <div class="comment-header">
                <span class="comment-author">{{ reply.author }}</span>
                <span class="comment-date">{{ formatDate(reply.createdAt) }}</span>
              </div>
              <p class="comment-content">{{ reply.content }}</p>
              <div class="comment-foot">
                <button class="comment-act">♥ {{ reply.likes }}</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="!post.comments.length" class="empty-state">
        <p>첫 댓글을 남겨보세요.</p>
      </div>

      <button class="load-more-btn">댓글 더 보기</button>
    </section>
  </div>

  <div v-else class="empty-state">
    <p>게시글을 찾을 수 없습니다.</p>
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

.post-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 32px 36px;
  margin-bottom: 28px;
}

.post-meta-top { margin-bottom: 24px; }

.cat-badge {
  display: inline-block;
  font-size: 0.72rem;
  padding: 3px 10px;
  border-radius: 4px;
  font-weight: 700;
  margin-bottom: 12px;
}
.cat-공지 { background: rgba(200,160,100,0.15); color: var(--gold); }
.cat-매매후기 { background: rgba(100,150,220,0.12); color: #7ab0e0; }
.cat-자유 { background: rgba(100,180,100,0.12); color: #7ac97a; }
.cat-Q\\&A { background: rgba(180,100,200,0.12); color: #c07ad0; }

.post-title {
  margin: 0 0 18px;
  font-size: 1.45rem;
  font-weight: 700;
  color: var(--cream);
  line-height: 1.35;
}

.post-author-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.9rem;
  font-weight: 700;
  color: #1a1208;
  flex-shrink: 0;
}

.author-name {
  margin: 0 0 2px;
  font-size: 0.92rem;
  font-weight: 700;
  color: var(--cream);
}

.author-level {
  font-size: 0.72rem;
  color: var(--gold);
  background: rgba(200,160,100,0.12);
  padding: 1px 6px;
  border-radius: 4px;
  margin-left: 4px;
}

.author-info {
  margin: 0;
  font-size: 0.78rem;
  color: var(--cream-muted);
}

.post-actions-top {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.act-btn {
  padding: 6px 14px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: transparent;
  color: var(--cream-muted);
  font-size: 0.8rem;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s;
}
.act-btn:hover { color: var(--cream); }
.act-btn.danger { color: #e87a7a; border-color: rgba(232, 122, 122, 0.2); }

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

.related-prop {
  margin-bottom: 24px;
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
  transition: border-color 0.2s;
}
.prop-card:hover { border-color: var(--border-strong); }

.prop-name { margin: 0 0 3px; font-size: 1rem; font-weight: 700; color: var(--cream); }
.prop-addr { margin: 0; font-size: 0.8rem; color: var(--cream-muted); }

.prop-right { text-align: right; flex-shrink: 0; }
.prop-price { margin: 0 0 4px; font-size: 1.1rem; font-weight: 700; color: var(--cream); }
.prop-link { font-size: 0.78rem; color: var(--gold); }

.post-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.post-action-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 11px 28px;
  border: 1px solid var(--border);
  border-radius: 30px;
  background: transparent;
  color: var(--cream-muted);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s;
}
.post-action-btn:hover { color: var(--cream); border-color: var(--border-strong); }

/* Comments */
.comments-section {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 28px 36px;
}

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
  padding: 11px 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: rgba(255,255,255,0.04);
  color: var(--cream);
  font-size: 0.88rem;
  font-family: inherit;
  transition: border-color 0.2s;
}
.comment-input:focus { border-color: var(--border-strong); outline: none; }
.comment-input::placeholder { color: rgba(154,128,96,0.7); }
.comment-input:disabled { opacity: 0.5; cursor: not-allowed; }

.comment-submit {
  padding: 11px 20px;
  background: var(--gold);
  color: #1a1208;
  border: none;
  border-radius: 8px;
  font-size: 0.88rem;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.2s;
  white-space: nowrap;
}
.comment-submit:hover:not(:disabled) { background: var(--gold-light); }
.comment-submit:disabled { opacity: 0.5; cursor: not-allowed; }

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid var(--border);
}
.comment-item:last-of-type { border-bottom: none; }

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.88rem;
  font-weight: 700;
  color: #1a1208;
  flex-shrink: 0;
}

.comment-avatar.sm { width: 28px; height: 28px; font-size: 0.76rem; }

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
  margin: 0 0 8px;
  font-size: 0.88rem;
  line-height: 1.65;
  color: var(--cream);
}

.comment-foot { display: flex; gap: 12px; }

.comment-act {
  background: none;
  border: none;
  color: var(--cream-muted);
  font-size: 0.8rem;
  cursor: pointer;
  padding: 0;
  font-family: inherit;
  transition: color 0.2s;
}
.comment-act:hover { color: var(--cream); }

.reply-item {
  display: flex;
  gap: 10px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

.load-more-btn {
  width: 100%;
  margin-top: 20px;
  padding: 12px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--cream-muted);
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s;
}
.load-more-btn:hover { color: var(--cream); border-color: var(--border-strong); }
</style>
