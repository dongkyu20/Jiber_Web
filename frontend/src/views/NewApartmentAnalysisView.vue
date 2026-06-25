<script setup lang="ts">
import { computed, ref } from 'vue'

import { getApiError } from '@/api/client'
import { propertyApi } from '@/api/property'
import type { NewApartmentAnalysisResponse, ShapValue } from '@/api/types'
import ShapChart from '@/charts/ShapChart.vue'
import { formatKrw } from '@/utils/format'

const today = new Date().toISOString().slice(0, 10)

const form = ref({
  propertyName: '',
  sido: '서울특별시',
  sigungu: '',
  legalDong: '',
  latitude: null as number | null,
  longitude: null as number | null,
  householdCount: null as number | null,
  exclusiveAreaM2: 84.95,
  floor: 10,
  builtYear: 2010,
  asOfDate: today,
  distanceToStationM: 420 as number | null
})

const loading = ref(false)
const errorMessage = ref('')
const result = ref<NewApartmentAnalysisResponse | null>(null)

const canSubmit = computed(() =>
  form.value.propertyName.trim().length > 0 &&
  form.value.sido.trim().length > 0 &&
  form.value.sigungu.trim().length > 0 &&
  form.value.legalDong.trim().length > 0 &&
  form.value.exclusiveAreaM2 > 0 &&
  form.value.floor !== null &&
  form.value.builtYear >= 1900 &&
  form.value.asOfDate.length > 0 &&
  !loading.value
)

const shapValues = computed<ShapValue[]>(() => result.value?.shap.values ?? [])

function optionalNumber(value: number | null) {
  return value === null || Number.isNaN(value) ? undefined : value
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
  if (!canSubmit.value) return

  loading.value = true
  errorMessage.value = ''
  result.value = null
  try {
    result.value = await propertyApi.analyzeNewApartment({
      propertyName: form.value.propertyName.trim(),
      sido: form.value.sido.trim(),
      sigungu: form.value.sigungu.trim(),
      legalDong: form.value.legalDong.trim(),
      latitude: optionalNumber(form.value.latitude),
      longitude: optionalNumber(form.value.longitude),
      householdCount: optionalNumber(form.value.householdCount),
      exclusiveAreaM2: form.value.exclusiveAreaM2,
      floor: form.value.floor,
      builtYear: form.value.builtYear,
      asOfDate: form.value.asOfDate,
      distanceToStationM: optionalNumber(form.value.distanceToStationM)
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

        <label>
          <span>시도</span>
          <input v-model="form.sido" type="text" placeholder="서울특별시" />
        </label>
        <label>
          <span>시군구</span>
          <input v-model="form.sigungu" type="text" placeholder="강남구" />
        </label>
        <label>
          <span>법정동</span>
          <input v-model="form.legalDong" type="text" placeholder="삼성동" />
        </label>
        <label>
          <span>전용면적(m²)</span>
          <input v-model.number="form.exclusiveAreaM2" min="0.01" step="0.01" type="number" />
        </label>
        <label>
          <span>층수</span>
          <input v-model.number="form.floor" step="1" type="number" />
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
        <label>
          <span>지하철 거리(m)</span>
          <input v-model.number="form.distanceToStationM" min="0" step="10" type="number" placeholder="선택" />
        </label>
        <label>
          <span>위도</span>
          <input v-model.number="form.latitude" step="0.0000001" type="number" placeholder="선택" />
        </label>
        <label>
          <span>경도</span>
          <input v-model.number="form.longitude" step="0.0000001" type="number" placeholder="선택" />
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
        <div class="estimate-card">
          <span>추정 적정가</span>
          <strong>{{ formatKrw(result.valuation.estimatedPrice) }}</strong>
          <small v-if="result.valuation.predictionInterval">
            예측구간 {{ formatKrw(result.valuation.predictionInterval.lower) }} -
            {{ formatKrw(result.valuation.predictionInterval.upper) }}
          </small>
        </div>
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
    <article class="info-panel">
      <h2>SHAP 요인 차트</h2>
      <ShapChart :values="shapValues" />
    </article>

    <article class="info-panel">
      <h2>주요 요인</h2>
      <div v-if="shapValues.length" class="factor-list">
        <div v-for="factor in shapValues" :key="factor.feature" class="factor-row">
          <div>
            <strong>{{ factor.labelKo }}</strong>
            <span>{{ factor.value }}</span>
          </div>
          <b :class="['factor-delta', factor.direction.toLowerCase()]">{{ formatKrw(factor.shapValue) }}</b>
        </div>
      </div>
      <p v-else class="muted">분석 후 가격에 영향을 준 요인이 표시됩니다.</p>
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

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.form-grid label {
  display: grid;
  gap: 7px;
}

.form-grid label span {
  color: var(--cream-muted);
  font-size: 0.82rem;
  font-weight: 700;
}

.form-grid .wide {
  grid-column: 1 / -1;
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

.estimate-card span,
.estimate-card small {
  color: var(--cream-muted);
  font-size: 0.82rem;
}

.estimate-card strong {
  color: var(--gold-light);
  font-size: 1.55rem;
}

.summary-list.compact {
  gap: 8px;
}

.factor-list {
  display: grid;
  gap: 10px;
}

.factor-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border);
}

.factor-row:last-child {
  border-bottom: 0;
}

.factor-row div {
  display: grid;
  gap: 4px;
}

.factor-row strong {
  color: var(--cream);
  font-size: 0.92rem;
}

.factor-row span {
  color: var(--cream-muted);
  font-size: 0.8rem;
}

.factor-delta {
  color: var(--cream-muted);
  white-space: nowrap;
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
}
</style>
