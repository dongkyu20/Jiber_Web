<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { favoritesApi } from '@/api/favorites'
import { getApiError } from '@/api/client'
import type { FavoriteApartmentItem, FavoriteAreaItem } from '@/api/types'
import EmptyState from '@/components/EmptyState.vue'
import { formatDate, formatKrw, transactionTypeLabel } from '@/utils/format'

type TabId = 'apartments' | 'areas' | 'alerts'
const tabs: { id: TabId; label: string }[] = [
  { id: 'apartments', label: '관심 단지' },
  { id: 'areas', label: '관심 지역' },
  { id: 'alerts', label: '가격 알림' },
]

const activeTab = ref<TabId>('apartments')
const apartments = ref<FavoriteApartmentItem[]>([])
const areas = ref<FavoriteAreaItem[]>([])
const loading = ref(false)
const deletingPropertyId = ref<number | null>(null)
const deletingAreaId = ref<number | null>(null)
const errorMessage = ref('')
const statusMessage = ref('')

async function fetchFavorites() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [aptRes, areaRes] = await Promise.all([favoritesApi.listApartments(), favoritesApi.listAreas()])
    apartments.value = aptRes.items
    areas.value = areaRes.items
  } catch {
    errorMessage.value = '즐겨찾기를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function areaMapQuery(item: FavoriteAreaItem): Record<string, string> {
  const q: Record<string, string> = { areaLabel: item.label }
  if (typeof item.centerLat === 'number' && typeof item.centerLng === 'number') {
    q.centerLat = String(item.centerLat)
    q.centerLng = String(item.centerLng)
  }
  if (typeof item.zoomLevel === 'number') q.zoomLevel = String(item.zoomLevel)
  return q
}

function formatAreaLocation(item: FavoriteAreaItem) {
  const t = [item.sido, item.sigungu, item.legalDong].filter(Boolean).join(' ')
  if (t) return t
  if (typeof item.centerLat === 'number') return `${item.centerLat.toFixed(4)}, ${item.centerLng?.toFixed(4)}`
  return '저장된 지도 영역'
}

async function removeApartment(propertyId: number) {
  deletingPropertyId.value = propertyId
  try {
    await favoritesApi.removeApartment(propertyId)
    statusMessage.value = '관심 단지에서 삭제했습니다.'
    await fetchFavorites()
  } catch (error) {
    const apiErr = getApiError(error)
    if (apiErr?.code === 'FAVORITE_NOT_FOUND') {
      apartments.value = apartments.value.filter(a => a.propertyId !== propertyId)
      statusMessage.value = '이미 삭제된 관심 단지입니다.'
    } else {
      errorMessage.value = '삭제하지 못했습니다.'
    }
  } finally {
    deletingPropertyId.value = null
  }
}

async function removeArea(favoriteAreaId: number) {
  deletingAreaId.value = favoriteAreaId
  try {
    await favoritesApi.removeArea(favoriteAreaId)
    statusMessage.value = '관심 지역에서 삭제했습니다.'
    await fetchFavorites()
  } catch {
    errorMessage.value = '삭제하지 못했습니다.'
  } finally {
    deletingAreaId.value = null
  }
}

onMounted(fetchFavorites)
</script>

<template>
  <div class="fav-page">
    <div class="fav-header">
      <h1 class="fav-title">관심목록</h1>
      <p class="fav-desc">저장한 단지의 시세 변동을 한눈에 확인하세요.</p>
    </div>

    <!-- Tabs -->
    <div class="fav-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        :class="['fav-tab', { active: activeTab === tab.id }]"
        @click="activeTab = tab.id"
      >
        {{ tab.label }}
        <span v-if="tab.id === 'apartments'" class="tab-count">{{ apartments.length }}</span>
        <span v-if="tab.id === 'areas'" class="tab-count">{{ areas.length }}</span>
      </button>
    </div>

    <p v-if="loading" class="loading-text">불러오는 중...</p>
    <p v-if="statusMessage" class="helper-text">{{ statusMessage }}</p>
    <p v-if="errorMessage" class="inline-error">{{ errorMessage }}</p>

    <!-- 관심 단지 tab -->
    <div v-if="activeTab === 'apartments'">
      <div v-if="apartments.length" class="fav-grid">
        <article
          v-for="item in apartments"
          :key="item.favoriteId"
          class="fav-card"
        >
          <div class="card-top">
            <span class="apt-badge">아파트</span>
            <button
              class="heart-btn"
              :disabled="deletingPropertyId === item.propertyId"
              @click="removeApartment(item.propertyId)"
              aria-label="관심 삭제"
            >♥</button>
          </div>
          <RouterLink :to="`/properties/${item.propertyId}`" class="card-body">
            <h3 class="card-name">{{ item.name }}</h3>
            <p class="card-addr">{{ item.address }}</p>
            <div class="card-price-row" v-if="item.latestTransaction">
              <span class="card-price">{{ formatKrw(item.latestTransaction.dealAmount) }}</span>
              <span class="card-tx">{{ transactionTypeLabel(item.latestTransaction.transactionType) }}</span>
            </div>
          </RouterLink>
          <div class="card-foot">
            <span class="card-date">저장 {{ formatDate(item.createdAt) }}</span>
            <RouterLink :to="`/properties/${item.propertyId}`" class="card-detail">상세 →</RouterLink>
          </div>
        </article>
      </div>
      <EmptyState
        v-else-if="!loading"
        title="관심 단지가 없습니다."
        description="지도나 상세 화면에서 관심 단지를 추가해 보세요."
      />
    </div>

    <!-- 관심 지역 tab -->
    <div v-if="activeTab === 'areas'">
      <div v-if="areas.length" class="fav-area-list">
        <article
          v-for="item in areas"
          :key="item.favoriteAreaId"
          class="area-card"
        >
          <div class="area-card-left">
            <span class="area-icon">📍</span>
            <div>
              <h3 class="area-name">{{ item.label }}</h3>
              <p class="area-loc">{{ formatAreaLocation(item) }}</p>
              <p class="area-meta">
                <span v-if="item.zoomLevel">지도 {{ item.zoomLevel }}단계 · </span>저장 {{ formatDate(item.createdAt) }}
              </p>
            </div>
          </div>
          <div class="area-card-actions">
            <RouterLink :to="{ path: '/map', query: areaMapQuery(item) }" class="area-map-btn">지도 보기</RouterLink>
            <button
              class="area-del-btn"
              :disabled="deletingAreaId === item.favoriteAreaId"
              @click="removeArea(item.favoriteAreaId)"
            >{{ deletingAreaId === item.favoriteAreaId ? '삭제 중' : '삭제' }}</button>
          </div>
        </article>
      </div>
      <EmptyState
        v-else-if="!loading"
        title="관심 지역이 없습니다."
        description="지도에서 현재 영역을 관심 지역으로 등록해 보세요."
      />
    </div>

    <!-- 가격 알림 tab -->
    <div v-if="activeTab === 'alerts'">
      <EmptyState
        title="가격 알림이 없습니다."
        description="관심 단지의 가격 변동 알림을 설정하면 여기에 표시됩니다."
      />
    </div>
  </div>
</template>

<style scoped>
.fav-page {}

.fav-header { margin-bottom: 20px; }

.fav-title {
  margin: 0 0 4px;
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--cream);
}

.fav-desc {
  margin: 0;
  color: var(--cream-muted);
  font-size: 0.88rem;
}

/* Tabs */
.fav-tabs {
  display: flex;
  gap: 4px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 24px;
}

.fav-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
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
.fav-tab:hover { color: var(--cream); }
.fav-tab.active { color: var(--gold); border-bottom-color: var(--gold); font-weight: 700; }

.tab-count {
  padding: 1px 7px;
  background: rgba(200,160,100,0.12);
  color: var(--gold);
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 700;
}

/* Apartment card grid */
.fav-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.fav-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: border-color 0.2s, transform 0.2s;
}
.fav-card:hover { border-color: var(--border-strong); transform: translateY(-2px); }

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.apt-badge {
  font-size: 0.7rem;
  background: rgba(200,160,100,0.12);
  color: var(--gold);
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 700;
}

.heart-btn {
  background: none;
  border: none;
  color: #e87a7a;
  font-size: 1.2rem;
  cursor: pointer;
  padding: 2px;
  transition: transform 0.15s;
}
.heart-btn:hover { transform: scale(1.2); }
.heart-btn:disabled { opacity: 0.5; }

.card-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  color: var(--cream);
}

.card-name {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: var(--cream);
  line-height: 1.3;
}

.card-addr {
  margin: 0;
  font-size: 0.78rem;
  color: var(--cream-muted);
}

.card-price-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}

.card-price {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--cream);
}

.card-tx {
  font-size: 0.76rem;
  color: var(--cream-muted);
  background: rgba(255,255,255,0.05);
  padding: 2px 7px;
  border-radius: 4px;
}

.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid var(--border);
}

.card-date { font-size: 0.76rem; color: var(--cream-muted); }
.card-detail { font-size: 0.8rem; color: var(--gold); font-weight: 600; }

/* Area list */
.fav-area-list { display: flex; flex-direction: column; gap: 12px; }

.area-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 18px 22px;
  transition: border-color 0.2s;
}
.area-card:hover { border-color: var(--border-strong); }

.area-card-left { display: flex; align-items: flex-start; gap: 14px; }
.area-icon { font-size: 1.2rem; margin-top: 2px; }

.area-name {
  margin: 0 0 3px;
  font-size: 1rem;
  font-weight: 700;
  color: var(--cream);
}

.area-loc {
  margin: 0 0 4px;
  font-size: 0.82rem;
  color: var(--cream-muted);
}

.area-meta { margin: 0; font-size: 0.76rem; color: var(--cream-muted); }

.area-card-actions { display: flex; gap: 8px; flex-shrink: 0; }

.area-map-btn {
  padding: 8px 16px;
  background: var(--gold);
  color: #1a1208;
  border-radius: 8px;
  font-size: 0.82rem;
  font-weight: 700;
  white-space: nowrap;
  transition: background 0.2s;
}
.area-map-btn:hover { background: var(--gold-light); }

.area-del-btn {
  padding: 8px 14px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--cream-muted);
  font-size: 0.82rem;
  cursor: pointer;
  font-family: inherit;
  white-space: nowrap;
  transition: all 0.2s;
}
.area-del-btn:hover:not(:disabled) { color: #e87a7a; border-color: rgba(232,122,122,0.3); }
.area-del-btn:disabled { opacity: 0.5; }

@media (max-width: 900px) { .fav-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 600px) { .fav-grid { grid-template-columns: 1fr; } .area-card { flex-direction: column; align-items: flex-start; } }
</style>
