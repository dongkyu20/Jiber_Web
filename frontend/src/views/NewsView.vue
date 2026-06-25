<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { getApiErrorMessage } from '@/api/client'
import { newsApi } from '@/api/news'
import type { NewsFeedItem } from '@/api/types'
import EmptyState from '@/components/EmptyState.vue'
import { formatDate } from '@/utils/format'

const DEFAULT_KEYWORD = '부동산'
const FEED_SIZE = 20
const topicKeywords = ['부동산', '아파트', '전세', '재건축', '청약']

const keyword = ref(DEFAULT_KEYWORD)
const searchedKeyword = ref(DEFAULT_KEYWORD)
const items = ref<NewsFeedItem[]>([])
const loading = ref(false)
const errorMessage = ref('')
const feedMessage = ref('')
const available = ref(true)

const hasItems = computed(() => items.value.length > 0)

async function fetchNews(nextKeyword = keyword.value) {
  const trimmedKeyword = nextKeyword.trim() || DEFAULT_KEYWORD
  keyword.value = trimmedKeyword
  searchedKeyword.value = trimmedKeyword
  loading.value = true
  errorMessage.value = ''
  feedMessage.value = ''

  try {
    const response = await newsApi.search({ query: trimmedKeyword, display: FEED_SIZE })
    available.value = response.available
    feedMessage.value = response.message
    items.value = response.items
  } catch (error) {
    available.value = false
    items.value = []
    errorMessage.value = getApiErrorMessage(error, '최신 뉴스를 불러오지 못했습니다.')
  } finally {
    loading.value = false
  }
}

function selectTopic(topic: string) {
  void fetchNews(topic)
}

function formatNewsDate(value?: string | null) {
  return formatDate(value)
}

onMounted(() => {
  void fetchNews(DEFAULT_KEYWORD)
})
</script>

<template>
  <section class="news-heading">
    <div>
      <p class="eyebrow">NAVER NEWS</p>
      <h1>최신 부동산 뉴스</h1>
      <p>네이버 뉴스 검색 결과를 최신순으로 모아 보여드립니다.</p>
    </div>
    <span class="news-source">네이버 뉴스</span>
  </section>

  <form class="news-search" @submit.prevent="fetchNews()">
    <label class="visually-hidden" for="news-keyword">뉴스 검색어</label>
    <input
      id="news-keyword"
      v-model="keyword"
      type="search"
      placeholder="부동산, 아파트, 전세..."
      autocomplete="off"
    />
    <button class="primary-button" type="submit" :disabled="loading">
      {{ loading ? '검색 중' : '검색' }}
    </button>
  </form>

  <div class="topic-tabs" aria-label="뉴스 주제">
    <button
      v-for="topic in topicKeywords"
      :key="topic"
      type="button"
      :class="['topic-tab', { active: searchedKeyword === topic }]"
      :disabled="loading"
      @click="selectTopic(topic)"
    >
      {{ topic }}
    </button>
  </div>

  <p v-if="errorMessage" class="inline-error">{{ errorMessage }}</p>
  <p v-else-if="!available && feedMessage" class="inline-error">{{ feedMessage }}</p>

  <div class="feed-meta">
    <strong>{{ searchedKeyword }}</strong>
    <span>{{ hasItems ? `${items.length}개 기사` : '검색 결과 없음' }}</span>
  </div>

  <div v-if="loading" class="news-loading" aria-live="polite">
    <span />
    <span />
    <span />
  </div>

  <div v-else-if="hasItems" class="news-feed">
    <a
      v-for="item in items"
      :key="`${item.naverLink}-${item.publishedAt}`"
      class="news-card"
      :href="item.naverLink"
      target="_blank"
      rel="noopener noreferrer"
    >
      <div class="news-card-top">
        <span class="news-publisher">{{ item.source }}</span>
        <time v-if="item.publishedAt" :datetime="item.publishedAt">{{ formatNewsDate(item.publishedAt) }}</time>
      </div>
      <h2>{{ item.title }}</h2>
      <p>{{ item.summary }}</p>
      <span class="news-link-label">네이버 뉴스에서 보기</span>
    </a>
  </div>

  <EmptyState
    v-else
    title="뉴스 검색 결과가 없습니다."
    description="다른 부동산 키워드로 다시 검색해 보세요."
  />
</template>

<style scoped>
.news-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 22px;
}

.news-heading h1 {
  margin: 0;
  color: var(--cream);
  font-size: clamp(1.8rem, 3.2vw, 2.6rem);
  line-height: 1.18;
}

.news-heading p:not(.eyebrow) {
  max-width: 620px;
  margin: 10px 0 0;
  color: var(--cream-muted);
  font-size: 0.92rem;
  line-height: 1.7;
}

.news-source {
  display: inline-flex;
  min-height: 34px;
  align-items: center;
  padding: 7px 12px;
  border: 1px solid var(--border);
  border-radius: 999px;
  color: var(--gold);
  font-size: 0.78rem;
  font-weight: 800;
  white-space: nowrap;
}

.news-search {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  margin-bottom: 12px;
}

.topic-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.topic-tab {
  min-height: 34px;
  padding: 7px 14px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: transparent;
  color: var(--cream-muted);
  font-size: 0.82rem;
  font-weight: 700;
  transition: all 0.2s;
}

.topic-tab:hover:not(:disabled) {
  color: var(--cream);
  border-color: var(--border-strong);
}

.topic-tab.active {
  border-color: var(--gold);
  background: rgba(200, 160, 100, 0.12);
  color: var(--gold);
}

.feed-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 0 0 12px;
  color: var(--cream-muted);
  font-size: 0.84rem;
}

.feed-meta strong {
  color: var(--cream);
  font-size: 0.92rem;
}

.news-feed {
  display: grid;
  gap: 10px;
}

.news-card {
  display: grid;
  gap: 8px;
  padding: 18px 20px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  transition:
    background 0.2s,
    border-color 0.2s,
    transform 0.2s;
}

.news-card:hover {
  border-color: var(--border-strong);
  background: var(--bg-card-hover);
  transform: translateY(-1px);
}

.news-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: var(--cream-muted);
  font-size: 0.78rem;
}

.news-publisher {
  color: var(--gold);
  font-weight: 800;
  overflow-wrap: anywhere;
}

.news-card h2 {
  margin: 0;
  color: var(--cream);
  font-size: 1.02rem;
  line-height: 1.45;
}

.news-card p {
  margin: 0;
  color: var(--cream-muted);
  font-size: 0.88rem;
  line-height: 1.7;
}

.news-link-label {
  color: var(--gold-dim);
  font-size: 0.82rem;
  font-weight: 800;
}

.news-loading {
  display: grid;
  gap: 10px;
}

.news-loading span {
  height: 112px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background:
    linear-gradient(90deg, transparent, rgba(200, 160, 100, 0.08), transparent),
    var(--bg-card);
  background-size: 220px 100%, 100% 100%;
  animation: shimmer 1.2s infinite linear;
}

@keyframes shimmer {
  from { background-position: -220px 0, 0 0; }
  to { background-position: calc(100% + 220px) 0, 0 0; }
}

@media (max-width: 720px) {
  .news-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .news-search {
    grid-template-columns: 1fr;
  }

  .news-card-top {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
