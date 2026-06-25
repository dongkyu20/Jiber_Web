<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

import type { AdministrativeCluster, PropertyMapItem } from '@/api/types'
import {
  DEFAULT_MAP_CENTER,
  DEFAULT_MAP_LEVEL,
  clearKakaoPropertyClusterer,
  clearOverlayMarkers,
  mapMarkerRenderMode,
  screenPointsFromKakaoClusters,
  syncAdministrativeClusterOverlays,
  syncKakaoPropertyClusters,
  syncPropertyMarkerOverlays,
  viewportFromMap,
  type LatLngPoint,
  type KakaoClusterLike,
  type KakaoMapLike,
  type KakaoMapsApi,
  type KakaoMarkerClustererLike,
  type KakaoOverlayLike,
  type MapScreenPoint,
  type MapViewport
} from '@/map/kakaoMap'
import { getKakaoMapFallbackMessage, getKakaoMaps, hasKakaoMapKey, loadKakaoMaps } from '@/map/kakaoLoader'

const props = withDefaults(
  defineProps<{
    items: PropertyMapItem[]
    administrativeClusters?: AdministrativeCluster[]
    showAdministrativePriceLayer?: boolean
    selectedPropertyId?: number | null
    focusTarget?: LatLngPoint | null
    focusZoomLevel?: number | null
  }>(),
  {
    administrativeClusters: () => [],
    showAdministrativePriceLayer: false,
    selectedPropertyId: null,
    focusTarget: null,
    focusZoomLevel: null
  }
)

const emit = defineEmits<{
  ready: [viewport: MapViewport]
  boundsChanged: [viewport: MapViewport]
  propertySelected: [propertyId: number]
  loadError: [message: string]
}>()

const loading = ref(false)
const ready = ref(false)
const message = ref(getKakaoMapFallbackMessage())
const mapElement = ref<HTMLDivElement | null>(null)

let kakaoMaps: KakaoMapsApi | null = null
let map: KakaoMapLike | null = null
let propertyOverlays: KakaoOverlayLike[] = []
let propertyClusterer: KakaoMarkerClustererLike | null = null
let propertyClusterReservedPoints: MapScreenPoint[] = []
let administrativeOverlays: KakaoOverlayLike[] = []
let idleTimer: number | null = null
let renderQueued = false
let disposed = false
let lastRenderedModeKey: string | null = null

function targetLevelForAdministrativeCluster(cluster: AdministrativeCluster): number {
  return cluster.level === 'SIGUNGU' ? 5 : 3
}

function emitViewport(eventName: 'ready' | 'boundsChanged') {
  if (!map) {
    return
  }

  const viewport = viewportFromMap(map)

  if (eventName === 'ready') {
    emit('ready', viewport)
    return
  }

  emit('boundsChanged', viewport)
}

function scheduleBoundsChanged() {
  if (disposed) {
    return
  }

  renderMapLayersIfModeChanged()

  if (idleTimer) {
    window.clearTimeout(idleTimer)
  }

  idleTimer = window.setTimeout(() => emitViewport('boundsChanged'), 180)
}

function mapRenderModeKey(mode: ReturnType<typeof mapMarkerRenderMode>) {
  return [
    mode.showIndividualMarkers ? 'individual' : 'no-individual',
    mode.showPropertyClusterer ? 'property-cluster' : 'no-property-cluster',
    mode.showAdministrativeClusters ? 'administrative' : 'no-administrative'
  ].join('|')
}

function syncAdministrativeLayers() {
  if (!kakaoMaps || !map) {
    return
  }

  administrativeOverlays = syncAdministrativeClusterOverlays({
    kakaoMaps,
    map,
    previousOverlays: administrativeOverlays,
    clusters: props.administrativeClusters,
    reservedPoints: propertyClusterReservedPoints,
    showPriceTone: props.showAdministrativePriceLayer,
    onClick: zoomToAdministrativeCluster
  })
}

function renderMapLayers(options: { force?: boolean } = {}) {
  if (disposed || !kakaoMaps || !map) {
    return
  }

  const mode = mapMarkerRenderMode(map.getLevel())
  const modeKey = mapRenderModeKey(mode)

  if (!options.force && modeKey === lastRenderedModeKey) {
    return
  }

  lastRenderedModeKey = modeKey

  if (mode.showIndividualMarkers) {
    propertyOverlays = syncPropertyMarkerOverlays({
      kakaoMaps,
      map,
      previousOverlays: propertyOverlays,
      items: props.items,
      selectedPropertyId: props.selectedPropertyId,
      onClick: (propertyId) => emit('propertySelected', propertyId)
    })
  } else {
    clearOverlayMarkers(propertyOverlays)
    propertyOverlays = []
  }

  if (mode.showPropertyClusterer) {
    propertyClusterReservedPoints = []
    propertyClusterer = syncKakaoPropertyClusters({
      kakaoMaps,
      map,
      previousClusterer: propertyClusterer,
      items: props.items,
      showPriceTone: props.showAdministrativePriceLayer,
      onClustered: (clusters) => {
        if (disposed || !kakaoMaps || !map) {
          return
        }

        propertyClusterReservedPoints = screenPointsFromKakaoClusters(map, clusters)
        const currentMode = mapMarkerRenderMode(map.getLevel())
        if (currentMode.showAdministrativeClusters) {
          syncAdministrativeLayers()
        }
      },
      onClusterClick: zoomToPropertyCluster
    })
  } else {
    clearKakaoPropertyClusterer(propertyClusterer)
    propertyClusterer = null
    propertyClusterReservedPoints = []
  }

  if (mode.showAdministrativeClusters) {
    syncAdministrativeLayers()
  } else {
    clearOverlayMarkers(administrativeOverlays)
    administrativeOverlays = []
  }
}

function renderMapLayersIfModeChanged() {
  renderMapLayers()
}

function queueRenderMapLayers() {
  if (disposed || renderQueued) {
    return
  }

  renderQueued = true
  window.queueMicrotask(() => {
    renderQueued = false
    renderMapLayers({ force: true })
  })
}

function focusMap(target: LatLngPoint | null) {
  if (disposed || !target || !kakaoMaps || !map) {
    return
  }

  const latLng = new kakaoMaps.LatLng(target.lat, target.lng)
  if (props.focusZoomLevel) {
    map.setLevel?.(props.focusZoomLevel)
  }

  if (map.panTo) {
    map.panTo(latLng)
    return
  }

  map.setCenter?.(latLng)
}

function zoomToLatLng(latLng: unknown, level: number) {
  if (disposed || !map) {
    return
  }

  map.setLevel?.(level, { anchor: latLng, animate: true })

  if (map.panTo) {
    map.panTo(latLng)
    return
  }

  map.setCenter?.(latLng)
}

function zoomToPropertyCluster(cluster: KakaoClusterLike) {
  if (disposed || !map) {
    return
  }

  const currentLevel = map.getLevel()
  const bounds = cluster.getBounds?.()
  if (bounds && map.setBounds) {
    map.setBounds(bounds)
  }

  const center = cluster.getCenter?.()
  if (center) {
    const targetLevel = Math.max(1, Math.min(map.getLevel(), currentLevel - 1))
    zoomToLatLng(center, targetLevel)
  }
}

function zoomToAdministrativeCluster(cluster: AdministrativeCluster) {
  if (disposed || !kakaoMaps) {
    return
  }

  const latLng = new kakaoMaps.LatLng(cluster.centerLat, cluster.centerLng)
  zoomToLatLng(latLng, targetLevelForAdministrativeCluster(cluster))
}

function changeZoom(direction: 'in' | 'out') {
  if (disposed || !map) {
    return
  }

  const currentLevel = map.getLevel()
  const nextLevel = direction === 'in' ? Math.max(1, currentLevel - 1) : Math.min(14, currentLevel + 1)
  if (nextLevel === currentLevel) {
    return
  }

  map.setLevel?.(nextLevel)
  scheduleBoundsChanged()
}

onMounted(async () => {
  if (!hasKakaoMapKey()) {
    return
  }

  loading.value = true
  message.value = '카카오 지도를 불러오고 있습니다.'

  try {
    await loadKakaoMaps()
    if (disposed) {
      return
    }

    const maps = getKakaoMaps()
    if (!maps || !mapElement.value) {
      throw new Error('카카오 지도 SDK를 확인하지 못했습니다. JavaScript 키 설정을 확인해 주세요.')
    }

    kakaoMaps = maps
    map = new maps.Map(mapElement.value, {
      center: new maps.LatLng(DEFAULT_MAP_CENTER.lat, DEFAULT_MAP_CENTER.lng),
      level: DEFAULT_MAP_LEVEL
    })
    maps.event.addListener(map, 'idle', scheduleBoundsChanged)

    ready.value = true
    message.value = '지도를 움직이면 현재 화면 범위로 검색합니다.'
    await nextTick()
    map?.relayout?.()
    emitViewport('ready')
    renderMapLayers({ force: true })
    focusMap(props.focusTarget)
  } catch (error) {
    message.value = error instanceof Error ? error.message : '카카오 지도를 불러오지 못했습니다.'
    emit('loadError', message.value)
  } finally {
    loading.value = false
  }
})

watch(
  [() => props.items, () => props.selectedPropertyId, () => props.administrativeClusters, () => props.showAdministrativePriceLayer],
  queueRenderMapLayers,
  { deep: true }
)
watch(() => [props.focusTarget, props.focusZoomLevel], () => focusMap(props.focusTarget), { deep: true })

onBeforeUnmount(() => {
  disposed = true
  if (idleTimer) {
    window.clearTimeout(idleTimer)
  }
  idleTimer = null
  renderQueued = false
  lastRenderedModeKey = null
  clearOverlayMarkers(propertyOverlays)
  propertyOverlays = []
  clearKakaoPropertyClusterer(propertyClusterer)
  propertyClusterer = null
  propertyClusterReservedPoints = []
  clearOverlayMarkers(administrativeOverlays)
  administrativeOverlays = []
  map = null
  kakaoMaps = null
})
</script>

<template>
  <section class="map-panel" :class="{ 'is-ready': ready }" aria-label="지도 영역">
    <div ref="mapElement" class="map-canvas" aria-hidden="true"></div>
    <div v-if="!ready" class="map-grid" aria-hidden="true">
      <span class="map-pin pin-one"></span>
      <span class="map-pin pin-two"></span>
      <span class="map-pin pin-three"></span>
    </div>
    <div v-if="!ready" class="map-message">
      <strong>{{ ready ? '카카오 지도 준비 완료' : '지도 준비 중' }}</strong>
      <p>{{ loading ? '지도 리소스를 확인하고 있습니다.' : message }}</p>
    </div>
    <div v-if="ready" class="map-zoom-controls" aria-label="지도 확대/축소">
      <button class="map-zoom-button" type="button" aria-label="지도 확대" @click.stop="changeZoom('in')">+</button>
      <button class="map-zoom-button" type="button" aria-label="지도 축소" @click.stop="changeZoom('out')">-</button>
    </div>
    <div
      v-if="ready && props.showAdministrativePriceLayer && props.administrativeClusters.length"
      class="map-price-legend"
      data-test="administrative-price-legend"
      aria-label="평균 거래금액 색상 범례"
    >
      <span>낮음</span>
      <span class="map-price-legend-gradient" aria-hidden="true"></span>
      <span>높음</span>
    </div>
    <p v-if="ready" class="map-status">{{ message }}</p>
  </section>
</template>
