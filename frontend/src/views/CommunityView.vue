<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'

import { communityApi } from '@/api/community'
import type { CommunityCategory, CommunityPostSummary } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { formatDate } from '@/utils/format'

type CategoryTab = 'ALL' | CommunityCategory

const authStore = useAuthStore()

const categoryTabs: Array<{ value: CategoryTab; label: string }> = [
  { value: 'ALL', label: '전체' },
  { value: 'FREE', label: '자유게시판' },
  { value: 'DEAL_REVIEW', label: '매물 후기' },
  { value: 'QNA', label: 'Q&A' }
]

const sortOptions = [
  { value: 'createdAt,desc', label: '최신순' },
  { value: 'viewCount,desc', label: '조회순' },
  { value: 'commentCount,desc', label: '댓글순' }
] as const

const activeTab = ref<CategoryTab>('ALL')
const keyword = ref('')
const submittedKeyword = ref('')
const sort = ref<(typeof sortOptions)[number]['value']>('createdAt,desc')
const currentPage = ref(1)
const postsPerPage = 15
const posts = ref<CommunityPostSummary[]>([])
const totalPages = ref(1)
const totalElements = ref(0)
const loading = ref(false)
const errorMessage = ref('')

const categoryLabel = computed(() => Object.fromEntries(categoryTabs.map((tab) => [tab.value, tab.label])))

async function loadPosts() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await communityApi.listPosts({
      page: currentPage.value - 1,
      size: postsPerPage,
      sort: sort.value,
      keyword: submittedKeyword.value,
      category: activeTab.value === 'ALL' ? undefined : activeTab.value
    })
    posts.value = response.items
    totalPages.value = Math.max(1, response.page.totalPages || 1)
    totalElements.value = response.page.totalElements
  } catch {
    posts.value = []
    totalPages.value = 1
    totalElements.value = 0
    errorMessage.value = '커뮤니티 게시글을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function setTab(tab: CategoryTab) {
  activeTab.value = tab
  currentPage.value = 1
}

function submitSearch() {
  submittedKeyword.value = keyword.value.trim()
  currentPage.value = 1
}

function clearSearch() {
  keyword.value = ''
  submittedKeyword.value = ''
  currentPage.value = 1
}

watch([activeTab, sort, currentPage, submittedKeyword], () => {
  void loadPosts()
})

onMounted(loadPosts)
</script>

<template>
  <div class="comm-wrap">
    <div class="comm-main">
      <div class="comm-header">
        <div>
          <h1 class="comm-title">커뮤니티</h1>
          <p class="comm-desc">실거주 후기, 매물 경험, 주거 생활 정보를 나누는 공간입니다.</p>
        </div>
        <RouterLink v-if="authStore.isAuthenticated" to="/community/write" class="write-btn">글쓰기</RouterLink>
      </div>

      <div class="comm-tabs">
        <button
          v-for="tab in categoryTabs"
          :key="tab.value"
          :class="['comm-tab', { active: activeTab === tab.value }]"
          type="button"
          @click="setTab(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="comm-toolbar">
        <form class="comm-search" @submit.prevent="submitSearch">
          <input v-model="keyword" type="search" placeholder="제목이나 내용을 검색하세요" />
          <button type="submit">검색</button>
          <button v-if="submittedKeyword" class="ghost-btn" type="button" @click="clearSearch">초기화</button>
        </form>
        <select v-model="sort" class="comm-sort" aria-label="정렬">
          <option v-for="option in sortOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </div>

      <p v-if="errorMessage" class="notice-error">{{ errorMessage }}</p>
      <p v-else class="comm-count">
        총 {{ totalElements.toLocaleString('ko-KR') }}건
        <span v-if="submittedKeyword"> · 검색어 "{{ submittedKeyword }}"</span>
      </p>

      <div class="post-table">
        <div class="post-table-head">
          <span class="col-cat">분류</span>
          <span class="col-title">제목</span>
          <span class="col-author">작성자</span>
          <span class="col-date">작성일</span>
          <span class="col-view">조회</span>
        </div>

        <div v-if="loading" class="post-empty">게시글을 불러오는 중입니다.</div>
        <div v-else-if="!posts.length" class="post-empty">표시할 게시글이 없습니다.</div>

        <div v-for="post in posts" v-else :key="post.postId" class="post-row">
          <span class="col-cat">
            <span :class="['cat-badge', `cat-${post.category.toLowerCase()}`]">
              {{ categoryLabel[post.category] }}
            </span>
          </span>
          <span class="col-title">
            <RouterLink :to="`/community/${post.postId}`" class="post-link">
              {{ post.title }}
              <span v-if="post.commentCount > 0" class="cmt-count">[{{ post.commentCount }}]</span>
            </RouterLink>
          </span>
          <span class="col-author">{{ post.authorDisplayName ?? '탈퇴한 사용자' }}</span>
          <span class="col-date">{{ formatDate(post.createdAt) }}</span>
          <span class="col-view">{{ post.viewCount.toLocaleString('ko-KR') }}</span>
        </div>
      </div>

      <div v-if="totalPages > 1" class="pagination">
        <button class="page-btn" type="button" :disabled="currentPage === 1" @click="currentPage -= 1">이전</button>
        <button
          v-for="page in totalPages"
          :key="page"
          :class="['page-btn', { active: currentPage === page }]"
          type="button"
          @click="currentPage = page"
        >
          {{ page }}
        </button>
        <button class="page-btn" type="button" :disabled="currentPage === totalPages" @click="currentPage += 1">다음</button>
      </div>
    </div>

    <aside class="comm-sidebar">
      <div class="sidebar-block">
        <h3 class="sidebar-title">커뮤니티 안내</h3>
        <p class="sidebar-copy">실거주 후기, 매물 경험, 질문을 자유롭게 남겨주세요. 개인정보와 허위 매물 정보는 삭제될 수 있습니다.</p>
      </div>
      <div class="sidebar-block">
        <h3 class="sidebar-title">현재 게시글</h3>
        <div class="stat-row"><span>전체</span><span>{{ totalElements.toLocaleString('ko-KR') }}</span></div>
        <div class="stat-row"><span>현재 페이지</span><span>{{ currentPage }} / {{ totalPages }}</span></div>
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

.comm-desc,
.comm-count,
.sidebar-copy {
  margin: 0;
  color: var(--cream-muted);
  font-size: 0.88rem;
  line-height: 1.6;
}

.write-btn,
.comm-search button {
  padding: 10px 18px;
  background: var(--gold);
  color: #1a1208;
  border: none;
  border-radius: 8px;
  font-size: 0.88rem;
  font-weight: 700;
  white-space: nowrap;
  transition: background 0.2s;
}

.write-btn:hover,
.comm-search button:hover { background: var(--gold-light); }

.comm-tabs {
  display: flex;
  gap: 4px;
  border-bottom: 1px solid var(--border);
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
}

.comm-tab:hover { color: var(--cream); }
.comm-tab.active { color: var(--gold); border-bottom-color: var(--gold); font-weight: 700; }

.comm-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin: 16px 0;
}

.comm-search {
  display: flex;
  gap: 8px;
  align-items: center;
  flex: 1;
}

.comm-search input { min-width: 0; }

.comm-search .ghost-btn {
  border: 1px solid var(--border);
  background: transparent;
  color: var(--cream-muted);
}

.comm-sort { width: 120px; }

.notice-error {
  margin: 0 0 12px;
  padding: 10px 14px;
  border: 1px solid rgba(232, 80, 80, 0.2);
  background: rgba(232, 80, 80, 0.08);
  color: #e87a7a;
  font-size: 0.84rem;
}

.post-table {
  margin-top: 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  overflow: hidden;
}

.post-table-head,
.post-row {
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr) 108px 112px 64px;
  align-items: center;
  gap: 10px;
}

.post-table-head {
  padding: 11px 16px;
  background: rgba(200,160,100,0.05);
  border-bottom: 1px solid var(--border);
  font-size: 0.78rem;
  color: var(--cream-muted);
  font-weight: 700;
  letter-spacing: 0.04em;
}

.post-row {
  padding: 13px 16px;
  border-bottom: 1px solid var(--border);
}
.post-row:last-child { border-bottom: none; }
.post-row:hover { background: rgba(200,160,100,0.04); }

.post-empty {
  padding: 34px 16px;
  color: var(--cream-muted);
  text-align: center;
}

.cat-badge {
  font-size: 0.7rem;
  padding: 3px 8px;
  border-radius: 4px;
  font-weight: 700;
  white-space: nowrap;
}

.cat-free { background: rgba(100,180,100,0.12); color: #7ac97a; }
.cat-deal_review { background: rgba(100,150,220,0.12); color: #7ab0e0; }
.cat-qna { background: rgba(180,100,200,0.12); color: #c07ad0; }

.col-title { min-width: 0; }
.col-author, .col-date, .col-view {
  color: var(--cream-muted);
  font-size: 0.82rem;
  text-align: center;
}

.post-link {
  color: var(--cream);
  font-size: 0.9rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}
.post-link:hover { color: var(--gold); }

.cmt-count { color: var(--gold-dim); font-size: 0.82rem; margin-left: 4px; }

.pagination {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-top: 24px;
}

.page-btn {
  min-width: 36px;
  height: 36px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: transparent;
  color: var(--cream-muted);
  font-size: 0.86rem;
  font-weight: 600;
}
.page-btn:hover:not(:disabled) { color: var(--cream); border-color: var(--border-strong); }
.page-btn.active { background: var(--gold); color: #1a1208; border-color: var(--gold); }

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
  .comm-toolbar { align-items: stretch; flex-direction: column; }
  .comm-search { flex-wrap: wrap; }
  .comm-sort { width: 100%; }
  .post-table-head, .post-row {
    grid-template-columns: 88px minmax(0, 1fr) 60px;
  }
  .col-author, .col-date { display: none; }
}
</style>
