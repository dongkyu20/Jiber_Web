<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { favoritesApi } from '@/api/favorites'
import { getApiError } from '@/api/client'
import { propertyApi } from '@/api/property'
import type {
  AdministrativeCluster,
  FavoriteAreaCreateRequest,
  PropertyMapItem,
  PropertySearchItem,
  PropertyType
} from '@/api/types'
import KakaoMapPanel from '@/components/KakaoMapPanel.vue'
import EmptyState from '@/components/EmptyState.vue'
import FloatingChat from '@/components/FloatingChat.vue'
import { SEOUL_SEED_VIEWPORT, normalizePropertyMapItems, type LatLngPoint, type MapViewport } from '@/map/kakaoMap'
import { hasKakaoMapKey } from '@/map/kakaoLoader'
import { useAuthStore } from '@/stores/auth'
import { useMapSearchStore, type MapSearchSnapshot } from '@/stores/mapSearch'
import { formatLatestTransactionAmount, propertyTypeLabel, transactionTypeLabel } from '@/utils/format'
import { SearchTrie } from '@/utils/searchTrie'

const propertyTypeOptions: PropertyType[] = ['APARTMENT', 'OFFICETEL', 'VILLA']
const keywordSearchPageSize = 100
const autocompleteFetchSize = 20

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const mapSearchStore = useMapSearchStore()
const selectedPropertyTypes = ref<PropertyType[]>([...propertyTypeOptions])
const zoomLevel = ref(SEOUL_SEED_VIEWPORT.zoomLevel)
const loading = ref(false)
const errorMessage = ref('')
const items = ref<PropertyMapItem[]>([])
const administrativeClusters = ref<AdministrativeCluster[]>([])
const showAdministrativePriceLayer = ref(false)
const currentViewport = ref<MapViewport>({ ...SEOUL_SEED_VIEWPORT })
const selectedPropertyId = ref<number | null>(null)
const searchKeyword = ref('')
const activeSearchKeyword = ref('')
const keywordSearchPage = ref(0)
const keywordSearchTotal = ref<number | null>(null)
const keywordLoadingMore = ref(false)
const autocompleteSuggestions = ref<string[]>([])
const autocompleteOpen = ref(false)
const highlightedSuggestionIndex = ref(-1)
const mapFocusTarget = ref<LatLngPoint | null>(null)
const hasMapKey = hasKakaoMapKey()
const areaFavoriteLoading = ref(false)
const areaFavoriteMessage = ref('')
const areaFavoriteErrorMessage = ref('')
const autocompleteTrie = new SearchTrie()
let searchTimer: number | null = null
let autocompleteTimer: number | null = null
let pendingInitialViewport: MapViewport | null = null
let restoredFromSnapshot = false
let mounted = false

const isKeywordSearch = computed(() => activeSearchKeyword.value.length > 0)
const hasMoreKeywordResults = computed(
  () => isKeywordSearch.value && keywordSearchTotal.value !== null && items.value.length < keywordSearchTotal.value
)
const resultCountText = computed(() => {
  if (isKeywordSearch.value && keywordSearchTotal.value !== null) {
    return `${keywordSearchTotal.value.toLocaleString('ko-KR')}건`
  }

  return `${items.value.length.toLocaleString('ko-KR')}건`
})
const mapRuntimeMessage = computed(() =>
  hasMapKey
    ? '지도 SDK가 준비되면 현재 화면 범위로 다시 검색합니다.'
    : '지도 SDK 키가 없어도 목록 검색은 계속 사용할 수 있습니다. VITE_KAKAO_MAP_APP_KEY를 설정하면 실제 지도를 불러올 수 있습니다.'
)
const currentAreaLabel = computed(() => {
  const keyword = activeSearchKeyword.value.trim()
  return keyword ? `검색: ${keyword}` : '현재 지도 영역'
})

const resultDescription = computed(() => {
  if (loading.value) {
    return isKeywordSearch.value
      ? '검색어와 거래 조건에 맞는 실거래 정보를 불러오고 있습니다.'
      : '현재 지도 범위의 실거래 정보를 불러오고 있습니다.'
  }

  if (items.value.length > 0) {
    const count = items.value.length.toLocaleString('ko-KR')
    return isKeywordSearch.value
      ? `"${activeSearchKeyword.value}" 검색 결과 ${count}건을 찾았습니다.`
      : `${count}건의 실거래 위치를 찾았습니다.`
  }

  return isKeywordSearch.value
    ? `"${activeSearchKeyword.value}"에 맞는 검색 결과가 없습니다. 검색어 또는 거래 조건을 조정해 보세요.`
    : '현재 조건에 맞는 검색 결과가 없습니다.'
})

function roundCoordinate(value: number) {
  return Number(value.toFixed(7))
}

function viewportCenter(viewport: MapViewport): LatLngPoint {
  return {
    lat: roundCoordinate((viewport.swLat + viewport.neLat) / 2),
    lng: roundCoordinate((viewport.swLng + viewport.neLng) / 2)
  }
}

function readQueryString(value: unknown): string | null {
  const rawValue = Array.isArray(value) ? value[0] : value
  if (typeof rawValue !== 'string') {
    return null
  }

  const trimmed = rawValue.trim()
  return trimmed || null
}

function readQueryNumber(value: unknown): number | null {
  const rawValue = readQueryString(value)
  if (!rawValue) {
    return null
  }

  const parsed = Number(rawValue)
  return Number.isFinite(parsed) ? parsed : null
}

function readQueryBoolean(value: unknown): boolean | null {
  const rawValue = readQueryString(value)
  if (rawValue === null) {
    return null
  }

  return rawValue === '1' || rawValue === 'true'
}

function parsePropertyTypes(value: unknown): PropertyType[] | null {
  const rawValue = readQueryString(value)
  if (rawValue === null) {
    return null
  }
  if (rawValue === 'none') {
    return []
  }

  const allowed = new Set<PropertyType>(propertyTypeOptions)
  return rawValue
    .split(',')
    .map((type) => type.trim())
    .filter((type): type is PropertyType => allowed.has(type as PropertyType))
}

function clampZoomLevel(value: number | null): number {
  if (value === null) {
    return SEOUL_SEED_VIEWPORT.zoomLevel
  }

  return Math.min(14, Math.max(1, Math.round(value)))
}

function viewportAroundCenter(center: LatLngPoint, nextZoomLevel: number): MapViewport {
  const latSpan = SEOUL_SEED_VIEWPORT.neLat - SEOUL_SEED_VIEWPORT.swLat
  const lngSpan = SEOUL_SEED_VIEWPORT.neLng - SEOUL_SEED_VIEWPORT.swLng

  return {
    swLat: roundCoordinate(center.lat - latSpan / 2),
    swLng: roundCoordinate(center.lng - lngSpan / 2),
    neLat: roundCoordinate(center.lat + latSpan / 2),
    neLng: roundCoordinate(center.lng + lngSpan / 2),
    zoomLevel: nextZoomLevel
  }
}

function queryViewport(): MapViewport | null {
  const swLat = readQueryNumber(route.query.swLat)
  const swLng = readQueryNumber(route.query.swLng)
  const neLat = readQueryNumber(route.query.neLat)
  const neLng = readQueryNumber(route.query.neLng)

  if (swLat === null || swLng === null || neLat === null || neLng === null) {
    return null
  }

  return {
    swLat,
    swLng,
    neLat,
    neLng,
    zoomLevel: clampZoomLevel(readQueryNumber(route.query.zoomLevel))
  }
}

function hasMapStateQuery() {
  return (
    route.query.types !== undefined ||
    route.query.q !== undefined ||
    route.query.swLat !== undefined ||
    route.query.zoomLevel !== undefined ||
    route.query.priceLayer !== undefined
  )
}

function hasFavoriteAreaQuery() {
  return route.query.centerLat !== undefined || route.query.centerLng !== undefined
}

function restoreMapStateFromQuery(): MapViewport | null {
  const nextTypes = parsePropertyTypes(route.query.types)
  if (nextTypes !== null) {
    selectedPropertyTypes.value = nextTypes
  }

  const keyword = readQueryString(route.query.q) ?? ''
  searchKeyword.value = keyword
  activeSearchKeyword.value = keyword

  const priceLayer = readQueryBoolean(route.query.priceLayer)
  if (priceLayer !== null) {
    showAdministrativePriceLayer.value = priceLayer
  }

  const propertyId = readQueryNumber(route.query.selectedPropertyId)
  selectedPropertyId.value = propertyId === null ? null : Math.round(propertyId)

  const viewport = queryViewport()
  if (viewport) {
    currentViewport.value = viewport
    zoomLevel.value = viewport.zoomLevel
    mapFocusTarget.value = viewportCenter(viewport)
  }

  return viewport
}

function restoreMapStateFromSnapshot(snapshot: MapSearchSnapshot) {
  selectedPropertyTypes.value = [...snapshot.selectedPropertyTypes]
  zoomLevel.value = snapshot.zoomLevel
  currentViewport.value = { ...snapshot.currentViewport }
  searchKeyword.value = snapshot.searchKeyword
  activeSearchKeyword.value = snapshot.activeSearchKeyword
  keywordSearchPage.value = snapshot.keywordSearchPage
  keywordSearchTotal.value = snapshot.keywordSearchTotal
  showAdministrativePriceLayer.value = snapshot.showAdministrativePriceLayer
  selectedPropertyId.value = snapshot.selectedPropertyId
  mapFocusTarget.value = snapshot.mapFocusTarget ? { ...snapshot.mapFocusTarget } : null
  items.value = [...snapshot.items]
  administrativeClusters.value = [...snapshot.administrativeClusters]
  addAutocompleteCandidates(items.value)
}

function currentSnapshot(): MapSearchSnapshot {
  return {
    selectedPropertyTypes: selectedPropertyTypes.value,
    zoomLevel: zoomLevel.value,
    currentViewport: currentViewport.value,
    searchKeyword: searchKeyword.value,
    activeSearchKeyword: activeSearchKeyword.value,
    keywordSearchPage: keywordSearchPage.value,
    keywordSearchTotal: keywordSearchTotal.value,
    showAdministrativePriceLayer: showAdministrativePriceLayer.value,
    selectedPropertyId: selectedPropertyId.value,
    mapFocusTarget: mapFocusTarget.value,
    items: items.value,
    administrativeClusters: administrativeClusters.value
  }
}

function mapQuery() {
  return {
    types: selectedPropertyTypes.value.length ? selectedPropertyTypes.value.join(',') : 'none',
    q: activeSearchKeyword.value || undefined,
    swLat: String(roundCoordinate(currentViewport.value.swLat)),
    swLng: String(roundCoordinate(currentViewport.value.swLng)),
    neLat: String(roundCoordinate(currentViewport.value.neLat)),
    neLng: String(roundCoordinate(currentViewport.value.neLng)),
    zoomLevel: String(zoomLevel.value),
    priceLayer: showAdministrativePriceLayer.value ? '1' : undefined,
    selectedPropertyId: selectedPropertyId.value ? String(selectedPropertyId.value) : undefined
  }
}

function sameQuery(left: Record<string, unknown>, right: Record<string, unknown>) {
  const keys = new Set([...Object.keys(left), ...Object.keys(right)])
  for (const key of keys) {
    if ((left[key] ?? undefined) !== (right[key] ?? undefined)) {
      return false
    }
  }
  return true
}

function persistMapState(updateRoute = true) {
  mapSearchStore.save(currentSnapshot())

  if (!updateRoute || route.name !== 'map') {
    return
  }

  const nextQuery = mapQuery()
  if (sameQuery(route.query, nextQuery)) {
    return
  }

  void router.replace({ name: 'map', query: nextQuery }).catch(() => {})
}

function restoreFavoriteAreaFromQuery(): MapViewport | null {
  const centerLat = readQueryNumber(route.query.centerLat)
  const centerLng = readQueryNumber(route.query.centerLng)

  if (centerLat === null || centerLng === null) {
    return null
  }

  const nextZoomLevel = clampZoomLevel(readQueryNumber(route.query.zoomLevel))
  const nextViewport = viewportAroundCenter({ lat: centerLat, lng: centerLng }, nextZoomLevel)
  currentViewport.value = nextViewport
  zoomLevel.value = nextViewport.zoomLevel
  mapFocusTarget.value = { lat: centerLat, lng: centerLng }

  const label = readQueryString(route.query.areaLabel)
  if (label) {
    areaFavoriteMessage.value = `${label} 관심 지역을 지도로 불러왔습니다.`
  }

  return nextViewport
}

function selectedOrFirstResult(): PropertyMapItem | null {
  return (
    items.value.find((item) => item.propertyId === selectedPropertyId.value) ??
    items.value.find((item) => Number.isFinite(item.lat) && Number.isFinite(item.lng)) ??
    null
  )
}

function buildFavoriteAreaPayload(): FavoriteAreaCreateRequest {
  const selectedResult = isKeywordSearch.value ? selectedOrFirstResult() : null
  const center = selectedResult
    ? { lat: selectedResult.lat, lng: selectedResult.lng }
    : viewportCenter(currentViewport.value)

  return {
    label: currentAreaLabel.value,
    centerLat: roundCoordinate(center.lat),
    centerLng: roundCoordinate(center.lng),
    zoomLevel: zoomLevel.value
  }
}

function setAreaFavoriteFailure(error: unknown) {
  const apiError = getApiError(error)

  if (apiError?.code === 'FAVORITE_AREA_ALREADY_EXISTS') {
    areaFavoriteMessage.value = '이미 관심 지역에 저장되어 있습니다.'
    return
  }

  if (apiError?.code === 'VALIDATION_FAILED') {
    areaFavoriteErrorMessage.value = '관심 지역 정보를 저장할 수 없습니다. 지도를 이동한 뒤 다시 시도해 주세요.'
    return
  }

  if (apiError?.code === 'AUTH_REQUIRED') {
    areaFavoriteErrorMessage.value = '로그인이 필요한 기능입니다. 로그인 후 다시 시도해 주세요.'
    return
  }

  areaFavoriteErrorMessage.value = '관심 지역을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.'
}

async function saveCurrentAreaFavorite() {
  areaFavoriteMessage.value = ''
  areaFavoriteErrorMessage.value = ''

  if (!authStore.isAuthenticated) {
    areaFavoriteErrorMessage.value = '로그인 후 관심 지역을 저장할 수 있습니다.'
    return
  }

  areaFavoriteLoading.value = true

  try {
    await favoritesApi.addArea(buildFavoriteAreaPayload())
    areaFavoriteMessage.value = '관심 지역에 추가했습니다.'
  } catch (error) {
    setAreaFavoriteFailure(error)
    errorMessage.value =
      '실거래 데이터 API 연결에 실패했습니다. 백엔드 서버 실행 상태와 VITE_API_BASE_URL, DB_PORT 설정을 확인해 주세요.'
  } finally {
    areaFavoriteLoading.value = false
  }
}

function toMapItem(item: PropertySearchItem): PropertyMapItem {
  return {
    propertyId: item.propertyId,
    propertyType: item.propertyType,
    name: item.name,
    address: item.address,
    lat: item.lat,
    lng: item.lng,
    latestTransaction: item.latestTransaction,
    dealCount: item.latestTransaction ? 1 : 0,
    recentTransactionCount: 0,
    aiAvailable: item.aiAvailable
  }
}

function focusFirstResult(nextItems: PropertyMapItem[]) {
  const firstItem = nextItems.find((item) => Number.isFinite(item.lat) && Number.isFinite(item.lng))
  selectedPropertyId.value = firstItem?.propertyId ?? null
  mapFocusTarget.value = firstItem ? { lat: firstItem.lat, lng: firstItem.lng } : null
}

function clearAutocompleteTimer() {
  if (autocompleteTimer) {
    window.clearTimeout(autocompleteTimer)
    autocompleteTimer = null
  }
}

function addAutocompleteCandidates(nextItems: Array<PropertyMapItem | PropertySearchItem>) {
  for (const item of nextItems) {
    autocompleteTrie.insert(item.name)
    autocompleteTrie.insert(item.address)
    if ('legalDong' in item && item.legalDong) {
      autocompleteTrie.insert(item.legalDong)
    }
  }
  refreshAutocompleteSuggestions()
}

function refreshAutocompleteSuggestions() {
  const keyword = searchKeyword.value.trim()
  autocompleteSuggestions.value = autocompleteTrie
    .suggest(keyword)
    .filter((suggestion) => suggestion !== keyword)
  highlightedSuggestionIndex.value = autocompleteSuggestions.value.length ? 0 : -1
  autocompleteOpen.value = keyword.length > 0 && autocompleteSuggestions.value.length > 0
}

async function fetchAutocompleteCandidates(keyword: string) {
  if (!keyword.trim()) {
    return
  }

  try {
    const response = await propertyApi.searchProperties({
      keyword,
      propertyTypes: selectedPropertyTypes.value,
      page: 0,
      size: autocompleteFetchSize,
      sort: 'relevance,desc'
    })
    addAutocompleteCandidates(response.items)
  } catch {
    refreshAutocompleteSuggestions()
  }
}

function handleSearchInput() {
  refreshAutocompleteSuggestions()
  clearAutocompleteTimer()

  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    autocompleteOpen.value = false
    return
  }

  autocompleteTimer = window.setTimeout(() => {
    void fetchAutocompleteCandidates(keyword)
  }, 180)
}

function handleSearchFocus() {
  refreshAutocompleteSuggestions()
}

function closeAutocompleteSoon() {
  window.setTimeout(() => {
    autocompleteOpen.value = false
  }, 120)
}

function moveAutocompleteHighlight(direction: 1 | -1) {
  if (!autocompleteSuggestions.value.length) {
    return
  }
  autocompleteOpen.value = true
  highlightedSuggestionIndex.value =
    (highlightedSuggestionIndex.value + direction + autocompleteSuggestions.value.length) %
    autocompleteSuggestions.value.length
}

function applyAutocompleteSuggestion(suggestion: string) {
  searchKeyword.value = suggestion
  autocompleteOpen.value = false
  clearAutocompleteTimer()
  void searchByKeyword(suggestion)
}

function handleAutocompleteEnter(event: KeyboardEvent) {
  if (!autocompleteOpen.value || highlightedSuggestionIndex.value < 0) {
    return
  }

  event.preventDefault()
  applyAutocompleteSuggestion(autocompleteSuggestions.value[highlightedSuggestionIndex.value])
}

async function searchVisibleArea(viewport: MapViewport = currentViewport.value) {
  loading.value = true
  errorMessage.value = ''
  activeSearchKeyword.value = ''
  keywordSearchPage.value = 0
  keywordSearchTotal.value = null
  mapFocusTarget.value = null
  currentViewport.value = viewport
  zoomLevel.value = viewport.zoomLevel

  try {
    const response = await propertyApi.getMapProperties({
      swLat: viewport.swLat,
      swLng: viewport.swLng,
      neLat: viewport.neLat,
      neLng: viewport.neLng,
      zoomLevel: viewport.zoomLevel,
      propertyTypes: selectedPropertyTypes.value
    })
    const nextItems = normalizePropertyMapItems(response.items)
    items.value = nextItems
    addAutocompleteCandidates(nextItems)
    administrativeClusters.value = response.administrativeClusters ?? []
    if (!nextItems.some((item) => item.propertyId === selectedPropertyId.value)) {
      selectedPropertyId.value = null
    }
  } catch {
    items.value = []
    administrativeClusters.value = []
    errorMessage.value = '실거래 데이터 API 연결에 실패했습니다. 백엔드 서버 실행 상태를 확인해 주세요.'
  } finally {
    loading.value = false
    persistMapState()
  }
}

async function searchByKeyword(keyword: string, page = 0, append = false) {
  if (append) {
    keywordLoadingMore.value = true
  } else {
    loading.value = true
  }
  errorMessage.value = ''
  activeSearchKeyword.value = keyword
  administrativeClusters.value = []
  autocompleteOpen.value = false

  try {
    const response = await propertyApi.searchProperties({
      keyword,
      propertyTypes: selectedPropertyTypes.value,
      page,
      size: keywordSearchPageSize,
      sort: 'relevance,desc'
    })
    const fetchedItems = response.items.map(toMapItem)
    addAutocompleteCandidates(response.items)
    const nextItems = normalizePropertyMapItems(append ? [...items.value, ...fetchedItems] : fetchedItems)
    items.value = nextItems
    keywordSearchPage.value = response.page.number
    keywordSearchTotal.value = response.page.totalElements

    if (!append) {
      focusFirstResult(nextItems)
    }
  } catch {
    if (!append) {
      items.value = []
      keywordSearchTotal.value = null
      selectedPropertyId.value = null
      mapFocusTarget.value = null
    }
    administrativeClusters.value = []
    errorMessage.value = '검색 결과를 불러오지 못했습니다. 검색어를 확인하거나 잠시 후 다시 시도해 주세요.'
  } finally {
    loading.value = false
    keywordLoadingMore.value = false
    persistMapState()
  }
}

function loadMoreKeywordResults() {
  if (!activeSearchKeyword.value || keywordLoadingMore.value || !hasMoreKeywordResults.value) {
    return
  }

  void searchByKeyword(activeSearchKeyword.value, keywordSearchPage.value + 1, true)
}

function handleKeywordSubmit() {
  const keyword = searchKeyword.value.trim()
  clearAutocompleteTimer()
  autocompleteOpen.value = false

  if (!keyword) {
    searchKeyword.value = ''
    void searchVisibleArea()
    return
  }

  void searchByKeyword(keyword)
}

function resetToVisibleArea() {
  searchKeyword.value = ''
  void searchVisibleArea()
}

function scheduleSearch(viewport: MapViewport) {
  if (isKeywordSearch.value) {
    return
  }

  if (searchTimer) {
    window.clearTimeout(searchTimer)
  }

  searchTimer = window.setTimeout(() => {
    void searchVisibleArea(viewport)
  }, 250)
}

function handleMapReady(viewport: MapViewport) {
  if (restoredFromSnapshot) {
    return
  }

  const nextViewport = pendingInitialViewport ?? viewport
  currentViewport.value = nextViewport
  zoomLevel.value = nextViewport.zoomLevel
  if (!isKeywordSearch.value) {
    void searchVisibleArea(nextViewport)
  }
  pendingInitialViewport = null
}

function handleBoundsChanged(viewport: MapViewport) {
  currentViewport.value = viewport
  zoomLevel.value = viewport.zoomLevel
  scheduleSearch(viewport)
}

function handleMapLoadError() {
  void searchVisibleArea(SEOUL_SEED_VIEWPORT)
}

function selectProperty(propertyId: number) {
  selectedPropertyId.value = propertyId
  persistMapState()
  void router.push(`/properties/${propertyId}`)
}

onMounted(() => {
  const cachedSnapshot = mapSearchStore.snapshot
  if (cachedSnapshot && !hasFavoriteAreaQuery()) {
    restoreMapStateFromSnapshot(cachedSnapshot)
    restoredFromSnapshot = true
  }

  const restoredQueryViewport = hasMapStateQuery() ? restoreMapStateFromQuery() : null
  if (restoredQueryViewport) {
    pendingInitialViewport = restoredQueryViewport
  }
  const restoredViewport = restoredQueryViewport ?? (restoredFromSnapshot ? currentViewport.value : restoreFavoriteAreaFromQuery())

  if (!hasMapKey) {
    if (!restoredFromSnapshot) {
      if (activeSearchKeyword.value) {
        void searchByKeyword(activeSearchKeyword.value)
      } else {
        void searchVisibleArea(restoredViewport ?? SEOUL_SEED_VIEWPORT)
      }
    }
  } else if (!restoredFromSnapshot && activeSearchKeyword.value) {
    void searchByKeyword(activeSearchKeyword.value)
  }

  mounted = true
})

onBeforeUnmount(() => {
  if (searchTimer) {
    window.clearTimeout(searchTimer)
  }
  clearAutocompleteTimer()
})

watch([selectedPropertyTypes, showAdministrativePriceLayer], () => {
  if (mounted) {
    persistMapState()
  }
}, { deep: true })
</script>

<template>
  <div class="map-shell">
    <!-- Left panel -->
    <aside class="map-left" aria-label="지도 검색 조건">
      <!-- Search -->
      <form class="map-search-form" data-test="map-search-form" @submit.prevent="handleKeywordSubmit">
        <label class="sr-only" for="map-search-keyword">단지명 또는 지역 검색</label>
        <div class="map-search-row">
          <input
            id="map-search-keyword"
            v-model="searchKeyword"
            data-test="map-search-keyword"
            type="search"
            autocomplete="off"
            role="combobox"
            aria-autocomplete="list"
            aria-controls="map-search-suggestions"
            :aria-expanded="autocompleteOpen"
            placeholder="경희궁롯데캐슬 · 무악동 · 구 검색"
            class="map-input"
            @input="handleSearchInput"
            @focus="handleSearchFocus"
            @blur="closeAutocompleteSoon"
            @keydown.down.prevent="moveAutocompleteHighlight(1)"
            @keydown.up.prevent="moveAutocompleteHighlight(-1)"
            @keydown.enter="handleAutocompleteEnter"
          />
          <button class="map-search-btn" type="submit" aria-label="검색">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" viewBox="0 0 24 24">
              <circle cx="11" cy="11" r="8" stroke="currentColor" stroke-width="2"/>
              <path stroke="currentColor" stroke-linecap="round" stroke-width="2" d="M21 21l-4.35-4.35"/>
            </svg>
          </button>
        </div>
        <ul
          v-if="autocompleteOpen && autocompleteSuggestions.length"
          id="map-search-suggestions"
          class="autocomplete-list"
          data-test="map-search-suggestions"
          role="listbox"
        >
          <li
            v-for="(suggestion, index) in autocompleteSuggestions"
            :key="suggestion"
            role="option"
            :aria-selected="highlightedSuggestionIndex === index"
          >
            <button
              class="autocomplete-option"
              :class="{ 'is-active': highlightedSuggestionIndex === index }"
              type="button"
              @mousedown.prevent="applyAutocompleteSuggestion(suggestion)"
            >
              {{ suggestion }}
            </button>
          </li>
        </ul>
        <button
          v-if="isKeywordSearch"
          class="text-button full-width"
          type="button"
          style="font-size:0.8rem; margin-top:4px;"
          :disabled="loading"
          @click="resetToVisibleArea"
        >검색어 초기화</button>
      </form>

      <!-- Filters -->
      <div class="filter-section">
        <fieldset>
          <legend>매물유형</legend>
          <div class="check-group">
            <label v-for="type in propertyTypeOptions" :key="type" class="check-row">
              <input v-model="selectedPropertyTypes" :value="type" type="checkbox" />
              <span>{{ propertyTypeLabel(type) }}</span>
            </label>
          </div>
        </fieldset>

        <fieldset>
          <legend>지도 표시</legend>
          <label class="check-row">
            <input
              v-model="showAdministrativePriceLayer"
              data-test="administrative-price-layer-toggle"
              type="checkbox"
            />
            <span>평균 거래금액 색상</span>
          </label>
        </fieldset>

      </div>

      <button class="search-area-btn" type="button" :disabled="loading" @click="resetToVisibleArea">
        {{ loading ? '검색 중...' : '현재 화면 범위 검색' }}
      </button>

      <!-- Area favorite -->
      <div class="area-fav">
        <button
          class="area-fav-btn"
          data-test="area-favorite-button"
          type="button"
          :disabled="areaFavoriteLoading"
          @click="saveCurrentAreaFavorite"
        >
          {{ areaFavoriteLoading ? '저장 중...' : '이 지역 관심 등록' }}
        </button>
        <p v-if="areaFavoriteMessage" class="helper-text" style="font-size:0.78rem; margin-top:6px;">{{ areaFavoriteMessage }}</p>
        <p v-if="areaFavoriteErrorMessage" class="inline-error" style="font-size:0.78rem; margin-top:6px;">{{ areaFavoriteErrorMessage }}</p>
      </div>

      <p class="helper-text" style="font-size:0.78rem; margin-top:8px;">{{ mapRuntimeMessage }}</p>
      <p v-if="errorMessage" class="inline-error" style="font-size:0.8rem; margin-top:8px;">{{ errorMessage }}</p>
      <p v-if="errorMessage" class="helper-text" style="font-size:0.78rem; padding:0 16px;">
        실거래 데이터 API 연결에 실패했습니다. VITE_API_BASE_URL, DB_PORT 설정을 확인해 주세요.
      </p>

      <!-- Results list -->
      <div class="result-divider">
        <span>검색 결과 {{ resultCountText }}</span>
      </div>

      <p class="helper-text" style="font-size:0.78rem; padding: 0 16px 8px;">{{ resultDescription }}</p>

      <ul v-if="items.length" class="map-result-list result-list">
        <li
          v-for="item in items"
          :id="`property-result-${item.propertyId}`"
          :key="item.propertyId"
          :class="{ 'is-selected': selectedPropertyId === item.propertyId }"
          @mouseenter="selectedPropertyId = item.propertyId"
          @focusin="selectedPropertyId = item.propertyId"
        >
          <RouterLink
            :to="`/properties/${item.propertyId}`"
            class="map-result-link"
            @click="selectedPropertyId = item.propertyId; persistMapState()"
          >
            <strong class="result-name">{{ item.name }}</strong>
            <span class="result-addr">{{ item.address }}</span>
            <span v-if="item.latestTransaction" class="result-price">
              {{ transactionTypeLabel(item.latestTransaction.transactionType) }} · {{ formatLatestTransactionAmount(item.latestTransaction) }}
            </span>
          </RouterLink>
        </li>
      </ul>

      <button
        v-if="hasMoreKeywordResults"
        class="load-more-btn"
        data-test="keyword-load-more"
        type="button"
        :disabled="keywordLoadingMore"
        @click="loadMoreKeywordResults"
      >
        {{ keywordLoadingMore ? '더 불러오는 중...' : '검색 결과 더 보기' }}
      </button>

      <EmptyState
        v-else-if="!loading"
        title="검색 결과 없음"
        :description="isKeywordSearch ? '검색어나 조건을 변경해 보세요.' : '지도를 이동하거나 조건을 조정해 보세요.'"
      />
    </aside>

    <!-- Map -->
    <div class="map-right">
      <KakaoMapPanel
        :items="items"
        :administrative-clusters="administrativeClusters"
        :show-administrative-price-layer="showAdministrativePriceLayer"
        :selected-property-id="selectedPropertyId"
        :focus-target="mapFocusTarget"
        :focus-zoom-level="zoomLevel"
        @ready="handleMapReady"
        @bounds-changed="handleBoundsChanged"
        @property-selected="selectProperty"
        @load-error="handleMapLoadError"
      />
    </div>
  </div>

  <FloatingChat />
</template>

<style scoped>
.map-shell {
  display: grid;
  grid-template-columns: 300px 1fr;
  height: calc(100vh - var(--header-h));
  overflow: hidden;
}

/* Left panel */
.map-left {
  border-right: 1px solid var(--border);
  background: var(--bg);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: none;
}

.map-left::-webkit-scrollbar { display: none; }

.map-search-form { padding: 14px 14px 0; flex-shrink: 0; }

.map-search-row {
  display: flex;
  gap: 6px;
  align-items: center;
}

.map-input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: rgba(255,255,255,0.04);
  color: var(--cream);
  font-size: 0.86rem;
  min-width: 0;
  width: 100%;
}
.map-input:focus { border-color: var(--border-strong); outline: none; }
.map-input::placeholder { color: rgba(154,128,96,0.6); }

.map-search-btn {
  width: 36px;
  height: 36px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: transparent;
  color: var(--cream-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
}
.map-search-btn:hover { color: var(--cream); border-color: var(--border-strong); }

.autocomplete-list {
  list-style: none;
  margin: 6px 0 0;
  padding: 4px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg);
  box-shadow: 0 8px 18px rgba(0,0,0,0.2);
}

.autocomplete-option {
  width: 100%;
  padding: 8px 10px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--cream-muted);
  cursor: pointer;
  font: inherit;
  font-size: 0.82rem;
  text-align: left;
}
.autocomplete-option:hover,
.autocomplete-option.is-active {
  background: rgba(200,160,100,0.1);
  color: var(--cream);
}

/* Filter section */
.filter-section {
  padding: 14px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.filter-section fieldset {
  margin: 0 0 12px;
  padding: 0;
  border: 0;
}

.filter-section legend {
  font-size: 0.76rem;
  font-weight: 700;
  color: var(--cream-muted);
  letter-spacing: 0.08em;
  margin-bottom: 8px;
  display: block;
}

.check-group { display: grid; gap: 4px; }

.filter-section .field-label {
  display: block;
  font-size: 0.76rem;
  font-weight: 700;
  color: var(--cream-muted);
  letter-spacing: 0.08em;
  margin-bottom: 6px;
}

.search-area-btn {
  margin: 10px 14px;
  width: calc(100% - 28px);
  padding: 10px;
  background: var(--gold);
  color: #1a1208;
  border: none;
  border-radius: 8px;
  font-size: 0.86rem;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.2s;
  flex-shrink: 0;
}
.search-area-btn:hover:not(:disabled) { background: var(--gold-light); }
.search-area-btn:disabled { opacity: 0.6; }

.area-fav {
  padding: 8px 14px 12px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.area-fav-btn {
  width: 100%;
  padding: 8px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--cream-muted);
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s;
}
.area-fav-btn:hover:not(:disabled) { color: var(--gold); border-color: rgba(200,160,100,0.3); }
.area-fav-btn:disabled { opacity: 0.5; }

.result-divider {
  padding: 12px 14px 0;
  font-size: 0.76rem;
  font-weight: 700;
  color: var(--cream-muted);
  letter-spacing: 0.08em;
  flex-shrink: 0;
}

/* Result list */
.map-result-list {
  list-style: none;
  margin: 0;
  padding: 0 8px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.map-result-list li {
  border-radius: 8px;
  transition: background 0.15s;
}
.map-result-list li:hover, .map-result-list li.is-selected { background: rgba(200,160,100,0.08); }
.map-result-list li.is-selected { border-left: 2px solid var(--gold); }

.map-result-link {
  display: grid;
  gap: 2px;
  padding: 10px 12px;
  color: var(--cream);
}
.map-result-link:hover { color: var(--cream); }

.result-name { font-size: 0.9rem; font-weight: 700; color: var(--cream); }
.result-addr { font-size: 0.78rem; color: var(--cream-muted); }
.result-price { font-size: 0.82rem; color: var(--gold); font-weight: 600; }

.load-more-btn {
  margin: 0 14px 16px;
  width: calc(100% - 28px);
  padding: 9px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: transparent;
  color: var(--cream-muted);
  cursor: pointer;
  font: inherit;
  font-size: 0.82rem;
  font-weight: 700;
}
.load-more-btn:hover:not(:disabled) {
  color: var(--gold);
  border-color: rgba(200,160,100,0.3);
}
.load-more-btn:disabled { opacity: 0.55; }

/* Map right */
.map-right {
  position: relative;
  height: calc(100vh - var(--header-h));
  overflow: hidden;
}

@media (max-width: 768px) {
  .map-shell { grid-template-columns: 1fr; grid-template-rows: auto 1fr; height: auto; }
  .map-left { max-height: 320px; }
  .map-right { height: 60vh; }
}
</style>
