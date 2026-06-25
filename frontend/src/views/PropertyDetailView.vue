<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { favoritesApi } from '@/api/favorites'
import { propertyApi } from '@/api/property'
import { getApiError } from '@/api/client'
import type {
  PropertyDetail,
  PropertyTransaction,
  ShapValue,
  TransactionType,
  ValuationRequest,
  ValuationResponse
} from '@/api/types'
import ShapChart from '@/charts/ShapChart.vue'
import EmptyState from '@/components/EmptyState.vue'
import { useAuthStore } from '@/stores/auth'
import { useChatContextStore } from '@/stores/chatContext'
import { formatDate, formatKrw, propertyTypeLabel, transactionTypeLabel } from '@/utils/format'

type TransactionSortKey = 'dealDate' | 'transactionType' | 'dealAmount' | 'exclusiveAreaM2' | 'floor'
type SortDirection = 'asc' | 'desc'

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
const aiLoading = ref(false)
const valuation = ref<ValuationResponse | null>(null)
const shapValues = ref<ShapValue[]>([])
const selectedAiTransaction = ref<PropertyTransaction | null>(null)
const transactionTypeOptions: TransactionType[] = ['SALE', 'JEONSE', 'MONTHLY_RENT']
const selectedTransactionTypes = ref<TransactionType[]>([...transactionTypeOptions])
const transactionPage = ref(1)
const transactionsPerPage = 5
const transactionSort = ref<{ key: TransactionSortKey; direction: SortDirection }>({
  key: 'dealDate',
  direction: 'desc'
})

const propertyId = computed(() => String(route.params.propertyId))
const aiUnavailableMessage = '아파트 단지에 한해 제공되는 기능입니다.'
const latestTransaction = computed(() => {
  if (!property.value?.transactions.length) {
    return null
  }

  return (
    property.value.transactions.find((transaction) => transaction.dealDate === property.value?.summary.latestDealDate) ??
    property.value.transactions[0]
  )
})
const recentTransactionCount = computed(() => property.value?.transactions.length ?? 0)
const filteredTransactions = computed(() => {
  const selectedTypes = new Set(selectedTransactionTypes.value)
  return property.value?.transactions.filter((transaction) => selectedTypes.has(transaction.transactionType)) ?? []
})
const sortedTransactions = computed(() => {
  return [...filteredTransactions.value].sort((left, right) => {
    return compareTransactions(left, right, transactionSort.value.key, transactionSort.value.direction)
  })
})
const transactionTotalPages = computed(() => Math.max(1, Math.ceil(sortedTransactions.value.length / transactionsPerPage)))
const transactionPageStart = computed(() => (transactionPage.value - 1) * transactionsPerPage)
const pagedTransactions = computed(() =>
  sortedTransactions.value.slice(transactionPageStart.value, transactionPageStart.value + transactionsPerPage)
)
const transactionVisibleStart = computed(() =>
  sortedTransactions.value.length ? transactionPageStart.value + 1 : 0
)
const transactionVisibleEnd = computed(() =>
  Math.min(transactionPageStart.value + pagedTransactions.value.length, sortedTransactions.value.length)
)
const isApartmentFavorite = computed(() => Boolean(property.value?.favorite?.apartmentFavorited))
const canRequestAi = computed(() => {
  return Boolean(
    authStore.isAuthenticated &&
      property.value?.propertyType === 'APARTMENT' &&
      (property.value.ai.valuationAvailable || property.value.ai.shapAvailable)
  )
})
const canAskChatAboutAnalysis = computed(() => Boolean(property.value && valuation.value && shapValues.value.length))
const aiInputTransaction = computed(() => {
  const transactions = property.value?.transactions ?? []
  if (!transactions.length) {
    return null
  }

  if (isUsableAiTransaction(selectedAiTransaction.value)) {
    return selectedAiTransaction.value
  }

  return transactions.find(isUsableAiTransaction) ?? null
})
const aiInputSummary = computed(() => {
  const transaction = aiInputTransaction.value
  if (!transaction) {
    return ''
  }

  return `${formatDate(transaction.dealDate)} 매매 · ${formatArea(transaction.exclusiveAreaM2)} · ${formatFloor(transaction.floor)} 기준`
})

function transactionKey(transaction: PropertyTransaction) {
  return transaction.transactionId ?? `${transaction.dealDate}-${transaction.transactionType}-${transaction.floor}`
}

function setTransactionSort(key: TransactionSortKey) {
  transactionSort.value =
    transactionSort.value.key === key
      ? {
          key,
          direction: transactionSort.value.direction === 'asc' ? 'desc' : 'asc'
        }
      : {
          key,
          direction: key === 'dealDate' ? 'desc' : 'asc'
        }
  transactionPage.value = 1
}

function goToTransactionPage(page: number) {
  transactionPage.value = Math.min(Math.max(page, 1), transactionTotalPages.value)
}

watch(selectedTransactionTypes, () => {
  transactionPage.value = 1
})

watch(transactionTotalPages, (totalPages) => {
  if (transactionPage.value > totalPages) {
    transactionPage.value = totalPages
  }
})

function transactionSortAria(key: TransactionSortKey) {
  if (transactionSort.value.key !== key) {
    return 'none'
  }

  return transactionSort.value.direction === 'asc' ? 'ascending' : 'descending'
}

function transactionSortIndicator(key: TransactionSortKey) {
  if (transactionSort.value.key !== key) {
    return ''
  }

  return transactionSort.value.direction === 'asc' ? '▲' : '▼'
}

function compareTransactions(
  left: PropertyTransaction,
  right: PropertyTransaction,
  key: TransactionSortKey,
  direction: SortDirection
) {
  const leftValue = transactionSortValue(left, key)
  const rightValue = transactionSortValue(right, key)

  if (leftValue === null && rightValue === null) {
    return 0
  }
  if (leftValue === null) {
    return 1
  }
  if (rightValue === null) {
    return -1
  }
  const comparison =
    typeof leftValue === 'string' && typeof rightValue === 'string'
      ? leftValue.localeCompare(rightValue, 'ko-KR')
      : Number(leftValue) - Number(rightValue)

  return direction === 'asc' ? comparison : comparison * -1
}

function transactionSortValue(
  transaction: PropertyTransaction,
  key: TransactionSortKey
): number | string | null {
  if (key === 'dealDate') {
    const parsed = Date.parse(transaction.dealDate)
    return Number.isNaN(parsed) ? null : parsed
  }
  if (key === 'transactionType') {
    return transactionTypeLabel(transaction.transactionType)
  }
  if (key === 'dealAmount') {
    return representativeTransactionAmount(transaction)
  }

  return transaction[key] ?? null
}

function representativeTransactionAmount(transaction: PropertyTransaction) {
  return transaction.dealAmount ?? transaction.depositAmount ?? null
}

function isUsableAiTransaction(transaction?: PropertyTransaction | null): transaction is PropertyTransaction {
  return Boolean(
    transaction &&
      transaction.transactionType === 'SALE' &&
      transaction.exclusiveAreaM2 !== undefined &&
      transaction.exclusiveAreaM2 !== null &&
      transaction.exclusiveAreaM2 > 0 &&
      transaction.floor !== undefined &&
      transaction.floor !== null
  )
}

function isSelectedAiTransaction(transaction: PropertyTransaction) {
  const selected = aiInputTransaction.value
  return Boolean(selected && transactionKey(selected) === transactionKey(transaction))
}

function buildAiRequestPayload(): ValuationRequest | null {
  const transaction = aiInputTransaction.value
  if (!transaction?.exclusiveAreaM2 || transaction.floor === undefined || transaction.floor === null) {
    return null
  }

  return {
    exclusiveAreaM2: transaction.exclusiveAreaM2,
    floor: transaction.floor,
    asOfDate: new Date().toISOString().slice(0, 10)
  }
}

function aiRequestFailureMessage(error: unknown) {
  const apiError = getApiError(error)

  if (apiError?.code === 'AUTH_REQUIRED') {
    return '로그인이 필요한 기능입니다.'
  }
  if (apiError?.code === 'VALUATION_INSUFFICIENT_DATA') {
    return '추론에 필요한 단지 정보가 부족합니다. 면적, 층수, 준공연도, 위치 정보가 연결됐는지 확인해 주세요.'
  }
  if (apiError?.code === 'MODEL_SERVER_UNAVAILABLE') {
    return '모델 서버가 응답하지 않습니다. docker compose에서 model-server 상태를 확인해 주세요.'
  }
  if (apiError?.code === 'PROPERTY_NOT_FOUND') {
    return '부동산 정보를 찾을 수 없습니다.'
  }

  return '추정가와 SHAP 요인을 아직 불러오지 못했습니다. 로그인 상태와 백엔드 API를 확인해 주세요.'
}

function formatTransactionAmount(transaction: PropertyTransaction) {
  if (transaction.transactionType === 'MONTHLY_RENT') {
    const hasDeposit = transaction.depositAmount !== undefined && transaction.depositAmount !== null
    const hasMonthlyRent = transaction.monthlyRent !== undefined && transaction.monthlyRent !== null

    if (!hasDeposit && !hasMonthlyRent) {
      return formatKrw(null)
    }

    if (!hasDeposit) {
      return `월세 ${formatMonthlyRent(transaction.monthlyRent)}`
    }

    if (!hasMonthlyRent || transaction.monthlyRent === 0) {
      return `보증금 ${formatKrw(transaction.depositAmount)}`
    }

    return `보증금 ${formatKrw(transaction.depositAmount)} / 월세 ${formatMonthlyRent(transaction.monthlyRent)}`
  }

  return formatKrw(representativeTransactionAmount(transaction))
}

function formatMonthlyRent(value?: number | null) {
  if (value === undefined || value === null) {
    return formatKrw(value)
  }

  if (value >= 100000000) {
    return formatKrw(value)
  }

  if (value >= 10000) {
    const man = value / 10000
    return `${man.toLocaleString('ko-KR', { maximumFractionDigits: 1 })}만 원`
  }

  return `${value.toLocaleString('ko-KR')}원`
}

function formatArea(value?: number | null) {
  if (value === undefined || value === null) {
    return '정보 없음'
  }

  return `${value.toLocaleString('ko-KR', { maximumFractionDigits: 2 })}㎡`
}

function formatFloor(value?: number | null) {
  if (value === undefined || value === null) {
    return '정보 없음'
  }

  return `${value}층`
}

function setApartmentFavorite(nextValue: boolean) {
  if (!property.value) {
    return
  }

  property.value.favorite = {
    apartmentFavorited: nextValue,
    areaFavorited: property.value.favorite?.areaFavorited ?? false
  }
}

function setFavoriteFailure(error: unknown, fallbackMessage: string) {
  const apiError = getApiError(error)

  if (apiError?.code === 'FAVORITE_ALREADY_EXISTS') {
    setApartmentFavorite(true)
    favoriteMessage.value = '이미 관심 아파트에 저장되어 있습니다.'
    return
  }

  if (apiError?.code === 'FAVORITE_NOT_FOUND') {
    setApartmentFavorite(false)
    favoriteMessage.value = '이미 삭제된 관심 아파트입니다.'
    return
  }

  if (apiError?.code === 'PROPERTY_NOT_FOUND') {
    favoriteErrorMessage.value = '부동산 정보를 찾을 수 없습니다.'
    return
  }

  if (apiError?.code === 'AUTH_REQUIRED') {
    favoriteErrorMessage.value = '로그인이 필요한 기능입니다.'
    return
  }

  favoriteErrorMessage.value = fallbackMessage
}

async function fetchProperty() {
  loading.value = true
  errorMessage.value = ''

  try {
    property.value = await propertyApi.getProperty(propertyId.value)
    selectedAiTransaction.value = null
    valuation.value = null
    shapValues.value = []
  } catch {
    errorMessage.value = '부동산 상세 정보를 아직 불러오지 못했습니다. 백엔드 API 연결을 확인해 주세요.'
  } finally {
    loading.value = false
  }
}

async function requestAiExplanation(transaction?: PropertyTransaction) {
  if (transaction) {
    if (!isUsableAiTransaction(transaction)) {
      aiMessage.value = '추정가와 요인 분석은 매매 거래내역에서만 확인할 수 있습니다.'
      return
    }
    selectedAiTransaction.value = transaction
  }

  aiMessage.value = ''

  if (!authStore.isAuthenticated) {
    aiMessage.value = '로그인이 필요한 기능입니다.'
    return
  }

  if (!canRequestAi.value) {
    aiMessage.value = aiUnavailableMessage
    return
  }

  const currentProperty = property.value
  if (!currentProperty) {
    aiMessage.value = '부동산 상세 정보를 먼저 불러와야 합니다.'
    return
  }

  const payload = buildAiRequestPayload()
  if (!payload) {
    aiMessage.value = '추정가와 요인 분석에 사용할 매매 거래내역이 필요합니다.'
    return
  }

  aiLoading.value = true
  try {
    const messages: string[] = []
    let firstError: unknown = null
    let nextValuation: ValuationResponse | null = null
    let nextShapValues: ShapValue[] | null = null

    if (currentProperty.ai.valuationAvailable) {
      try {
        nextValuation = await propertyApi.requestValuation(propertyId.value, payload)
        messages.push(nextValuation.message)
      } catch (error) {
        firstError = error
      }
    }

    if (currentProperty.ai.shapAvailable) {
      try {
        const shap = await propertyApi.requestShap(propertyId.value, payload)
        nextShapValues = shap.values
        messages.push(shap.message)
      } catch (error) {
        firstError ??= error
      }
    }

    if (nextValuation) {
      valuation.value = nextValuation
    }
    if (nextShapValues) {
      shapValues.value = nextShapValues
    }

    if (valuation.value && shapValues.value.length) {
      chatContextStore.setPropertyAnalysisContext(currentProperty, valuation.value, shapValues.value)
    }

    if (messages.length) {
      aiMessage.value = messages.join(' ')
      return
    }

    aiMessage.value = aiRequestFailureMessage(firstError)
  } finally {
    aiLoading.value = false
  }
}

async function toggleApartmentFavorite() {
  favoriteMessage.value = ''
  favoriteErrorMessage.value = ''

  if (!property.value) {
    return
  }

  if (!authStore.isAuthenticated) {
    favoriteMessage.value = '로그인 후 관심 아파트를 저장할 수 있습니다.'
    return
  }

  if (property.value.propertyType !== 'APARTMENT') {
    favoriteMessage.value = '아파트 단지만 관심 아파트로 저장할 수 있습니다.'
    return
  }

  favoriteUpdating.value = true

  try {
    if (isApartmentFavorite.value) {
      await favoritesApi.removeApartment(property.value.propertyId)
      setApartmentFavorite(false)
      favoriteMessage.value = '관심 아파트에서 삭제했습니다.'
      return
    }

    await favoritesApi.addApartment(property.value.propertyId)
    setApartmentFavorite(true)
    favoriteMessage.value = '관심 아파트에 추가했습니다.'
  } catch (error) {
    setFavoriteFailure(error, '관심 아파트 상태를 변경하지 못했습니다. 잠시 후 다시 시도해 주세요.')
  } finally {
    favoriteUpdating.value = false
  }
}

async function askChatAboutAnalysis() {
  if (property.value && valuation.value) {
    chatContextStore.setPropertyAnalysisContext(property.value, valuation.value, shapValues.value)
  }
  await router.push({
    name: 'chat',
    query: {
      q: `${property.value?.name ?? '이 매물'}의 가격 예측이 왜 이렇게 나왔는지 설명해줘`
    }
  })
}

onMounted(fetchProperty)
</script>

<template>
  <section class="page-heading">
    <p class="eyebrow">부동산 상세</p>
    <h1>{{ property?.name ?? `부동산 #${propertyId}` }}</h1>
    <p v-if="property">
      {{ propertyTypeLabel(property.propertyType) }} · {{ property.address.sido }} {{ property.address.sigungu }}
      {{ property.address.legalDong }}
    </p>
  </section>

  <p v-if="loading" class="loading-text">상세 정보를 불러오고 있습니다.</p>
  <p v-if="errorMessage" class="inline-error">{{ errorMessage }}</p>

  <section v-if="property" class="detail-grid">
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
          <dd>{{ formatKrw(property.summary.latestDealAmount) }}</dd>
        </div>
        <div>
          <dt>최근 거래유형</dt>
          <dd>{{ latestTransaction ? transactionTypeLabel(latestTransaction.transactionType) : '정보 없음' }}</dd>
        </div>
        <div>
          <dt>최근 거래일</dt>
          <dd>{{ formatDate(property.summary.latestDealDate) }}</dd>
        </div>
        <div>
          <dt>거래 건수</dt>
          <dd>최근 거래 {{ recentTransactionCount.toLocaleString('ko-KR') }}건</dd>
        </div>
      </dl>
      <div class="favorite-actions">
        <button
          class="secondary-button"
          data-test="apartment-favorite-button"
          type="button"
          :disabled="favoriteUpdating"
          @click="toggleApartmentFavorite"
        >
          {{
            favoriteUpdating
              ? '처리 중입니다'
              : isApartmentFavorite
                ? '관심 아파트 삭제'
                : authStore.isAuthenticated
                  ? '관심 아파트 추가'
                  : '로그인 후 즐겨찾기'
          }}
        </button>
        <p v-if="favoriteMessage" class="helper-text">{{ favoriteMessage }}</p>
        <p v-if="favoriteErrorMessage" class="inline-error">{{ favoriteErrorMessage }}</p>
      </div>
    </article>

    <article class="info-panel">
      <h2>AI 분석</h2>
      <p v-if="!authStore.isAuthenticated" class="muted">추정가와 SHAP 요인은 로그인 후 확인할 수 있습니다.</p>
      <p v-else-if="!canRequestAi" class="muted">{{ aiUnavailableMessage }}</p>
      <p v-else class="muted">
        매매 실거래 데이터를 바탕으로 계산한 추정과 주요 요인 설명을 요청합니다.
        <span v-if="aiInputSummary">{{ aiInputSummary }}</span>
        <span v-else>매매 거래내역을 선택해 주세요.</span>
      </p>
      <p v-if="aiLoading" class="helper-text" data-test="ai-loading-message">
        가격 예측을 계산하는 중입니다. 보통 몇 초 안에 표시됩니다.
      </p>
      <p v-if="aiMessage" class="helper-text">{{ aiMessage }}</p>
      <p v-if="valuation?.estimatedPrice" class="estimate-text">
        추정가 {{ formatKrw(valuation.estimatedPrice) }}
      </p>
      <button
        v-if="canAskChatAboutAnalysis"
        class="secondary-button"
        type="button"
        @click="askChatAboutAnalysis"
      >
        챗봇으로 요인 설명 보기
      </button>
    </article>
  </section>

  <section v-if="property" class="chart-grid">
    <article class="info-panel transaction-history-panel">
      <h2>거래 내역</h2>
      <div class="transaction-toolbar">
        <fieldset class="transaction-type-filter">
          <legend>거래 유형</legend>
          <label v-for="type in transactionTypeOptions" :key="type" class="check-row">
            <input
              v-model="selectedTransactionTypes"
              :data-test="`transaction-type-filter-${type}`"
              :value="type"
              type="checkbox"
            />
            <span>{{ transactionTypeLabel(type) }}</span>
          </label>
        </fieldset>
        <p class="muted">
          {{ transactionVisibleStart.toLocaleString('ko-KR') }}-{{ transactionVisibleEnd.toLocaleString('ko-KR') }}건 표시 / 필터 결과
          {{ sortedTransactions.length.toLocaleString('ko-KR') }}건 / 전체
          {{ property.transactions.length.toLocaleString('ko-KR') }}건
        </p>
      </div>
      <div v-if="sortedTransactions.length" class="transaction-table-wrap">
        <table class="transaction-table">
          <thead>
            <tr>
              <th :aria-sort="transactionSortAria('dealDate')" scope="col">
                <button
                  data-test="transaction-sort-dealDate"
                  type="button"
                  @click="setTransactionSort('dealDate')"
                >
                  거래일 <span aria-hidden="true">{{ transactionSortIndicator('dealDate') }}</span>
                </button>
              </th>
              <th :aria-sort="transactionSortAria('transactionType')" scope="col">
                <button
                  data-test="transaction-sort-transactionType"
                  type="button"
                  @click="setTransactionSort('transactionType')"
                >
                  거래유형 <span aria-hidden="true">{{ transactionSortIndicator('transactionType') }}</span>
                </button>
              </th>
              <th :aria-sort="transactionSortAria('dealAmount')" scope="col">
                <button
                  data-test="transaction-sort-dealAmount"
                  type="button"
                  @click="setTransactionSort('dealAmount')"
                >
                  거래금액 <span aria-hidden="true">{{ transactionSortIndicator('dealAmount') }}</span>
                </button>
              </th>
              <th :aria-sort="transactionSortAria('exclusiveAreaM2')" scope="col">
                <button
                  data-test="transaction-sort-exclusiveAreaM2"
                  type="button"
                  @click="setTransactionSort('exclusiveAreaM2')"
                >
                  전용면적 <span aria-hidden="true">{{ transactionSortIndicator('exclusiveAreaM2') }}</span>
                </button>
              </th>
              <th :aria-sort="transactionSortAria('floor')" scope="col">
                <button
                  data-test="transaction-sort-floor"
                  type="button"
                  @click="setTransactionSort('floor')"
                >
                  층수 <span aria-hidden="true">{{ transactionSortIndicator('floor') }}</span>
                </button>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="transaction in pagedTransactions"
              :key="transactionKey(transaction)"
              :class="{
                'is-ai-selectable': isUsableAiTransaction(transaction),
                'is-ai-selected': isSelectedAiTransaction(transaction)
              }"
              :tabindex="isUsableAiTransaction(transaction) ? 0 : undefined"
              :title="isUsableAiTransaction(transaction) ? '이 매매 거래 기준으로 추정가와 요인 분석 보기' : undefined"
              data-test="transaction-row"
              @click="requestAiExplanation(transaction)"
              @keydown.enter.prevent="requestAiExplanation(transaction)"
              @keydown.space.prevent="requestAiExplanation(transaction)"
            >
              <td>{{ formatDate(transaction.dealDate) }}</td>
              <td>
                <span :class="['transaction-chip', `transaction-chip--${transaction.transactionType.toLowerCase()}`]">
                  {{ transactionTypeLabel(transaction.transactionType) }}
                </span>
              </td>
              <td><strong class="transaction-amount">{{ formatTransactionAmount(transaction) }}</strong></td>
              <td>{{ formatArea(transaction.exclusiveAreaM2) }}</td>
              <td>{{ formatFloor(transaction.floor) }}</td>
            </tr>
          </tbody>
        </table>
        <nav
          v-if="transactionTotalPages > 1"
          class="transaction-pagination"
          aria-label="거래내역 페이지"
        >
          <button
            class="transaction-page-btn"
            data-test="transaction-prev-page"
            type="button"
            :disabled="transactionPage === 1"
            @click="goToTransactionPage(transactionPage - 1)"
          >
            이전
          </button>
          <span class="transaction-page-state">
            {{ transactionPage.toLocaleString('ko-KR') }} / {{ transactionTotalPages.toLocaleString('ko-KR') }}
          </span>
          <button
            class="transaction-page-btn"
            data-test="transaction-next-page"
            type="button"
            :disabled="transactionPage === transactionTotalPages"
            @click="goToTransactionPage(transactionPage + 1)"
          >
            다음
          </button>
        </nav>
      </div>
      <EmptyState
        v-else
        title="표시할 거래 내역이 없습니다."
        description="거래 유형 선택을 바꾸거나 실거래 데이터가 연결된 뒤 다시 확인해 주세요."
      />
    </article>
    <article class="info-panel">
      <h2>SHAP 요인 차트</h2>
      <ShapChart :values="shapValues" />
    </article>
  </section>

  <EmptyState
    v-if="!loading && !property && !errorMessage"
    title="상세 정보를 기다리고 있습니다."
    description="부동산 상세 API가 연결되면 기본 정보와 거래 내역을 표시합니다."
  />
</template>
