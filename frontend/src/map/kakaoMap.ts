import type { AdministrativeCluster, Bounds, PropertyMapItem } from '@/api/types'
import { formatKrw } from '@/utils/format'

export interface MapViewport extends Bounds {
  zoomLevel: number
}

export interface LatLngPoint {
  lat: number
  lng: number
}

export interface MapMarkerRenderMode {
  showIndividualMarkers: boolean
  showPropertyClusterer: boolean
  showAdministrativeClusters: boolean
}

export interface KakaoLatLngLike {
  getLat(): number
  getLng(): number
}

export interface KakaoBoundsLike {
  getSouthWest(): KakaoLatLngLike
  getNorthEast(): KakaoLatLngLike
}

export interface KakaoMapLike {
  getBounds(): KakaoBoundsLike
  getLevel(): number
  getProjection?(): KakaoProjectionLike
  setCenter?(latLng: unknown): void
  setLevel?(level: number): void
  panTo?(latLng: unknown): void
}

export interface KakaoPointLike {
  x?: number
  y?: number
  getX?: () => number
  getY?: () => number
}

export interface MapScreenPoint {
  x: number
  y: number
}

export interface KakaoProjectionLike {
  containerPointFromCoords(latLng: unknown): KakaoPointLike
}

export interface KakaoMarkerLike {
  setMap(map: KakaoMapLike | null): void
}

export interface KakaoOverlayLike {
  setMap(map: KakaoMapLike | null): void
}

export interface KakaoClusterLike {
  getMarkers(): KakaoMarkerLike[]
  getClusterMarker(): {
    setContent(content: string): void
  }
  getCenter?(): unknown
}

export interface KakaoMarkerClustererLike {
  addMarkers(markers: KakaoMarkerLike[]): void
  clear(): void
  setMap?(map: KakaoMapLike | null): void
}

export interface KakaoMapsApi {
  LatLng: new (lat: number, lng: number) => unknown
  Map: new (container: HTMLElement, options: { center: unknown; level: number }) => KakaoMapLike
  Marker: new (options: {
    map?: KakaoMapLike
    position: unknown
    title: string
    image?: unknown
    clickable?: boolean
    opacity?: number
  }) => KakaoMarkerLike
  MarkerClusterer?: new (options: {
    map: KakaoMapLike
    averageCenter: boolean
    minLevel: number
    gridSize: number
  }) => KakaoMarkerClustererLike
  CustomOverlay?: new (options: {
    map: KakaoMapLike
    position: unknown
    content: string | HTMLElement
    yAnchor?: number
  }) => KakaoOverlayLike
  MarkerImage?: new (src: string, size: unknown, options?: { offset?: unknown }) => unknown
  Size?: new (width: number, height: number) => unknown
  Point?: new (x: number, y: number) => unknown
  event: {
    addListener(target: unknown, eventName: string, handler: (...args: unknown[]) => void): void
  }
}

export const DEFAULT_MAP_CENTER: LatLngPoint = {
  lat: 37.5008,
  lng: 127.0366
}

export const DEFAULT_MAP_LEVEL = 5

export const SEOUL_SEED_VIEWPORT: MapViewport = {
  swLat: 37.48,
  swLng: 127.01,
  neLat: 37.52,
  neLng: 127.06,
  zoomLevel: DEFAULT_MAP_LEVEL
}

const ADMINISTRATIVE_OVERLAY_MIN_X_DISTANCE_PX = 154
const ADMINISTRATIVE_OVERLAY_MIN_Y_DISTANCE_PX = 86

export function boundsFromKakao(bounds: KakaoBoundsLike, zoomLevel: number): MapViewport {
  const southWest = bounds.getSouthWest()
  const northEast = bounds.getNorthEast()

  return {
    swLat: southWest.getLat(),
    swLng: southWest.getLng(),
    neLat: northEast.getLat(),
    neLng: northEast.getLng(),
    zoomLevel
  }
}

export function viewportFromMap(map: KakaoMapLike): MapViewport {
  return boundsFromKakao(map.getBounds(), map.getLevel())
}

export function clearMarkers(markers: KakaoMarkerLike[]) {
  markers.forEach((marker) => marker.setMap(null))
}

export function clearOverlayMarkers(overlays: KakaoOverlayLike[]) {
  overlays.forEach((overlay) => overlay.setMap(null))
}

export function clearKakaoPropertyClusterer(clusterer: KakaoMarkerClustererLike | null) {
  clusterer?.clear()
  clusterer?.setMap?.(null)
}

export function mapMarkerRenderMode(zoomLevel: number): MapMarkerRenderMode {
  if (zoomLevel <= 3) {
    return {
      showIndividualMarkers: true,
      showPropertyClusterer: false,
      showAdministrativeClusters: false
    }
  }

  if (zoomLevel <= 5) {
    return {
      showIndividualMarkers: false,
      showPropertyClusterer: true,
      showAdministrativeClusters: zoomLevel >= 5
    }
  }

  return {
    showIndividualMarkers: false,
    showPropertyClusterer: false,
    showAdministrativeClusters: true
  }
}

export function sumRecentTransactionCount(items: PropertyMapItem[]): number {
  return items.reduce((total, item) => total + (item.recentTransactionCount ?? 0), 0)
}

function transactionTime(transaction: PropertyMapItem['latestTransaction']): number {
  if (!transaction?.dealDate) {
    return Number.NEGATIVE_INFINITY
  }

  const parsed = Date.parse(transaction.dealDate)
  return Number.isFinite(parsed) ? parsed : Number.NEGATIVE_INFINITY
}

function isRecentYearTransaction(transaction: PropertyMapItem['latestTransaction'], referenceDate: Date): boolean {
  const parsed = transactionTime(transaction)
  if (!Number.isFinite(parsed)) {
    return false
  }

  const oneYearMs = 365 * 24 * 60 * 60 * 1000
  return parsed >= referenceDate.getTime() - oneYearMs && parsed <= referenceDate.getTime()
}

function average(values: number[]): number | null {
  if (!values.length) {
    return null
  }

  return Math.round(values.reduce((total, value) => total + value, 0) / values.length)
}

export function normalizePropertyMapItems(items: PropertyMapItem[], referenceDate = new Date()): PropertyMapItem[] {
  const grouped = new Map<
    number,
    {
      base: PropertyMapItem
      latestTransaction: PropertyMapItem['latestTransaction']
      recentSaleDealAmounts: number[]
      recentJeonseDepositAmounts: number[]
      recentTransactionRows: number
      providedSaleAverage: number | null
      providedJeonseAverage: number | null
      maxDealCount: number
      maxRecentTransactionCount: number
      rowCount: number
    }
  >()

  items.forEach((item) => {
    const current = grouped.get(item.propertyId)
    const providedSaleAverage =
      typeof item.recentYearAverageDealAmount === 'number' ? item.recentYearAverageDealAmount : null
    const providedJeonseAverage =
      typeof item.recentYearAverageJeonseDepositAmount === 'number'
        ? item.recentYearAverageJeonseDepositAmount
        : null
    const recentTransactionRows = isRecentYearTransaction(item.latestTransaction, referenceDate) ? 1 : 0
    const recentSaleDealAmounts =
      recentTransactionRows > 0 &&
      item.latestTransaction?.transactionType === 'SALE' &&
      typeof item.latestTransaction.dealAmount === 'number'
        ? [item.latestTransaction.dealAmount]
        : []
    const recentJeonseDepositAmounts =
      recentTransactionRows > 0 &&
      item.latestTransaction?.transactionType === 'JEONSE' &&
      typeof item.latestTransaction.dealAmount === 'number'
        ? [item.latestTransaction.dealAmount]
        : []

    if (!current) {
      grouped.set(item.propertyId, {
        base: item,
        latestTransaction: item.latestTransaction ?? null,
        recentSaleDealAmounts,
        recentJeonseDepositAmounts,
        recentTransactionRows,
        providedSaleAverage,
        providedJeonseAverage,
        maxDealCount: item.dealCount ?? 0,
        maxRecentTransactionCount: item.recentTransactionCount ?? 0,
        rowCount: 1
      })
      return
    }

    current.rowCount += 1
    current.recentSaleDealAmounts.push(...recentSaleDealAmounts)
    current.recentJeonseDepositAmounts.push(...recentJeonseDepositAmounts)
    current.recentTransactionRows += recentTransactionRows
    current.maxDealCount = Math.max(current.maxDealCount, item.dealCount ?? 0)
    current.maxRecentTransactionCount = Math.max(current.maxRecentTransactionCount, item.recentTransactionCount ?? 0)
    if (providedSaleAverage !== null) {
      current.providedSaleAverage = providedSaleAverage
    }
    if (providedJeonseAverage !== null) {
      current.providedJeonseAverage = providedJeonseAverage
    }
    if (transactionTime(item.latestTransaction) > transactionTime(current.latestTransaction)) {
      current.latestTransaction = item.latestTransaction ?? null
    }
  })

  return Array.from(grouped.values()).map((group) => ({
    ...group.base,
    latestTransaction: group.latestTransaction,
    dealCount: Math.max(group.maxDealCount, group.rowCount),
    recentTransactionCount: Math.max(group.maxRecentTransactionCount, group.recentTransactionRows),
    recentYearAverageDealAmount: group.providedSaleAverage ?? average(group.recentSaleDealAmounts),
    recentYearAverageJeonseDepositAmount:
      group.providedJeonseAverage ?? average(group.recentJeonseDepositAmounts)
  }))
}

export function formatAdministrativeClusterLabel(cluster: AdministrativeCluster): string {
  const averageLabel =
    typeof cluster.averageDealAmount === 'number'
      ? `평균 ${formatKrw(cluster.averageDealAmount)}`
      : '평균 정보 없음'

  return `${cluster.label}\n${averageLabel}\n거래 ${cluster.transactionCount.toLocaleString('ko-KR')}건`
}

function propertyClusterBadgeContent(count: number): string {
  const formattedCount = count.toLocaleString('ko-KR')

  return [
    '<div class="map-property-cluster" aria-label="부동산 수 클러스터">',
    '<span class="map-property-cluster-label">부동산 수</span>',
    `<strong class="map-property-cluster-count">${formattedCount}</strong>`,
    '</div>'
  ].join('')
}

function escapeHtml(value: string): string {
  return value.replace(/[&<>"']/g, (character) => {
    switch (character) {
      case '&':
        return '&amp;'
      case '<':
        return '&lt;'
      case '>':
        return '&gt;'
      case '"':
        return '&quot;'
      case "'":
        return '&#39;'
      default:
        return character
    }
  })
}

function administrativeClusterContent(cluster: AdministrativeCluster): string {
  const [areaLabel, averageLabel, countLabel] = formatAdministrativeClusterLabel(cluster).split('\n')

  return [
    `<div class="map-admin-cluster" data-cluster-id="${escapeHtml(cluster.clusterId)}">`,
    `<strong>${escapeHtml(areaLabel)}</strong>`,
    `<span>${escapeHtml(averageLabel)}</span>`,
    `<span>${escapeHtml(countLabel)}</span>`,
    '</div>'
  ].join('')
}

function pointCoordinate(point: KakaoPointLike, axis: 'x' | 'y'): number | null {
  const directValue = point[axis]
  if (typeof directValue === 'number' && Number.isFinite(directValue)) {
    return directValue
  }

  const getter = axis === 'x' ? point.getX : point.getY
  if (!getter) {
    return null
  }

  const getterValue = getter()
  return Number.isFinite(getterValue) ? getterValue : null
}

function screenPointFromCoords(map: KakaoMapLike, latLng: unknown): MapScreenPoint | null {
  const projection = map.getProjection?.()
  if (!projection) {
    return null
  }

  const point = projection.containerPointFromCoords(latLng)
  const x = pointCoordinate(point, 'x')
  const y = pointCoordinate(point, 'y')

  if (x === null || y === null) {
    return null
  }

  return { x, y }
}

function administrativeClusterPoint(
  kakaoMaps: KakaoMapsApi,
  map: KakaoMapLike,
  cluster: AdministrativeCluster
): MapScreenPoint | null {
  return screenPointFromCoords(map, new kakaoMaps.LatLng(cluster.centerLat, cluster.centerLng))
}

export function screenPointsFromKakaoClusters(map: KakaoMapLike, clusters: KakaoClusterLike[]): MapScreenPoint[] {
  return clusters.reduce<MapScreenPoint[]>((points, cluster) => {
    const center = cluster.getCenter?.()
    const screenPoint = center ? screenPointFromCoords(map, center) : null

    if (screenPoint) {
      points.push(screenPoint)
    }

    return points
  }, [])
}

function administrativeClusterPriority(cluster: AdministrativeCluster): number {
  return cluster.transactionCount * 100000 + cluster.propertyCount
}

function pointsOverlap(first: MapScreenPoint, second: MapScreenPoint): boolean {
  return (
    Math.abs(first.x - second.x) < ADMINISTRATIVE_OVERLAY_MIN_X_DISTANCE_PX &&
    Math.abs(first.y - second.y) < ADMINISTRATIVE_OVERLAY_MIN_Y_DISTANCE_PX
  )
}

function nonOverlappingAdministrativeClusters(
  kakaoMaps: KakaoMapsApi,
  map: KakaoMapLike,
  clusters: AdministrativeCluster[],
  reservedPoints: MapScreenPoint[] = []
): AdministrativeCluster[] {
  const positionedClusters = clusters.map((cluster, index) => ({
    cluster,
    index,
    point: administrativeClusterPoint(kakaoMaps, map, cluster)
  }))

  if (positionedClusters.some(({ point }) => point === null)) {
    return clusters
  }

  const selected: typeof positionedClusters = []
  const byPriority = [...positionedClusters].sort((first, second) => {
    const priorityGap = administrativeClusterPriority(second.cluster) - administrativeClusterPriority(first.cluster)
    return priorityGap || first.index - second.index
  })

  byPriority.forEach((candidate) => {
    const candidatePoint = candidate.point
    if (!candidatePoint) {
      return
    }

    const overlapsSelected = selected.some((selectedCluster) => {
      const selectedPoint = selectedCluster.point
      return selectedPoint ? pointsOverlap(candidatePoint, selectedPoint) : false
    })

    const overlapsReservedPoint = reservedPoints.some((reservedPoint) => pointsOverlap(candidatePoint, reservedPoint))

    if (!overlapsSelected && !overlapsReservedPoint) {
      selected.push(candidate)
    }
  })

  return selected.sort((first, second) => first.index - second.index).map(({ cluster }) => cluster)
}

function propertyAverageLabels(item: PropertyMapItem): string[] {
  const saleAverageLabel =
    typeof item.recentYearAverageDealAmount === 'number'
      ? `매매 평균 ${formatKrw(item.recentYearAverageDealAmount)}`
      : '매매 정보 없음'
  const jeonseAverageLabel =
    typeof item.recentYearAverageJeonseDepositAmount === 'number'
      ? `전세 평균 ${formatKrw(item.recentYearAverageJeonseDepositAmount)}`
      : '전세 정보 없음'

  return [saleAverageLabel, jeonseAverageLabel]
}

function propertyMarkerContent(
  item: PropertyMapItem,
  selected: boolean,
  onClick: (propertyId: number) => void
): HTMLElement {
  const markerButton = document.createElement('button')
  markerButton.type = 'button'
  markerButton.className = selected ? 'map-property-marker is-selected' : 'map-property-marker'
  markerButton.setAttribute('aria-label', `${item.name}, ${propertyAverageLabels(item).join(', ')}`)

  const name = document.createElement('strong')
  name.textContent = item.name

  const priceList = document.createElement('span')
  priceList.className = 'map-property-marker-prices'

  propertyAverageLabels(item).forEach((label) => {
    const priceLabel = document.createElement('span')
    priceLabel.className = 'map-property-marker-price'
    priceLabel.textContent = label
    priceList.append(priceLabel)
  })

  markerButton.append(name, priceList)
  markerButton.addEventListener('click', (event) => {
    event.preventDefault()
    onClick(item.propertyId)
  })

  return markerButton
}

function markerSvg(fill: string, stroke: string): string {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="34" height="42" viewBox="0 0 34 42"><path d="M17 41s15-13.2 15-25A15 15 0 1 0 2 16c0 11.8 15 25 15 25Z" fill="${fill}" stroke="${stroke}" stroke-width="3"/><circle cx="17" cy="16" r="5" fill="#fff"/></svg>`
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`
}

function createMarkerImage(kakaoMaps: KakaoMapsApi, selected: boolean): unknown {
  if (!kakaoMaps.MarkerImage || !kakaoMaps.Size || !kakaoMaps.Point) {
    return undefined
  }

  const fill = selected ? '#b45309' : '#2563eb'
  const stroke = selected ? '#78350f' : '#1d4ed8'

  return new kakaoMaps.MarkerImage(markerSvg(fill, stroke), new kakaoMaps.Size(34, 42), {
    offset: new kakaoMaps.Point(17, 42)
  })
}

function createTransparentMarkerImage(kakaoMaps: KakaoMapsApi): unknown {
  if (!kakaoMaps.MarkerImage || !kakaoMaps.Size || !kakaoMaps.Point) {
    return undefined
  }

  const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="1" height="1"><rect width="1" height="1" fill="none"/></svg>'
  return new kakaoMaps.MarkerImage(
    `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`,
    new kakaoMaps.Size(1, 1),
    {
      offset: new kakaoMaps.Point(0, 0)
    }
  )
}

export function syncKakaoMarkers(options: {
  kakaoMaps: KakaoMapsApi
  map: KakaoMapLike
  previousMarkers: KakaoMarkerLike[]
  items: PropertyMapItem[]
  selectedPropertyId: number | null
  onClick: (propertyId: number) => void
}): KakaoMarkerLike[] {
  clearMarkers(options.previousMarkers)

  return options.items.map((item) => {
    const marker = new options.kakaoMaps.Marker({
      map: options.map,
      position: new options.kakaoMaps.LatLng(item.lat, item.lng),
      title: `property-${item.propertyId}`,
      image: createMarkerImage(options.kakaoMaps, options.selectedPropertyId === item.propertyId),
      clickable: true
    })

    options.kakaoMaps.event.addListener(marker, 'click', () => options.onClick(item.propertyId))

    return marker
  })
}

export function syncPropertyMarkerOverlays(options: {
  kakaoMaps: KakaoMapsApi
  map: KakaoMapLike
  previousOverlays: KakaoOverlayLike[]
  items: PropertyMapItem[]
  selectedPropertyId: number | null
  onClick: (propertyId: number) => void
}): KakaoOverlayLike[] {
  clearOverlayMarkers(options.previousOverlays)

  if (!options.kakaoMaps.CustomOverlay) {
    return []
  }

  const CustomOverlay = options.kakaoMaps.CustomOverlay

  return options.items.map((item) => {
    const content = propertyMarkerContent(item, options.selectedPropertyId === item.propertyId, options.onClick)

    return new CustomOverlay({
      map: options.map,
      position: new options.kakaoMaps.LatLng(item.lat, item.lng),
      content,
      yAnchor: 1
    })
  })
}

export function syncKakaoPropertyClusters(options: {
  kakaoMaps: KakaoMapsApi
  map: KakaoMapLike
  previousClusterer: KakaoMarkerClustererLike | null
  items: PropertyMapItem[]
  onClustered?: (clusters: KakaoClusterLike[]) => void
}): KakaoMarkerClustererLike | null {
  clearKakaoPropertyClusterer(options.previousClusterer)

  if (!options.kakaoMaps.MarkerClusterer) {
    return null
  }

  const markers = options.items.map((item) => {
    return new options.kakaoMaps.Marker({
      position: new options.kakaoMaps.LatLng(item.lat, item.lng),
      title: `property-cluster-${item.propertyId}`,
      image: createTransparentMarkerImage(options.kakaoMaps),
      clickable: false,
      opacity: 0
    })
  })

  const clusterer = new options.kakaoMaps.MarkerClusterer({
    map: options.map,
    averageCenter: true,
    minLevel: 4,
    gridSize: 80
  })

  options.kakaoMaps.event.addListener(clusterer, 'clustered', (clusters: unknown) => {
    if (!Array.isArray(clusters)) {
      return
    }

    const kakaoClusters = clusters as KakaoClusterLike[]

    kakaoClusters.forEach((kakaoCluster) => {
      kakaoCluster.getClusterMarker().setContent(propertyClusterBadgeContent(kakaoCluster.getMarkers().length))
    })
    options.onClustered?.(kakaoClusters)
  })

  clusterer.addMarkers(markers)

  return clusterer
}

export function syncAdministrativeClusterOverlays(options: {
  kakaoMaps: KakaoMapsApi
  map: KakaoMapLike
  previousOverlays: KakaoOverlayLike[]
  clusters: AdministrativeCluster[]
  reservedPoints?: MapScreenPoint[]
}): KakaoOverlayLike[] {
  clearOverlayMarkers(options.previousOverlays)

  if (!options.kakaoMaps.CustomOverlay) {
    return []
  }

  const CustomOverlay = options.kakaoMaps.CustomOverlay

  const visibleClusters = nonOverlappingAdministrativeClusters(
    options.kakaoMaps,
    options.map,
    options.clusters,
    options.reservedPoints ?? []
  )

  return visibleClusters.map(
    (cluster) =>
      new CustomOverlay({
        map: options.map,
        position: new options.kakaoMaps.LatLng(cluster.centerLat, cluster.centerLng),
        content: administrativeClusterContent(cluster),
        yAnchor: 0.5
      })
  )
}
