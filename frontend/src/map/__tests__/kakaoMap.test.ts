import { describe, expect, it, vi } from 'vitest'

import {
  boundsFromKakao,
  formatAdministrativeClusterLabel,
  mapMarkerRenderMode,
  sumRecentTransactionCount,
  syncKakaoMarkers
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
      showTransactionClusterer: false,
      showAdministrativeClusters: false
    })
  })

  it('selects transaction clusterer at zoom level 4', () => {
    expect(mapMarkerRenderMode(4)).toEqual({
      showIndividualMarkers: false,
      showTransactionClusterer: true,
      showAdministrativeClusters: false
    })
  })

  it('selects administrative clusters at zoom level 5 and above', () => {
    expect(mapMarkerRenderMode(5)).toEqual({
      showIndividualMarkers: false,
      showTransactionClusterer: true,
      showAdministrativeClusters: true
    })
    expect(mapMarkerRenderMode(7)).toEqual({
      showIndividualMarkers: false,
      showTransactionClusterer: true,
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
})
