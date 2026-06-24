<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { favoritesApi } from '@/api/favorites'
import { propertyApi } from '@/api/property'
import { getApiError } from '@/api/client'
import type { PropertyDetail, ShapValue, ValuationResponse } from '@/api/types'
import ShapChart from '@/charts/ShapChart.vue'
import TransactionChart from '@/charts/TransactionChart.vue'
import EmptyState from '@/components/EmptyState.vue'
import FloatingChat from '@/components/FloatingChat.vue'
import { useAuthStore } from '@/stores/auth'
import { useChatContextStore } from '@/stores/chatContext'
import { formatDate, formatKrw, propertyTypeLabel, transactionTypeLabel } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const chatContextStore = useChatContextStore()

const property = ref<PropertyDetail | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const aiMessage = ref('')
const favoriteMessage = ref('')
const favoriteErrorMessage = ref('')
const favoriteUpdating = ref(false)
const valuation = ref<ValuationResponse | null>(null)
const shapValues = ref<ShapValue[]>([])

const propertyId = computed(() => String(route.params.propertyId))
const aiUnavailableMessage = '아파트 단지에 한해 제공되는 기능입니다.'
const latestTransaction = computed(() => {
  if (!property.value?.transactions.length) return null
  return (
    property.value.transactions.find(t => t.dealDate === property.value?.summary.latestDealDate) ??
    property.value.transactions[0]
  )
})
const recentTransactionCount = computed(() => property.value?.transactions.length ?? 0)
const isApartmentFavorite = computed(() => Boolean(property.value?.favorite?.apartmentFavorited))
const canRequestAi = computed(() =>
  Boolean(authStore.isAuthenticated && property.value?.propertyType === 'APARTMENT' &&
    property.value.ai.valuationAvailable && property.value.ai.shapAvailable)
)
const canAskChat = computed(() => Boolean(property.value && valuation.value && shapValues.value.length))

function setApartmentFavorite(next: boolean) {
  if (!property.value) return
  property.value.favorite = { apartmentFavorited: next, areaFavorited: property.value.favorite?.areaFavorited ?? false }
}

function setFavoriteFailure(error: unknown, fallback: string) {
  const apiErr = getApiError(error)
  if (apiErr?.code === 'FAVORITE_ALREADY_EXISTS') { setApartmentFavorite(true); favoriteMessage.value = '이미 관심 단지에 저장되어 있습니다.'; return }
  if (apiErr?.code === 'FAVORITE_NOT_FOUND') { setApartmentFavorite(false); favoriteMessage.value = '이미 삭제된 관심 단지입니다.'; return }
  if (apiErr?.code === 'PROPERTY_NOT_FOUND') { favoriteErrorMessage.value = '부동산 정보를 찾을 수 없습니다.'; return }
  if (apiErr?.code === 'AUTH_REQUIRED') { favoriteErrorMessage.value = '로그인이 필요한 기능입니다.'; return }
  favoriteErrorMessage.value = fallback
}

async function fetchProperty() {
  loading.value = true
  errorMessage.value = ''
  try {
    property.value = await propertyApi.getProperty(propertyId.value)
    // Auto-inject context so FloatingChat has property info
    if (property.value) {
      chatContextStore.setPropertyAnalysisContext(property.value, null as any, [])
    }
  } catch {
    errorMessage.value = '부동산 상세 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function requestAi() {
  aiMessage.value = ''
  valuation.value = null
  shapValues.value = []
  if (!authStore.isAuthenticated) { aiMessage.value = '로그인이 필요한 기능입니다.'; return }
  if (!canRequestAi.value) { aiMessage.value = aiUnavailableMessage; return }
  const cur = property.value
  if (!cur) return
  try {
    const payload = { exclusiveAreaM2: 84.95, floor: 15, asOfDate: new Date().toISOString().slice(0, 10) }
    valuation.value = await propertyApi.requestValuation(propertyId.value, payload)
    const shap = await propertyApi.requestShap(propertyId.value, payload)
    shapValues.value = shap.values
    aiMessage.value = valuation.value.message || shap.message
    chatContextStore.setPropertyAnalysisContext(cur, valuation.value, shapValues.value)
  } catch {
    aiMessage.value = '추정가와 SHAP 요인을 불러오지 못했습니다.'
  }
}

async function toggleFavorite() {
  favoriteMessage.value = ''
  favoriteErrorMessage.value = ''
  if (!property.value) return
  if (!authStore.isAuthenticated) { favoriteMessage.value = '로그인 후 관심 단지를 저장할 수 있습니다.'; return }
  if (property.value.propertyType !== 'APARTMENT') { favoriteMessage.value = '아파트 단지만 저장할 수 있습니다.'; return }
  favoriteUpdating.value = true
  try {
    if (isApartmentFavorite.value) {
      await favoritesApi.removeApartment(property.value.propertyId)
      setApartmentFavorite(false)
      favoriteMessage.value = '관심 단지에서 삭제했습니다.'
    } else {
      await favoritesApi.addApartment(property.value.propertyId)
      setApartmentFavorite(true)
      favoriteMessage.value = '관심 단지에 추가했습니다.'
    }
  } catch (error) {
    setFavoriteFailure(error, '관심 단지 상태를 변경하지 못했습니다.')
  } finally {
    favoriteUpdating.value = false
  }
}

async function askChat() {
  if (property.value && valuation.value) {
    chatContextStore.setPropertyAnalysisContext(property.value, valuation.value, shapValues.value)
  }
  await router.push({ name: 'chat', query: { q: `${property.value?.name ?? '이 매물'}의 가격 예측이 왜 이렇게 나왔는지 설명해줘` } })
}

onMounted(fetchProperty)
</script>

<template>
  <div class="detail-wrap">
    <p v-if="loading" class="loading-text">상세 정보를 불러오는 중...</p>
    <p v-if="errorMessage" class="inline-error">{{ errorMessage }}</p>

    <template v-if="property">
      <!-- Breadcrumb / heading -->
      <div class="detail-heading">
        <p class="eyebrow">{{ propertyTypeLabel(property.propertyType) }}</p>
        <h1 class="detail-name">{{ property.name }}</h1>
        <p class="detail-addr">{{ property.address.sido }} {{ property.address.sigungu }} {{ property.address.legalDong }}</p>

        <!-- Favorite button -->
        <button
          class="fav-toggle"
          :class="{ favorited: isApartmentFavorite }"
          data-test="apartment-favorite-button"
          type="button"
          :disabled="favoriteUpdating"
          @click="toggleFavorite"
        >
          {{ favoriteUpdating ? '처리 중...' : isApartmentFavorite ? '♥ 관심 단지' : '♡ 관심 단지 추가' }}
        </button>
        <p v-if="favoriteMessage" class="helper-text">{{ favoriteMessage }}</p>
        <p v-if="favoriteErrorMessage" class="inline-error">{{ favoriteErrorMessage }}</p>
      </div>

      <!-- Summary + AI -->
      <div class="detail-grid">
        <article class="info-panel">
          <h2>단지 요약</h2>
          <dl class="summary-list">
            <div>
              <dt>준공연도</dt>
              <dd>{{ property.summary.builtYear ?? '정보 없음' }}</dd>
            </div>
            <div>
              <dt>세대수</dt>
              <dd>{{ property.summary.householdCount?.toLocaleString('ko-KR') ?? '정보 없음' }}</dd>
            </div>
            <div>
              <dt>최근 거래금액</dt>
              <dd class="gold-text">{{ formatKrw(property.summary.latestDealAmount) }}</dd>
            </div>
            <div>
              <dt>거래유형</dt>
              <dd>{{ latestTransaction ? transactionTypeLabel(latestTransaction.transactionType) : '정보 없음' }}</dd>
            </div>
            <div>
              <dt>최근 거래일</dt>
              <dd>{{ formatDate(property.summary.latestDealDate) }}</dd>
            </div>
            <div>
              <dt>거래 건수</dt>
              <dd>최근 {{ recentTransactionCount.toLocaleString() }}건</dd>
            </div>
          </dl>
        </article>

        <article class="info-panel ai-panel">
          <h2>AI 분석</h2>
          <p v-if="!authStore.isAuthenticated" class="muted">로그인 후 추정가와 SHAP 요인을 확인할 수 있습니다.</p>
          <p v-else-if="!canRequestAi" class="muted">{{ aiUnavailableMessage }}</p>
          <p v-else class="muted">실거래 데이터 기반 헤도닉 모델로 적정가와 SHAP 가격 요인을 분석합니다.</p>

          <button class="primary-button" type="button" @click="requestAi">
            추정가와 요인 보기
          </button>

          <p v-if="aiMessage" class="helper-text">{{ aiMessage }}</p>

          <div v-if="valuation?.estimatedPrice" class="ai-result">
            <p class="ai-label">AI 추정가</p>
            <p class="ai-price">{{ formatKrw(valuation.estimatedPrice) }}</p>
          </div>

          <button v-if="canAskChat" class="secondary-button" type="button" @click="askChat">
            챗봇으로 요인 설명 보기
          </button>
        </article>
      </div>

      <!-- Charts -->
      <div class="chart-grid">
        <article class="info-panel">
          <h2>거래 추이</h2>
          <TransactionChart :transactions="property.transactions" />
        </article>
        <article class="info-panel">
          <h2>SHAP 가격 요인</h2>
          <ShapChart :values="shapValues" />
        </article>
      </div>
    </template>

    <EmptyState
      v-if="!loading && !property && !errorMessage"
      title="상세 정보를 기다리고 있습니다."
      description="부동산 상세 API가 연결되면 기본 정보와 거래 내역을 표시합니다."
    />
  </div>

  <FloatingChat />
</template>

<style scoped>
.detail-wrap {}

.detail-heading {
  margin-bottom: 28px;
}

.detail-name {
  margin: 0 0 6px;
  font-size: clamp(1.6rem, 3vw, 2.2rem);
  font-weight: 700;
  color: var(--cream);
  line-height: 1.2;
}

.detail-addr {
  margin: 0 0 16px;
  color: var(--cream-muted);
  font-size: 0.92rem;
}

.fav-toggle {
  padding: 10px 22px;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background: transparent;
  color: var(--cream-muted);
  font-size: 0.9rem;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s;
}
.fav-toggle:hover { color: #e87a7a; border-color: rgba(232,122,122,0.3); }
.fav-toggle.favorited { color: #e87a7a; border-color: rgba(232,122,122,0.3); background: rgba(232,122,122,0.07); }
.fav-toggle:disabled { opacity: 0.5; }

.ai-panel { display: flex; flex-direction: column; gap: 12px; }

.ai-result {
  padding: 16px;
  background: rgba(200,160,100,0.08);
  border: 1px solid rgba(200,160,100,0.2);
  border-radius: 10px;
}

.ai-label { margin: 0 0 4px; font-size: 0.76rem; color: var(--cream-muted); letter-spacing: 0.1em; }
.ai-price { margin: 0; font-size: 1.5rem; font-weight: 700; color: var(--gold-light); }

.gold-text { color: var(--gold); }
</style>
