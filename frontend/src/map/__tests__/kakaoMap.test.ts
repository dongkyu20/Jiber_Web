import { describe, expect, it, vi } from 'vitest'

import {
  boundsFromKakao,
  formatAdministrativeClusterLabel,
  mapMarkerRenderMode,
  normalizePropertyMapItems,
  sumRecentTransactionCount,
  syncAdministrativeClusterOverlays,
  syncKakaoMarkers,
  syncPropertyMarkerOverlays,
  syncKakaoPropertyClusters
} from '@/map/kakaoMap'
import type { AdministrativeCluster, PropertyMapItem } from '@/api/types'

function property(propertyId: number, lat: number, lng: number): PropertyMapItem {
  return {
    propertyId,
    propertyType: 'APARTMENT',
    name: `테스트 단지 ${propertyId}`,
    address: '서울특별시 강남구 테스트로',
    lat,
    lng,
    latestTransaction: null,
    dealCount: 1,
    recentTransactionCount: 1,
    aiAvailable: true
  }
}

describe('kakaoMap utilities', () => {
  it('converts Kakao bounds to the property map search contract', () => {
    const bounds = {
      getSouthWest: () => ({
        getLat: () => 37.48,
        getLng: () => 127.01
      }),
      getNorthEast: () => ({
        getLat: () => 37.52,
        getLng: () => 127.06
      })
    }

    expect(boundsFromKakao(bounds, 5)).toEqual({
      swLat: 37.48,
      swLng: 127.01,
      neLat: 37.52,
      neLng: 127.06,
      zoomLevel: 5
    })
  })

  it('normalizes transaction-like map rows into one item per property with a recent-year sale average price', () => {
    const normalized = normalizePropertyMapItems(
      [
        {
          ...property(1001, 37.5, 127.03),
          latestTransaction: {
            transactionType: 'SALE',
            dealAmount: 1000000000,
            dealDate: '2026-06-01'
          }
        },
        {
          ...property(1001, 37.5001, 127.0301),
          latestTransaction: {
            transactionType: 'JEONSE',
            dealAmount: 1200000000,
            dealDate: '2025-07-01'
          }
        },
        {
          ...property(1001, 37.5002, 127.0302),
          latestTransaction: {
            transactionType: 'SALE',
            dealAmount: 900000000,
            dealDate: '2025-05-01'
          }
        },
        property(1002, 37.51, 127.04)
      ],
      new Date('2026-06-23T00:00:00Z')
    )

    expect(normalized).toHaveLength(2)
    expect(normalized[0].propertyId).toBe(1001)
    expect(normalized[0].latestTransaction?.dealDate).toBe('2026-06-01')
    expect(normalized[0].recentTransactionCount).toBe(2)
    expect(normalized[0].recentYearAverageDealAmount).toBe(1000000000)
    expect(normalized[0].recentYearAverageJeonseDepositAmount).toBe(1200000000)
    expect(normalized[1].propertyId).toBe(1002)
  })

  it('clears old property overlays, renders labeled average-price markers, and wires clicks', () => {
    const oldOverlay = { setMap: vi.fn() }
    const createdOverlays: Array<{ content: HTMLElement; setMap: ReturnType<typeof vi.fn> }> = []
    const map = { id: 'map' }
    const kakaoMaps = {
      LatLng: vi.fn((lat: number, lng: number) => ({ lat, lng })),
      CustomOverlay: vi.fn((options: { content: HTMLElement }) => {
        const overlay = { content: options.content, setMap: vi.fn() }
        createdOverlays.push(overlay)
        return overlay
      }),
      event: {
        addListener: vi.fn()
      }
    }
    const onClick = vi.fn()

    const overlays = syncPropertyMarkerOverlays({
      kakaoMaps,
      map,
      previousOverlays: [oldOverlay],
      items: [
        {
          ...property(1001, 37.5, 127.03),
          name: '경희궁롯데캐슬',
          recentYearAverageDealAmount: 1100000000,
          recentYearAverageJeonseDepositAmount: 780000000
        }
      ],
      selectedPropertyId: 1001,
      onClick
    })

    expect(oldOverlay.setMap).toHaveBeenCalledWith(null)
    expect(overlays).toHaveLength(1)
    expect(kakaoMaps.CustomOverlay).toHaveBeenCalledWith({
      map,
      position: { lat: 37.5, lng: 127.03 },
      content: createdOverlays[0].content,
      yAnchor: 1,
      zIndex: 30
    })
    expect(createdOverlays[0].content.className).toContain('map-property-marker')
    expect(createdOverlays[0].content.className).toContain('is-selected')
    expect(createdOverlays[0].content.textContent).toContain('경희궁롯데캐슬')
    expect(createdOverlays[0].content.textContent).toContain('매매 평균 11억 원')
    expect(createdOverlays[0].content.textContent).toContain('전세 평균 7.8억 원')

    createdOverlays[0].content.dispatchEvent(new MouseEvent('click'))

    expect(onClick).toHaveBeenCalledWith(1001)
  })

  it('minimizes overlapping unselected property overlays while keeping the selected marker detailed', () => {
    const createdOverlays: Array<{
      content: HTMLElement
      setMap: ReturnType<typeof vi.fn>
      setZIndex: ReturnType<typeof vi.fn>
      zIndex?: number
    }> = []
    const map = {
      getProjection: () => ({
        containerPointFromCoords: (latLng: { lat: number; lng: number }) => ({
          x: (latLng.lng - 127) * 10000,
          y: (37.6 - latLng.lat) * 10000
        })
      })
    }
    const kakaoMaps = {
      LatLng: vi.fn((lat: number, lng: number) => ({ lat, lng })),
      CustomOverlay: vi.fn((options: { content: HTMLElement; zIndex?: number }) => {
        const overlay = { content: options.content, setMap: vi.fn(), setZIndex: vi.fn(), zIndex: options.zIndex }
        createdOverlays.push(overlay)
        return overlay
      })
    }

    syncPropertyMarkerOverlays({
      kakaoMaps,
      map,
      previousOverlays: [],
      items: [
        property(1001, 37.5, 127.03),
        property(1002, 37.5001, 127.0301),
        property(1003, 37.5, 127.06)
      ],
      selectedPropertyId: 1002,
      onClick: vi.fn()
    })

    expect(createdOverlays).toHaveLength(3)
    expect(createdOverlays[0].zIndex).toBe(10)
    expect(createdOverlays[0].content.className).toContain('is-minimized')
    expect(createdOverlays[0].content.querySelector('.map-property-marker-dot')).not.toBeNull()
    expect(createdOverlays[0].content.querySelector('.map-property-marker-detail')?.textContent).toContain(
      '테스트 단지 1001'
    )
    createdOverlays[0].content.dispatchEvent(new MouseEvent('mouseenter'))
    expect(createdOverlays[0].setZIndex).toHaveBeenCalledWith(40)
    createdOverlays[0].content.dispatchEvent(new MouseEvent('mouseleave'))
    expect(createdOverlays[0].setZIndex).toHaveBeenCalledWith(10)
    expect(createdOverlays[1].zIndex).toBe(30)
    expect(createdOverlays[1].content.className).toContain('is-selected')
    expect(createdOverlays[1].content.className).not.toContain('is-minimized')
    expect(createdOverlays[2].zIndex).toBe(20)
    expect(createdOverlays[2].content.className).not.toContain('is-minimized')
  })

  it('keeps the upper unselected marker detailed when overlapping property markers compete', () => {
    const createdOverlays: Array<{
      content: HTMLElement
      setMap: ReturnType<typeof vi.fn>
      setZIndex: ReturnType<typeof vi.fn>
      zIndex?: number
    }> = []
    const map = {
      getProjection: () => ({
        containerPointFromCoords: (latLng: { lat: number; lng: number }) => ({
          x: (latLng.lng - 127) * 10000,
          y: (37.6 - latLng.lat) * 10000
        })
      })
    }
    const kakaoMaps = {
      LatLng: vi.fn((lat: number, lng: number) => ({ lat, lng })),
      CustomOverlay: vi.fn((options: { content: HTMLElement; zIndex?: number }) => {
        const overlay = { content: options.content, setMap: vi.fn(), setZIndex: vi.fn(), zIndex: options.zIndex }
        createdOverlays.push(overlay)
        return overlay
      })
    }

    syncPropertyMarkerOverlays({
      kakaoMaps,
      map,
      previousOverlays: [],
      items: [
        {
          ...property(1001, 37.5, 127.03),
          recentTransactionCount: 50,
          dealCount: 50
        },
        {
          ...property(1002, 37.5001, 127.0301),
          recentTransactionCount: 1,
          dealCount: 1
        }
      ],
      selectedPropertyId: null,
      onClick: vi.fn()
    })

    expect(createdOverlays).toHaveLength(2)
    expect(createdOverlays[0].content.textContent).toContain('테스트 단지 1001')
    expect(createdOverlays[0].content.className).toContain('is-minimized')
    expect(createdOverlays[1].content.textContent).toContain('테스트 단지 1002')
    expect(createdOverlays[1].content.className).not.toContain('is-minimized')
  })

  it('clears old markers, renders new markers, and wires marker clicks to property selection', () => {
    const oldMarker = { setMap: vi.fn() }
    const clickHandlers: Array<() => void> = []
    const createdMarkers: Array<{ setMap: ReturnType<typeof vi.fn>; propertyId: number }> = []
    const map = { id: 'map' }
    const kakaoMaps = {
      LatLng: vi.fn((lat: number, lng: number) => ({ lat, lng })),
      Size: vi.fn(),
      Point: vi.fn(),
      MarkerImage: vi.fn(),
      Marker: vi.fn((options: { title: string }) => {
        const marker = {
          propertyId: Number(options.title.replace('property-', '')),
          setMap: vi.fn()
        }
        createdMarkers.push(marker)
        return marker
      }),
      event: {
        addListener: vi.fn((_target: unknown, eventName: string, handler: () => void) => {
          if (eventName === 'click') {
            clickHandlers.push(handler)
          }
        })
      }
    }
    const onClick = vi.fn()

    const markers = syncKakaoMarkers({
      kakaoMaps,
      map,
      previousMarkers: [oldMarker],
      items: [property(1001, 37.5, 127.03), property(1002, 37.51, 127.04)],
      selectedPropertyId: 1002,
      onClick
    })

    expect(oldMarker.setMap).toHaveBeenCalledWith(null)
    expect(markers).toHaveLength(2)
    expect(kakaoMaps.Marker).toHaveBeenCalledTimes(2)
    expect(kakaoMaps.event.addListener).toHaveBeenCalledTimes(2)

    clickHandlers[1]()
    expect(onClick).toHaveBeenCalledWith(1002)
    expect(createdMarkers[0].setMap).not.toHaveBeenCalledWith(null)
  })

  it('selects individual markers at zoom level 3', () => {
    expect(mapMarkerRenderMode(3)).toEqual({
      showIndividualMarkers: true,
      showPropertyClusterer: false,
      showAdministrativeClusters: false
    })
  })

  it('switches to property cluster markers at zoom level 4', () => {
    expect(mapMarkerRenderMode(4)).toEqual({
      showIndividualMarkers: false,
      showPropertyClusterer: true,
      showAdministrativeClusters: false
    })
  })

  it('keeps property cluster markers alongside administrative clusters only at zoom level 5', () => {
    expect(mapMarkerRenderMode(5)).toEqual({
      showIndividualMarkers: false,
      showPropertyClusterer: true,
      showAdministrativeClusters: true
    })
  })

  it('shows only administrative overlays from zoom level 6', () => {
    expect(mapMarkerRenderMode(6)).toEqual({
      showIndividualMarkers: false,
      showPropertyClusterer: false,
      showAdministrativeClusters: true
    })
    expect(mapMarkerRenderMode(7)).toEqual({
      showIndividualMarkers: false,
      showPropertyClusterer: false,
      showAdministrativeClusters: true
    })
  })

  it('formats administrative cluster labels with average deal amount', () => {
    const cluster: AdministrativeCluster = {
      clusterId: 'legal-dong-1168010100',
      level: 'LEGAL_DONG',
      sido: '서울특별시',
      sigungu: '강남구',
      legalDong: '역삼동',
      label: '역삼동',
      centerLat: 37.5,
      centerLng: 127.03,
      propertyCount: 12,
      transactionCount: 1234,
      averageDealAmount: 1500000000
    }

    expect(formatAdministrativeClusterLabel(cluster)).toBe('역삼동\n평균 15억 원\n거래 1,234건')
  })

  it('formats administrative cluster labels without average deal amount', () => {
    const cluster: AdministrativeCluster = {
      clusterId: 'sigungu-11680',
      level: 'SIGUNGU',
      sido: '서울특별시',
      sigungu: '강남구',
      legalDong: null,
      label: '강남구',
      centerLat: 37.5,
      centerLng: 127.03,
      propertyCount: 120,
      transactionCount: 0,
      averageDealAmount: null
    }

    expect(formatAdministrativeClusterLabel(cluster)).toBe('강남구\n평균 정보 없음\n거래 0건')
  })

  it('sums recent transaction counts defensively', () => {
    expect(
      sumRecentTransactionCount([
        property(1001, 37.5, 127.03),
        { ...property(1002, 37.51, 127.04), recentTransactionCount: 3 },
        { ...property(1003, 37.52, 127.05), recentTransactionCount: undefined as unknown as number }
      ])
    ).toBe(4)
  })

  it('creates a MarkerClusterer and updates cluster marker content with property counts', () => {
    const oldClusterer = { clear: vi.fn() }
    const clusteredHandlers: Array<(clusters: unknown[]) => void> = []
    const clusterClickHandlers: Array<(cluster: unknown) => void> = []
    const onClustered = vi.fn()
    const onClusterClick = vi.fn()
    const clusterMarker = { setContent: vi.fn() }
    const createdMarkers: Array<{ setMap: ReturnType<typeof vi.fn> }> = []
    const markerOptions: Array<Record<string, unknown>> = []
    const addMarkers = vi.fn()
    const map = { id: 'map' }
    const kakaoMaps = {
      LatLng: vi.fn((lat: number, lng: number) => ({ lat, lng })),
      Size: vi.fn((width: number, height: number) => ({ width, height })),
      Point: vi.fn((x: number, y: number) => ({ x, y })),
      MarkerImage: vi.fn((src: string) => ({ src })),
      Marker: vi.fn((options: Record<string, unknown>) => {
        markerOptions.push(options)
        const marker = { setMap: vi.fn() }
        createdMarkers.push(marker)
        return marker
      }),
      MarkerClusterer: vi.fn(() => ({
        addMarkers,
        clear: vi.fn()
      })),
      event: {
        addListener: vi.fn((_target: unknown, eventName: string, handler: (clusters: unknown[]) => void) => {
          if (eventName === 'clustered') {
            clusteredHandlers.push(handler)
          }
          if (eventName === 'clusterclick') {
            clusterClickHandlers.push(handler)
          }
        })
      }
    }

    const clusterer = syncKakaoPropertyClusters({
      kakaoMaps,
      map,
      previousClusterer: oldClusterer,
      items: [
        { ...property(1001, 37.5, 127.03), recentTransactionCount: 3 },
        { ...property(1002, 37.51, 127.04), recentTransactionCount: 5 }
      ],
      onClustered,
      onClusterClick
    })

    expect(oldClusterer.clear).toHaveBeenCalled()
    expect(clusterer).not.toBeNull()
    expect(kakaoMaps.MarkerClusterer).toHaveBeenCalledWith({
      map,
      averageCenter: true,
      minLevel: 4,
      gridSize: expect.any(Number),
      disableClickZoom: true
    })
    expect(markerOptions).toHaveLength(2)
    expect(markerOptions[0]).not.toHaveProperty('map')
    expect(markerOptions[1]).not.toHaveProperty('map')
    expect(markerOptions[0]).toMatchObject({
      clickable: false,
      opacity: 0,
      image: expect.objectContaining({ src: expect.stringContaining('data:image/svg+xml') })
    })
    expect(markerOptions[1]).toMatchObject({
      clickable: false,
      opacity: 0,
      image: expect.objectContaining({ src: expect.stringContaining('data:image/svg+xml') })
    })
    expect(addMarkers).toHaveBeenCalledWith(createdMarkers)
    expect(kakaoMaps.event.addListener.mock.invocationCallOrder[0]).toBeLessThan(
      addMarkers.mock.invocationCallOrder[0]
    )

    clusteredHandlers[0]([
      {
        getMarkers: () => createdMarkers,
        getClusterMarker: () => clusterMarker
      }
    ])

    expect(onClustered).toHaveBeenCalledWith([
      {
        getMarkers: expect.any(Function),
        getClusterMarker: expect.any(Function)
      }
    ])

    const clusterContent = clusterMarker.setContent.mock.calls[0][0]
    expect(clusterContent).toBeInstanceOf(HTMLElement)
    expect(clusterContent.className).toContain('map-property-cluster')
    expect(clusterContent.querySelector('.map-property-cluster-count')?.textContent).toBe('2')
    expect(clusterContent.textContent).toContain('부동산 수')
    expect(clusterContent.textContent).not.toContain('곳')
    clusterContent.dispatchEvent(new MouseEvent('click'))
    expect(onClusterClick).toHaveBeenCalledWith(
      expect.objectContaining({
        getMarkers: expect.any(Function),
        getClusterMarker: expect.any(Function)
      })
    )
    const clickedCluster = {
      getMarkers: () => createdMarkers,
      getClusterMarker: () => clusterMarker
    }
    clusterClickHandlers[0](clickedCluster)
    expect(onClusterClick).toHaveBeenCalledWith(clickedCluster)
  })

  it('clears old administrative overlays and creates Korean cluster content', () => {
    const oldOverlay = { setMap: vi.fn() }
    const createdOverlays: Array<{ content: HTMLElement; setMap: ReturnType<typeof vi.fn> }> = []
    const onClick = vi.fn()
    const map = { id: 'map' }
    const kakaoMaps = {
      LatLng: vi.fn((lat: number, lng: number) => ({ lat, lng })),
      Marker: vi.fn(),
      CustomOverlay: vi.fn((options: { content: HTMLElement }) => {
        const overlay = { content: options.content, setMap: vi.fn() }
        createdOverlays.push(overlay)
        return overlay
      }),
      event: {
        addListener: vi.fn()
      }
    }
    const cluster: AdministrativeCluster = {
      clusterId: 'legal-dong-1168010100"><img src=x onerror=alert(1)>',
      level: 'LEGAL_DONG',
      sido: '서울특별시',
      sigungu: '강남구',
      legalDong: '역삼동',
      label: '<img src=x onerror=alert(1)>',
      centerLat: 37.5,
      centerLng: 127.03,
      propertyCount: 12,
      transactionCount: 1234,
      averageDealAmount: 1500000000
    }

    const overlays = syncAdministrativeClusterOverlays({
      kakaoMaps,
      map,
      previousOverlays: [oldOverlay],
      clusters: [cluster],
      onClick
    })

    expect(oldOverlay.setMap).toHaveBeenCalledWith(null)
    expect(overlays).toHaveLength(1)
    expect(kakaoMaps.CustomOverlay).toHaveBeenCalledWith({
      map,
      position: { lat: 37.5, lng: 127.03 },
      content: createdOverlays[0].content,
      yAnchor: 0.5
    })
    expect(createdOverlays[0].content.className).toContain('map-admin-cluster')
    expect(createdOverlays[0].content.dataset.clusterId).toBe('legal-dong-1168010100"><img src=x onerror=alert(1)>')
    expect(createdOverlays[0].content.textContent).toContain('<img src=x onerror=alert(1)>')
    expect(createdOverlays[0].content.querySelector('img')).toBeNull()
    expect(createdOverlays[0].content.textContent).toContain('평균 15억 원')
    expect(createdOverlays[0].content.textContent).toContain('거래 1,234건')

    createdOverlays[0].content.dispatchEvent(new MouseEvent('click'))
    expect(onClick).toHaveBeenCalledWith(cluster)
  })

  it('skips lower-priority administrative overlays that would overlap on the map', () => {
    const createdOverlays: Array<{ content: HTMLElement; setMap: ReturnType<typeof vi.fn> }> = []
    const map = {
      id: 'map',
      getProjection: () => ({
        containerPointFromCoords: (latLng: { lat: number; lng: number }) => ({
          x: (latLng.lng - 127) * 10000,
          y: (37.6 - latLng.lat) * 10000
        })
      })
    }
    const kakaoMaps = {
      LatLng: vi.fn((lat: number, lng: number) => ({ lat, lng })),
      Marker: vi.fn(),
      CustomOverlay: vi.fn((options: { content: HTMLElement }) => {
        const overlay = { content: options.content, setMap: vi.fn() }
        createdOverlays.push(overlay)
        return overlay
      }),
      event: {
        addListener: vi.fn()
      }
    }
    const lowPriorityOverlap: AdministrativeCluster = {
      clusterId: 'legal-dong-low',
      level: 'LEGAL_DONG',
      sido: '서울특별시',
      sigungu: '강남구',
      legalDong: '낮은동',
      label: '낮은동',
      centerLat: 37.5,
      centerLng: 127.03,
      propertyCount: 3,
      transactionCount: 3,
      averageDealAmount: 1000000000
    }
    const highPriorityOverlap: AdministrativeCluster = {
      ...lowPriorityOverlap,
      clusterId: 'legal-dong-high',
      legalDong: '높은동',
      label: '높은동',
      centerLat: 37.5001,
      centerLng: 127.0301,
      propertyCount: 20,
      transactionCount: 30
    }
    const farCluster: AdministrativeCluster = {
      ...lowPriorityOverlap,
      clusterId: 'legal-dong-far',
      legalDong: '먼동',
      label: '먼동',
      centerLat: 37.52,
      centerLng: 127.06,
      propertyCount: 2,
      transactionCount: 2
    }

    const overlays = syncAdministrativeClusterOverlays({
      kakaoMaps,
      map,
      previousOverlays: [],
      clusters: [lowPriorityOverlap, highPriorityOverlap, farCluster]
    })

    expect(overlays).toHaveLength(2)
    expect(createdOverlays.map((overlay) => overlay.content.textContent)).toEqual(
      expect.arrayContaining([expect.stringContaining('높은동'), expect.stringContaining('먼동')])
    )
    expect(createdOverlays.map((overlay) => overlay.content.textContent)).not.toEqual(
      expect.arrayContaining([expect.stringContaining('낮은동')])
    )
  })

  it('skips administrative overlays that would overlap Kakao property cluster markers', () => {
    const createdOverlays: Array<{ content: HTMLElement; setMap: ReturnType<typeof vi.fn> }> = []
    const map = {
      id: 'map',
      getProjection: () => ({
        containerPointFromCoords: (latLng: { lat: number; lng: number }) => ({
          x: (latLng.lng - 127) * 10000,
          y: (37.6 - latLng.lat) * 10000
        })
      })
    }
    const kakaoMaps = {
      LatLng: vi.fn((lat: number, lng: number) => ({ lat, lng })),
      Marker: vi.fn(),
      CustomOverlay: vi.fn((options: { content: HTMLElement }) => {
        const overlay = { content: options.content, setMap: vi.fn() }
        createdOverlays.push(overlay)
        return overlay
      }),
      event: {
        addListener: vi.fn()
      }
    }
    const overlappingCluster: AdministrativeCluster = {
      clusterId: 'legal-dong-overlap',
      level: 'LEGAL_DONG',
      sido: '서울특별시',
      sigungu: '강남구',
      legalDong: '역삼동',
      label: '역삼동',
      centerLat: 37.5,
      centerLng: 127.03,
      propertyCount: 20,
      transactionCount: 30,
      averageDealAmount: 1000000000
    }
    const farCluster: AdministrativeCluster = {
      ...overlappingCluster,
      clusterId: 'legal-dong-far',
      legalDong: '논현동',
      label: '논현동',
      centerLat: 37.52,
      centerLng: 127.06
    }

    const overlays = syncAdministrativeClusterOverlays({
      kakaoMaps,
      map,
      previousOverlays: [],
      clusters: [overlappingCluster, farCluster],
      reservedPoints: [{ x: 300, y: 1000 }]
    })

    expect(overlays).toHaveLength(1)
    expect(createdOverlays[0].content.textContent).toContain('논현동')
    expect(createdOverlays[0].content.textContent).not.toContain('역삼동')
  })
})
