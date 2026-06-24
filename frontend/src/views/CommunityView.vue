<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { noticesApi } from '@/api/notices'
import type { NoticeSummary } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { formatDate } from '@/utils/format'

const authStore = useAuthStore()

type Category = '전체' | '자유게시판' | '매매후기' | 'Q&A'

interface Post {
  id: number | string
  category: '공지' | '자유' | '매매후기' | 'Q&A'
  pinned?: boolean
  hot?: boolean
  title: string
  author: string
  createdAt: string
  viewCount: number
  commentCount: number
}

const activeTab = ref<Category>('전체')
const tabs: Category[] = ['전체', '자유게시판', '매매후기', 'Q&A']

const notices = ref<NoticeSummary[]>([])
const noticeError = ref('')

const mockPosts: Post[] = [
  { id: 1, category: '매매후기', hot: true, title: '반포 아크로리버파크 84m² 실거주 1년 후기 (장단점 솔직 정리)', author: '리버뷰', createdAt: '2026-06-21', viewCount: 3201, commentCount: 42 },
  { id: 2, category: '자유', hot: true, title: '요즘 마포 vs 성수 어디가 더 오를까요?', author: '김사누', createdAt: '2026-06-21', viewCount: 1547, commentCount: 88 },
  { id: 3, category: 'Q&A', title: 'AI 적정가가 호가보다 낮으면 협상 가능한가요?', author: '첫걸음이', createdAt: '2026-06-20', viewCount: 2033, commentCount: 31 },
  { id: 4, category: '자유', title: '헬리오시티 전세 살아본 솔직 후기', author: '송마루', createdAt: '2026-06-20', viewCount: 1206, commentCount: 19 },
  { id: 5, category: 'Q&A', title: 'SHAP 분석 보고 한강 고평가 판정받았는데, 이게 맞나요?', author: '데이터덕후', createdAt: '2026-06-19', viewCount: 954, commentCount: 12 },
  { id: 6, category: 'Q&A', title: '오피스텔도 적정가 추정이 되나요? 아파트만 되는건지', author: '오피스넬', createdAt: '2026-06-19', viewCount: 742, commentCount: 8 },
  { id: 7, category: '자유', title: '경희궁자이 학군 관련 정보 공유합니다', author: '공공주민', createdAt: '2026-06-13', viewCount: 1510, commentCount: 24 },
  { id: 8, category: '매매후기', title: '고덕그라시움 입주 6개월차 — 인프라 후기', author: '강동살이', createdAt: '2026-06-13', viewCount: 335, commentCount: 15 },
  { id: 9, category: '자유', title: '집er AI 가격예측 얼마나 믿을 수 있나요?', author: '부동산초보', createdAt: '2026-06-12', viewCount: 892, commentCount: 37 },
  { id: 10, category: '매매후기', title: '은마아파트 리모델링 기대감 - 실거주 3년 후기', author: '강남주민', createdAt: '2026-06-11', viewCount: 2140, commentCount: 56 },
]

const noticePosts = computed<Post[]>(() =>
  notices.value.map(n => ({
    id: `n-${n.noticeId}`,
    category: '공지' as const,
    pinned: n.pinned,
    title: n.title,
    author: '운영자',
    createdAt: n.publishedAt,
    viewCount: 0,
    commentCount: 0,
  }))
)

const filteredPosts = computed<Post[]>(() => {
  const pinned = noticePosts.value.filter(p => p.pinned)
  const unpinnedNotices = noticePosts.value.filter(p => !p.pinned)
  const communityPosts = activeTab.value === '전체'
    ? mockPosts
    : mockPosts.filter(p => {
        if (activeTab.value === '자유게시판') return p.category === '자유'
        return p.category === activeTab.value
      })
  return [...pinned, ...unpinnedNotices, ...communityPosts]
})

const hotPosts = computed(() => mockPosts.filter(p => p.hot).slice(0, 5))

const stats = { today: 128, total: 24910, active: 8402 }

const currentPage = ref(1)
const postsPerPage = 15
const totalPages = computed(() => Math.max(1, Math.ceil(filteredPosts.value.length / postsPerPage)))
const pagedPosts = computed(() => filteredPosts.value.slice((currentPage.value - 1) * postsPerPage, currentPage.value * postsPerPage))

function setTab(tab: Category) {
  activeTab.value = tab
  currentPage.value = 1
}

onMounted(async () => {
  try {
    const res = await noticesApi.list({ sort: 'publishedAt,desc', page: 0, size: 10 })
    notices.value = res.items
  } catch {
    noticeError.value = '공지사항을 불러오지 못했습니다.'
  }
})
</script>

<template>
  <div class="comm-wrap">
    <div class="comm-main">
      <!-- Header -->
      <div class="comm-header">
        <div>
          <h1 class="comm-title">커뮤니티</h1>
          <p class="comm-desc">실거주자의 생생한 이야기를 나눠요</p>
        </div>
        <RouterLink v-if="authStore.isAuthenticated" to="/community/write" class="write-btn">+ 글쓰기</RouterLink>
      </div>

      <!-- Tabs -->
      <div class="comm-tabs">
        <button
          v-for="tab in tabs"
          :key="tab"
          :class="['comm-tab', { active: activeTab === tab }]"
          @click="setTab(tab)"
        >{{ tab }}</button>
      </div>

      <!-- Table -->
      <p v-if="noticeError" class="notice-error">{{ noticeError }}</p>

      <div class="post-table">
        <div class="post-table-head">
          <span class="col-cat">분류</span>
          <span class="col-title">제목</span>
          <span class="col-author">작성자</span>
          <span class="col-date">작성일</span>
          <span class="col-view">조회</span>
        </div>

        <div v-for="post in pagedPosts" :key="post.id" class="post-row">
          <span class="col-cat">
            <span :class="['cat-badge', `cat-${post.category}`]">{{ post.category }}</span>
            <span v-if="post.pinned" class="pin-badge">고정</span>
            <span v-if="post.hot" class="hot-badge">HOT</span>
          </span>
          <span class="col-title">
            <RouterLink :to="`/community/${post.id}`" class="post-link">
              {{ post.title }}
              <span v-if="post.commentCount > 0" class="cmt-count">[{{ post.commentCount }}]</span>
            </RouterLink>
          </span>
          <span class="col-author">{{ post.author }}</span>
          <span class="col-date">{{ formatDate(post.createdAt) }}</span>
          <span class="col-view">{{ post.viewCount.toLocaleString('ko-KR') }}</span>
        </div>
      </div>

      <!-- Pagination -->
      <div class="pagination">
        <button
          v-for="p in totalPages"
          :key="p"
          :class="['page-btn', { active: currentPage === p }]"
          @click="currentPage = p"
        >{{ p }}</button>
      </div>
    </div>

    <!-- Sidebar -->
    <aside class="comm-sidebar">
      <div class="sidebar-block">
        <h3 class="sidebar-title">🔥 실시간 인기글</h3>
        <ol class="hot-list">
          <li v-for="(post, i) in hotPosts" :key="post.id">
            <span class="hot-rank">{{ i + 1 }}</span>
            <RouterLink :to="`/community/${post.id}`" class="hot-link">{{ post.title }}</RouterLink>
          </li>
        </ol>
      </div>

      <div class="sidebar-block">
        <h3 class="sidebar-title">커뮤니티 통계</h3>
        <div class="stat-row"><span>오늘 작성글</span><span>{{ stats.today }}</span></div>
        <div class="stat-row"><span>전체 게시글</span><span>{{ stats.total.toLocaleString() }}</span></div>
        <div class="stat-row"><span>활동 회원</span><span>{{ stats.active.toLocaleString() }}</span></div>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.comm-wrap {
  display: grid;
  grid-template-columns: 1fr 260px;
  gap: 28px;
  align-items: start;
}

.comm-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.comm-title {
  margin: 0 0 4px;
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--cream);
}

.comm-desc {
  margin: 0;
  color: var(--cream-muted);
  font-size: 0.88rem;
}

.write-btn {
  padding: 10px 20px;
  background: var(--gold);
  color: #1a1208;
  border: none;
  border-radius: 8px;
  font-size: 0.88rem;
  font-weight: 700;
  white-space: nowrap;
  transition: background 0.2s;
}
.write-btn:hover { background: var(--gold-light); }

/* Tabs */
.comm-tabs {
  display: flex;
  gap: 4px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 0;
}

.comm-tab {
  padding: 10px 18px;
  background: none;
  border: none;
  color: var(--cream-muted);
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: all 0.2s;
  font-family: inherit;
}

.comm-tab:hover { color: var(--cream); }
.comm-tab.active { color: var(--gold); border-bottom-color: var(--gold); font-weight: 700; }

.notice-error {
  margin: 0;
  padding: 10px 14px;
  border: 1px solid rgba(232, 80, 80, 0.2);
  background: rgba(232, 80, 80, 0.08);
  color: #e87a7a;
  font-size: 0.84rem;
}

/* Table */
.post-table {
  border: 1px solid var(--border);
  border-radius: 0 0 10px 10px;
  border-top: none;
  overflow: hidden;
}

.post-table-head {
  display: grid;
  grid-template-columns: 100px 1fr 90px 88px 60px;
  padding: 11px 16px;
  background: rgba(200,160,100,0.05);
  border-bottom: 1px solid var(--border);
  font-size: 0.78rem;
  color: var(--cream-muted);
  font-weight: 700;
  letter-spacing: 0.04em;
}

.post-row {
  display: grid;
  grid-template-columns: 100px 1fr 90px 88px 60px;
  padding: 13px 16px;
  border-bottom: 1px solid var(--border);
  align-items: center;
  transition: background 0.15s;
}
.post-row:last-child { border-bottom: none; }
.post-row:hover { background: rgba(200,160,100,0.04); }

.col-cat { display: flex; align-items: center; gap: 4px; flex-wrap: wrap; }
.col-title { min-width: 0; }
.col-author, .col-date, .col-view { font-size: 0.82rem; color: var(--cream-muted); text-align: center; }

.cat-badge {
  font-size: 0.7rem;
  padding: 2px 7px;
  border-radius: 4px;
  font-weight: 700;
  white-space: nowrap;
}

.cat-공지 { background: rgba(200,160,100,0.15); color: var(--gold); }
.cat-자유 { background: rgba(100,180,100,0.12); color: #7ac97a; }
.cat-매매후기 { background: rgba(100,150,220,0.12); color: #7ab0e0; }
.cat-Q\\&A { background: rgba(180,100,200,0.12); color: #c07ad0; }

.pin-badge, .hot-badge {
  font-size: 0.66rem;
  padding: 1px 5px;
  border-radius: 3px;
  font-weight: 700;
}

.pin-badge { background: rgba(200,160,100,0.12); color: var(--cream-muted); }
.hot-badge { background: rgba(220,80,80,0.15); color: #e87a7a; }

.post-link {
  color: var(--cream);
  font-size: 0.9rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
  transition: color 0.2s;
}
.post-link:hover { color: var(--gold); }

.cmt-count { color: var(--gold-dim); font-size: 0.82rem; margin-left: 4px; }

/* Pagination */
.pagination {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-top: 24px;
}

.page-btn {
  width: 36px;
  height: 36px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: transparent;
  color: var(--cream-muted);
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.page-btn:hover { color: var(--cream); border-color: var(--border-strong); }
.page-btn.active { background: var(--gold); color: #1a1208; border-color: var(--gold); }

/* Sidebar */
.comm-sidebar { display: flex; flex-direction: column; gap: 16px; }

.sidebar-block {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 18px 20px;
}

.sidebar-title {
  margin: 0 0 14px;
  font-size: 0.92rem;
  font-weight: 700;
  color: var(--cream);
}

.hot-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hot-list li { display: flex; align-items: flex-start; gap: 10px; }

.hot-rank {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--gold);
  min-width: 16px;
}

.hot-link {
  font-size: 0.82rem;
  color: var(--cream-muted);
  line-height: 1.45;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  transition: color 0.2s;
}
.hot-link:hover { color: var(--cream); }

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
  font-size: 0.84rem;
  color: var(--cream-muted);
}
.stat-row:last-child { border-bottom: none; }
.stat-row span:last-child { color: var(--cream); font-weight: 700; }

@media (max-width: 900px) {
  .comm-wrap { grid-template-columns: 1fr; }
  .comm-sidebar { order: -1; }
  .post-table-head, .post-row {
    grid-template-columns: 80px 1fr 70px;
  }
  .col-author, .col-date { display: none; }
  .col-view { display: none; }
}
</style>
