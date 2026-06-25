<script setup lang="ts">
import { computed, ref } from 'vue'

import { getApiError } from '@/api/client'
import { propertyApi } from '@/api/property'
import { openPostcodeSearch, PostcodeError, type PostcodeSelection } from '@/address/postcode'
import type { NewApartmentAddressCandidate, NewApartmentAnalysisResponse, ShapValue } from '@/api/types'
import ShapChart from '@/charts/ShapChart.vue'
import { formatKrw } from '@/utils/format'

const today = new Date().toISOString().slice(0, 10)

type SelectedAnalysisAddress = PostcodeSelection & {
  latitude?: number | null
  longitude?: number | null
}

const form = ref({
  propertyName: '',
  address: '',
  householdCount: null as number | null,
  exclusiveAreaM2: 84.95,
  floor: 10,
  topFloor: 30,
  builtYear: 2010,
  asOfDate: today,
})

const loading = ref(false)
const errorMessage = ref('')
const result = ref<NewApartmentAnalysisResponse | null>(null)
const addressLoading = ref(false)
const addressError = ref('')
const addressMessage = ref('')
const selectedAddress = ref<SelectedAnalysisAddress | null>(null)

const hasAnalysisFields = computed(() =>
  form.value.propertyName.trim().length > 0 &&
  form.value.exclusiveAreaM2 > 0 &&
  form.value.floor !== null &&
  form.value.topFloor !== null &&
  form.value.topFloor >= Math.max(form.value.floor, 1) &&
  form.value.builtYear >= 1900 &&
  form.value.asOfDate.length > 0
)

const canSubmit = computed(() => hasAnalysisFields.value && selectedAddress.value !== null && !loading.value)
const canSearchAddress = computed(() => !addressLoading.value)
const shapValues = computed<ShapValue[]>(() => result.value?.shap.values ?? [])

function optionalNumber(value: number | null) {
  return value === null || Number.isNaN(value) ? undefined : value
}

function selectedAddressDescription(address: SelectedAnalysisAddress) {
  const parts = []
  if (address.zonecode) {
    parts.push(`우편번호 ${address.zonecode}`)
  }
  if (address.jibunAddress && address.jibunAddress !== address.fullAddress) {
    parts.push(`지번 ${address.jibunAddress}`)
  }
  return parts.length ? parts.join(' · ') : `${address.sido} ${address.sigungu} ${address.legalDong}`
}

function isSameAddress(candidate: NewApartmentAddressCandidate, address: PostcodeSelection) {
  const selectedValues = [address.fullAddress, address.roadAddress, address.jibunAddress]
    .filter((value): value is string => Boolean(value))
    .map((value) => value.trim())
  const candidateValues = [candidate.fullAddress, candidate.roadAddress, candidate.jibunAddress]
    .filter((value): value is string => Boolean(value))
    .map((value) => value.trim())

  return selectedValues.some((selectedValue) => candidateValues.includes(selectedValue))
}

function toAnalysisAddress(postcodeAddress: PostcodeSelection, candidate?: NewApartmentAddressCandidate) {
  return {
    ...postcodeAddress,
    sido: candidate?.sido ?? postcodeAddress.sido,
    sigungu: candidate?.sigungu ?? postcodeAddress.sigungu,
    legalDong: candidate?.legalDong ?? postcodeAddress.legalDong,
    latitude: candidate?.latitude ?? null,
    longitude: candidate?.longitude ?? null
  }
}

async function resolveAddressCoordinates(postcodeAddress: PostcodeSelection) {
  const candidates = await propertyApi.searchNewApartmentAddresses(postcodeAddress.fullAddress)
  return candidates.find((candidate) => isSameAddress(candidate, postcodeAddress)) ?? candidates[0]
}

function postcodeErrorMessage(error: unknown) {
  if (error instanceof PostcodeError && error.code === 'CLOSED') {
    return '주소 선택이 취소되었습니다.'
  }
  if (error instanceof PostcodeError) {
    return '카카오 주소 검색 창을 열지 못했습니다. 잠시 후 다시 시도해 주세요.'
  }
  return getApiError(error)?.message ?? '선택한 주소의 좌표를 확인하지 못했습니다.'
}

async function searchAddressCandidates() {
  selectedAddress.value = null
  addressError.value = ''
  addressMessage.value = ''
  errorMessage.value = ''

  addressLoading.value = true
  try {
    const postcodeAddress = await openPostcodeSearch()
    form.value.address = postcodeAddress.fullAddress
    addressMessage.value = '주소를 선택했습니다. 좌표를 확인하고 있습니다.'
    const candidate = await resolveAddressCoordinates(postcodeAddress)
    selectedAddress.value = toAnalysisAddress(postcodeAddress, candidate)
    addressMessage.value = candidate
      ? '선택한 주소를 분석에 사용합니다.'
      : '주소를 선택했습니다. 좌표는 분석 과정에서 가능한 값으로 보완합니다.'
  } catch (error) {
    if (error instanceof PostcodeError && error.code === 'CLOSED') {
      addressMessage.value = postcodeErrorMessage(error)
    } else {
      addressError.value = postcodeErrorMessage(error)
    }
  } finally {
    addressLoading.value = false
  }
}

function analysisFailureMessage(error: unknown) {
  const apiError = getApiError(error)

  if (apiError?.code === 'AUTH_REQUIRED') {
    return '로그인 후 신규매물 분석을 사용할 수 있습니다.'
  }
  if (apiError?.code === 'VALUATION_INSUFFICIENT_DATA') {
    const missing = apiError.details?.map((detail) => detail.field).filter(Boolean).join(', ')
    return missing ? `모델 분석에 필요한 값이 부족합니다: ${missing}` : '모델 분석에 필요한 값이 부족합니다.'
  }
  if (apiError?.code === 'MODEL_SERVER_UNAVAILABLE') {
    return '가격예측 모델 서버와 연결할 수 없습니다. model-server 상태를 확인해주세요.'
  }

  return apiError?.message ?? '신규매물 분석을 완료하지 못했습니다.'
}

async function submitAnalysis() {
  if (!selectedAddress.value) {
    errorMessage.value = '카카오 주소 검색에서 정확한 주소를 선택해 주세요.'
    return
  }
  if (!hasAnalysisFields.value || loading.value) return

  loading.value = true
  errorMessage.value = ''
  result.value = null
  const address = selectedAddress.value

  try {
    result.value = await propertyApi.analyzeNewApartment({
      propertyName: form.value.propertyName.trim(),
      sido: address.sido,
      sigungu: address.sigungu,
      legalDong: address.legalDong,
      latitude: address.latitude ?? undefined,
      longitude: address.longitude ?? undefined,
      householdCount: optionalNumber(form.value.householdCount),
      exclusiveAreaM2: form.value.exclusiveAreaM2,
      floor: form.value.floor,
      topFloor: form.value.topFloor,
      builtYear: form.value.builtYear,
      asOfDate: form.value.asOfDate
    })
  } catch (error) {
    errorMessage.value = analysisFailureMessage(error)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="page-heading">
    <p class="eyebrow">AI ANALYSIS</p>
    <h1>신규매물 가격예측</h1>
    <p>아파트 조건을 입력하면 가격예측 모델이 적정가 범위와 SHAP 기반 영향 요인을 계산합니다.</p>
  </section>

  <section class="analysis-layout">
    <form class="analysis-form info-panel" @submit.prevent="submitAnalysis">
      <h2>매물 조건</h2>

      <div class="form-grid">
        <label class="wide">
          <span>단지명</span>
          <input v-model="form.propertyName" type="text" placeholder="예: 래미안 삼성" />
        </label>

        <div class="field-card wide address-field">
          <label for="new-analysis-address">
            <span>주소 검색</span>
          </label>
          <div class="address-search-row">
            <input
              id="new-analysis-address"
              v-model="form.address"
              data-test="new-analysis-address"
              type="text"
              placeholder="주소 검색 버튼으로 정확한 주소를 선택해 주세요."
              readonly
            />
            <button
              class="secondary-button"
              data-test="new-analysis-address-search"
              type="button"
              :disabled="!canSearchAddress"
              @click="searchAddressCandidates"
            >
              {{ addressLoading ? '검색 중' : '주소 검색' }}
            </button>
          </div>
          <small>카카오 우편번호 서비스에서 도로명 또는 지번 주소를 선택해 주세요.</small>
          <p v-if="addressError" class="inline-error">{{ addressError }}</p>
          <p v-else-if="addressMessage" class="muted address-message">{{ addressMessage }}</p>
          <div v-if="selectedAddress" class="selected-address">
            <span>선택된 주소</span>
            <strong>{{ selectedAddress.fullAddress }}</strong>
            <small>{{ selectedAddressDescription(selectedAddress) }}</small>
          </div>
        </div>
        <label>
          <span>전용면적(m²)</span>
          <input v-model.number="form.exclusiveAreaM2" min="0.01" step="0.01" type="number" />
        </label>
        <label>
          <span>층수</span>
          <input v-model.number="form.floor" min="1" step="1" type="number" />
        </label>
        <label>
          <span>최고 층수</span>
          <input
            v-model.number="form.topFloor"
            data-test="new-analysis-top-floor"
            min="1"
            step="1"
            type="number"
          />
        </label>
        <label>
          <span>준공연도</span>
          <input v-model.number="form.builtYear" min="1900" max="2100" step="1" type="number" />
        </label>
        <label>
          <span>분석 기준일</span>
          <input v-model="form.asOfDate" type="date" />
        </label>
        <label>
          <span>세대수</span>
          <input v-model.number="form.householdCount" min="0" step="1" type="number" placeholder="선택" />
        </label>
      </div>

      <p v-if="errorMessage" class="inline-error">{{ errorMessage }}</p>

      <div class="button-row">
        <button class="primary-button" type="submit" :disabled="!canSubmit">
          {{ loading ? '분석 중' : '적정가·요인 분석' }}
        </button>
      </div>
    </form>

    <aside class="analysis-summary info-panel">
      <h2>분석 결과</h2>
      <template v-if="result">
        <p class="muted">{{ result.message }}</p>
        <dl class="summary-list compact">
          <div>
            <dt>기준일</dt>
            <dd>{{ result.valuation.baselineDate ?? '정보 없음' }}</dd>
          </div>
        </dl>
      </template>
      <p v-else class="muted">매물 조건을 입력하고 분석을 실행하면 추정가와 요인 분석이 여기에 표시됩니다.</p>
    </aside>
  </section>

  <section class="chart-grid">
    <article class="info-panel analysis-insight-card analysis-chart-card" data-test="new-analysis-chart-card">
      <h2>SHAP 요인 차트</h2>
      <div v-if="result" class="estimate-card estimate-card-inline" data-test="new-analysis-estimate-card">
        <span>추정 적정가</span>
        <strong>{{ formatKrw(result.valuation.estimatedPrice) }}</strong>
        <small v-if="result.valuation.predictionInterval">
          예측구간 {{ formatKrw(result.valuation.predictionInterval.lower) }} -
          {{ formatKrw(result.valuation.predictionInterval.upper) }}
        </small>
      </div>
      <ShapChart :values="shapValues" />
    </article>

    <article
      class="info-panel analysis-insight-card analysis-factor-card is-stretched"
      data-test="new-analysis-factor-card"
    >
      <h2>주요 요인</h2>
      <div
        v-if="shapValues.length"
        class="factor-list is-compressed fills-card-space"
        data-test="new-analysis-factor-list"
      >
        <div v-for="factor in shapValues" :key="factor.feature" class="factor-row">
          <div>
            <strong>{{ factor.labelKo }}</strong>
            <span>{{ factor.value }}</span>
          </div>
          <b :class="['factor-delta', factor.direction.toLowerCase()]">{{ formatKrw(factor.shapValue) }}</b>
        </div>
      </div>
      <p v-else class="muted factor-empty">분석 후 가격에 영향을 준 요인이 표시됩니다.</p>
    </article>
  </section>
</template>

<style scoped>
.analysis-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(280px, 0.55fr);
  gap: 20px;
  align-items: start;
  margin-bottom: 20px;
}

.analysis-form,
.analysis-summary {
  padding: 24px;
}

.analysis-form {
  display: grid;
  gap: 18px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.form-grid > label,
.form-grid .field-card {
  display: grid;
  min-width: 0;
  gap: 9px;
  padding: 14px;
  border: 1px solid color-mix(in srgb, var(--border) 86%, var(--cream) 14%);
  border-radius: 12px;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--bg-card) 94%, var(--cream) 6%), var(--bg-card));
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.form-grid > label:focus-within,
.form-grid .field-card:focus-within {
  border-color: var(--border-strong);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--bg-card-hover) 90%, var(--cream) 10%), var(--bg-card-hover));
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.16);
  transform: translateY(-1px);
}

.form-grid > label span,
.form-grid .field-card label span {
  color: var(--cream-muted);
  font-size: 0.82rem;
  font-weight: 700;
}

.form-grid > label small,
.form-grid .field-card small {
  color: var(--cream-muted);
  font-size: 0.78rem;
  line-height: 1.55;
}

.form-grid input {
  width: 100%;
  min-width: 0;
  min-height: 42px;
  border: 1px solid transparent;
  border-radius: 9px;
  background: color-mix(in srgb, var(--bg) 82%, var(--cream) 18%);
  color: var(--cream);
  padding: 10px 12px;
  font: inherit;
  font-size: 0.94rem;
  line-height: 1.4;
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--border) 78%, transparent);
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    box-shadow 0.18s ease;
}

.form-grid input:hover {
  background: color-mix(in srgb, var(--bg) 74%, var(--cream) 26%);
}

.form-grid input:focus {
  border-color: var(--gold);
  outline: none;
  background: color-mix(in srgb, var(--bg) 70%, var(--cream) 30%);
  box-shadow:
    inset 0 0 0 1px color-mix(in srgb, var(--gold) 42%, transparent),
    0 0 0 3px color-mix(in srgb, var(--gold) 18%, transparent);
}

.form-grid input::placeholder {
  color: color-mix(in srgb, var(--cream-muted) 70%, transparent);
}

.form-grid input[type="date"] {
  color-scheme: dark;
}

:global(:root[data-theme="light"]) .form-grid input[type="date"] {
  color-scheme: light;
}

.form-grid .wide {
  grid-column: 1 / -1;
}

.address-search-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: stretch;
}

.address-search-row .secondary-button {
  min-height: 42px;
  padding-inline: 16px;
}

.address-message {
  margin: 0;
  font-size: 0.82rem;
}

.selected-address strong {
  font-size: 0.9rem;
  line-height: 1.45;
}

.selected-address span {
  color: var(--cream-muted);
  font-size: 0.78rem;
  line-height: 1.45;
}

.selected-address {
  display: grid;
  gap: 5px;
  padding: 12px;
  border: 1px solid color-mix(in srgb, var(--gold) 40%, var(--border));
  border-radius: 9px;
  background: color-mix(in srgb, var(--gold) 12%, transparent);
}

.estimate-card {
  display: grid;
  gap: 8px;
  margin: 18px 0;
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: rgba(200, 160, 100, 0.08);
}

.estimate-card-inline {
  grid-template-columns: minmax(86px, auto) minmax(0, 1fr);
  align-items: center;
  column-gap: 14px;
  row-gap: 4px;
  margin: 0;
  padding: 14px 16px;
}

.estimate-card span,
.estimate-card small {
  color: var(--cream-muted);
  font-size: 0.82rem;
}

.estimate-card strong {
  color: var(--gold-light);
  font-size: 1.55rem;
}

.estimate-card-inline strong {
  font-size: 1.28rem;
  line-height: 1.25;
}

.estimate-card-inline small {
  grid-column: 2;
  line-height: 1.45;
}

.summary-list.compact {
  gap: 8px;
}

.analysis-insight-card {
  display: grid;
  align-content: start;
  gap: 16px;
  min-width: 0;
  padding: 22px;
}

.analysis-insight-card h2 {
  margin: 0;
}

.analysis-factor-card.is-stretched {
  align-content: stretch;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 100%;
}

.analysis-chart-card :deep(.chart-box) {
  min-height: 300px;
}

.analysis-chart-card :deep(.empty-state) {
  min-height: 240px;
  align-content: center;
  gap: 8px;
  padding: 28px 24px;
}

.analysis-chart-card :deep(.empty-state strong) {
  line-height: 1.5;
}

.analysis-chart-card :deep(.empty-state p) {
  margin-top: 4px;
  line-height: 1.7;
}

.factor-list {
  display: grid;
  gap: 12px;
}

.factor-list.is-compressed {
  align-content: start;
  gap: 7px;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
  scrollbar-width: thin;
  scrollbar-color: color-mix(in srgb, var(--gold) 42%, transparent) transparent;
}

.factor-list.fills-card-space {
  align-self: stretch;
  max-height: none;
}

.factor-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px;
  border: 1px solid color-mix(in srgb, var(--border) 84%, transparent);
  border-radius: 10px;
  background: color-mix(in srgb, var(--bg-card-hover) 64%, transparent);
}

.factor-list.is-compressed .factor-row {
  align-items: center;
  gap: 10px;
  min-height: 48px;
  padding: 8px 10px;
  border-radius: 8px;
}

.factor-row div {
  display: grid;
  min-width: 0;
  gap: 7px;
}

.factor-row strong {
  color: var(--cream);
  font-size: 0.94rem;
  line-height: 1.45;
}

.factor-list.is-compressed .factor-row div {
  gap: 2px;
}

.factor-list.is-compressed .factor-row strong {
  font-size: 0.86rem;
  line-height: 1.25;
}

.factor-row span {
  color: var(--cream-muted);
  font-size: 0.82rem;
  line-height: 1.55;
}

.factor-list.is-compressed .factor-row span {
  font-size: 0.76rem;
  line-height: 1.25;
}

.factor-delta {
  color: var(--cream-muted);
  white-space: nowrap;
  font-size: 0.9rem;
  line-height: 1.45;
  padding-top: 2px;
}

.factor-list.is-compressed .factor-delta {
  font-size: 0.82rem;
  line-height: 1.25;
  padding-top: 0;
}

.factor-empty {
  margin: 0;
  line-height: 1.75;
}

.factor-delta.up { color: #e6a832; }
.factor-delta.down { color: #7ab0e0; }

@media (max-width: 900px) {
  .analysis-layout {
    grid-template-columns: 1fr;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .analysis-insight-card {
    padding: 20px;
  }

  .analysis-factor-card.is-stretched {
    min-height: auto;
  }

  .factor-row {
    flex-direction: column;
    gap: 10px;
  }

  .factor-list.is-compressed .factor-row {
    flex-direction: row;
    align-items: center;
  }

  .factor-delta {
    padding-top: 0;
  }
}

@media (max-width: 560px) {
  .analysis-form,
  .analysis-summary {
    padding: 18px;
  }

  .form-grid {
    gap: 12px;
  }

  .form-grid > label {
    padding: 12px;
  }

  .form-grid .field-card {
    padding: 12px;
  }

  .address-search-row {
    grid-template-columns: 1fr;
  }

  .analysis-insight-card {
    padding: 18px;
  }

  .analysis-chart-card :deep(.empty-state) {
    min-height: 200px;
    padding: 24px 18px;
  }

  .estimate-card-inline {
    grid-template-columns: 1fr;
  }

  .estimate-card-inline small {
    grid-column: auto;
  }
}
</style>
