import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { AdministrativeCluster, PropertyMapItem } from '@/api/types'
import KakaoMapPanel from '@/components/KakaoMapPanel.vue'

const kakaoMock = vi.hoisted(() => {
  const state = {
    hasKey: false,
    idleHandler: null as null | (() => void),
    markerClickHandlers: [] as Array<() => void>,
    mapInstance: {
      getBounds: vi.fn(() => ({
        getSouthWest: () => ({
          getLat: () => 37.48,
          getLng: () => 127.01
        }),
        getNorthEast: () => ({
          getLat: () => 37.52,
          getLng: () => 127.06
        })
      })),
      getLevel: vi.fn(() => 5),
      panTo: vi.fn(),
      setLevel: vi.fn(),
      setCenter: vi.fn()
    },
    markers: [] as Array<{ setMap: ReturnType<typeof vi.fn>; title: string; recentTransactionCount?: number }>,
    clusterers: [] as Array<{
      addMarkers: ReturnType<typeof vi.fn>
      clear: ReturnType<typeof vi.fn>
      setMap: ReturnType<typeof vi.fn>
    }>,
    overlays: [] as Array<{ setMap: ReturnType<typeof vi.fn>; content: string }>
  }

  const maps = {
    LatLng: vi.fn((lat: number, lng: number) => ({ lat, lng })),
    Size: vi.fn((width: number, height: number) => ({ width, height })),
    Point: vi.fn((x: number, y: number) => ({ x, y })),
    MarkerImage: vi.fn((src: string) => ({ src })),
    Map: vi.fn(() => state.mapInstance),
    Marker: vi.fn((options: { title: string }) => {
      const marker = {
        setMap: vi.fn(),
        title: options.title,
        recentTransactionCount: undefined as number | undefined
      }
      state.markers.push(marker)
      return marker
    }),
    MarkerClusterer: vi.fn(() => {
      const clusterer = {
        addMarkers: vi.fn(),
        clear: vi.fn(),
        setMap: vi.fn()
      }
      state.clusterers.push(clusterer)
      return clusterer
    }),
    CustomOverlay: vi.fn((options: { content: string | HTMLElement }) => {
      const overlay = {
        setMap: vi.fn(),
        content: options.content
      }
      state.overlays.push(overlay)
      return overlay
    }),
    event: {
      addListener: vi.fn((target: unknown, eventName: string, handler: () => void) => {
        if (target === state.mapInstance && eventName === 'idle') {
          state.idleHandler = handler
        }

        if (eventName === 'click') {
          state.markerClickHandlers.push(handler)
        }
      })
    },
    load: vi.fn((callback: () => void) => callback())
  }

  return {
    state,
    maps,
    hasKakaoMapKey: vi.fn(() => state.hasKey),
    loadKakaoMaps: vi.fn(() => Promise.resolve()),
    getKakaoMaps: vi.fn(() => maps),
    getKakaoMapFallbackMessage: vi.fn(
      () =>
        '카카오 지도 API 키가 아직 설정되지 않았습니다. root .env 또는 frontend 실행 환경에 VITE_KAKAO_MAP_APP_KEY를 설정하면 실제 지도를 불러올 수 있습니다.'
    )
  }
})

vi.mock('@/map/kakaoLoader', () => ({
  hasKakaoMapKey: kakaoMock.hasKakaoMapKey,
  loadKakaoMaps: kakaoMock.loadKakaoMaps,
  getKakaoMaps: kakaoMock.getKakaoMaps,
  getKakaoMapFallbackMessage: kakaoMock.getKakaoMapFallbackMessage
}))

function property(propertyId: number): PropertyMapItem {
  return {
    propertyId,
    propertyType: 'APARTMENT',
    name: `테스트 단지 ${propertyId}`,
    address: '서울특별시 강남구 테스트로',
    lat: 37.5 + propertyId / 100000,
    lng: 127.03 + propertyId / 100000,
    latestTransaction: null,
    dealCount: 1,
    recentTransactionCount: propertyId,
    recentYearAverageDealAmount: 1100000000,
    aiAvailable: true
  }
}

function administrativeCluster(
  clusterId: string,
  level: AdministrativeCluster['level'],
  label: string
): AdministrativeCluster {
  return {
    clusterId,
    level,
    sido: '서울특별시',
    sigungu: '강남구',
    legalDong: level === 'LEGAL_DONG' ? label : null,
    label,
    centerLat: 37.5,
    centerLng: 127.03,
    propertyCount: 12,
    transactionCount: 34,
    averageDealAmount: 1500000000
  }
}

describe('KakaoMapPanel', () => {
  beforeEach(() => {
    kakaoMock.state.hasKey = false
    kakaoMock.state.idleHandler = null
    kakaoMock.state.markerClickHandlers = []
    kakaoMock.state.markers = []
    kakaoMock.state.mapInstance.getBounds.mockClear()
    kakaoMock.state.mapInstance.getLevel.mockClear()
    kakaoMock.state.mapInstance.panTo.mockClear()
    kakaoMock.state.mapInstance.setLevel.mockClear()
    kakaoMock.state.mapInstance.setCenter.mockClear()
    kakaoMock.state.mapInstance.getLevel.mockReturnValue(5)
    kakaoMock.state.clusterers = []
    kakaoMock.state.overlays = []
    kakaoMock.maps.LatLng.mockClear()
    kakaoMock.maps.Size.mockClear()
    kakaoMock.maps.Point.mockClear()
    kakaoMock.maps.MarkerImage.mockClear()
    kakaoMock.maps.Map.mockClear()
    kakaoMock.maps.Marker.mockClear()
    kakaoMock.maps.MarkerClusterer.mockClear()
    kakaoMock.maps.CustomOverlay.mockClear()
    kakaoMock.maps.event.addListener.mockClear()
    kakaoMock.maps.load.mockClear()
    kakaoMock.hasKakaoMapKey.mockClear()
    kakaoMock.loadKakaoMaps.mockClear()
    kakaoMock.getKakaoMaps.mockClear()
    kakaoMock.getKakaoMapFallbackMessage.mockClear()
  })

  it('keeps the Korean fallback visible when the Kakao Maps key is missing', () => {
    const wrapper = mount(KakaoMapPanel, {
      props: {
        items: [],
        selectedPropertyId: null
      }
    })

    expect(wrapper.text()).toContain('지도 준비 중')
    expect(wrapper.text()).toContain('카카오 지도 API 키가 아직 설정되지 않았습니다.')
    expect(wrapper.text()).toContain('VITE_KAKAO_MAP_APP_KEY')
  })

  it('creates a Kakao map, emits lifecycle bounds, syncs labeled property overlays, and forwards marker clicks when a key exists', async () => {
    kakaoMock.state.hasKey = true
    kakaoMock.state.mapInstance.getLevel.mockReturnValue(3)
    vi.useFakeTimers()

    try {
      const wrapper = mount(KakaoMapPanel, {
        props: {
          items: [property(1001)],
          selectedPropertyId: null
        }
      })

      await flushPromises()

      expect(kakaoMock.loadKakaoMaps).toHaveBeenCalledTimes(1)
      expect(kakaoMock.getKakaoMaps).toHaveBeenCalledTimes(1)
      expect(kakaoMock.maps.Map).toHaveBeenCalledTimes(1)
      expect(kakaoMock.maps.event.addListener).toHaveBeenCalledWith(
        kakaoMock.state.mapInstance,
        'idle',
        expect.any(Function)
      )
      expect(wrapper.emitted('ready')?.[0]).toEqual([
        {
          swLat: 37.48,
          swLng: 127.01,
          neLat: 37.52,
          neLng: 127.06,
          zoomLevel: 3
        }
      ])

      expect(kakaoMock.maps.Marker).not.toHaveBeenCalled()
      expect(kakaoMock.maps.CustomOverlay).toHaveBeenCalledTimes(1)
      expect(kakaoMock.state.overlays[0].content).toBeInstanceOf(HTMLElement)
      expect(kakaoMock.state.overlays[0].content.textContent).toContain('테스트 단지 1001')
      expect(kakaoMock.state.overlays[0].content.textContent).toContain('최근 1년 평균 11억 원')

      kakaoMock.state.idleHandler?.()
      vi.advanceTimersByTime(180)

      expect(kakaoMock.maps.Marker).not.toHaveBeenCalled()
      expect(kakaoMock.maps.CustomOverlay).toHaveBeenCalledTimes(1)

      expect(wrapper.emitted('boundsChanged')?.[0]).toEqual([
        {
          swLat: 37.48,
          swLng: 127.01,
          neLat: 37.52,
          neLng: 127.06,
          zoomLevel: 3
        }
      ])

      kakaoMock.state.overlays[0].content.dispatchEvent(new MouseEvent('click'))
      expect(wrapper.emitted('propertySelected')?.[0]).toEqual([1001])

      const firstOverlay = kakaoMock.state.overlays[0]
      await wrapper.setProps({
        items: [property(1002)],
        selectedPropertyId: 1002
      })
      await flushPromises()

      expect(firstOverlay.setMap).toHaveBeenCalledWith(null)
      expect(kakaoMock.maps.Marker).not.toHaveBeenCalled()
      expect(kakaoMock.maps.CustomOverlay).toHaveBeenCalledTimes(2)
      expect(kakaoMock.state.overlays[1].content.textContent).toContain('테스트 단지 1002')

      await wrapper.setProps({
        focusTarget: {
          lat: 37.5738636,
          lng: 126.9594466
        },
        focusZoomLevel: 6
      })
      await flushPromises()

      expect(kakaoMock.state.mapInstance.setLevel).toHaveBeenCalledWith(6)
      expect(kakaoMock.state.mapInstance.panTo).toHaveBeenCalledWith({
        lat: 37.5738636,
        lng: 126.9594466
      })
    } finally {
      vi.useRealTimers()
    }
  })

  it('renders property clusterer plus legal-dong administrative overlays at level 5', async () => {
    kakaoMock.state.hasKey = true
    kakaoMock.state.mapInstance.getLevel.mockReturnValue(5)

    const wrapper = mount(KakaoMapPanel, {
      props: {
        items: [property(1001), property(1002)],
        selectedPropertyId: null,
        administrativeClusters: [administrativeCluster('legal-dong-1168010100', 'LEGAL_DONG', '역삼동')]
      }
    })

    await flushPromises()

    expect(kakaoMock.maps.MarkerClusterer).toHaveBeenCalledTimes(1)
    expect(kakaoMock.state.clusterers[0].addMarkers).toHaveBeenCalledWith(
      expect.arrayContaining([
        expect.objectContaining({ title: 'property-cluster-1001' }),
        expect.objectContaining({ title: 'property-cluster-1002' })
      ])
    )
    expect(kakaoMock.maps.CustomOverlay).toHaveBeenCalledTimes(1)
    expect(kakaoMock.state.overlays[0].content).toContain('map-admin-cluster')
    expect(kakaoMock.state.overlays[0].content).toContain('역삼동')

    const firstClusterer = kakaoMock.state.clusterers[0]
    const firstAdminOverlay = kakaoMock.state.overlays[0]

    await wrapper.setProps({
      items: [property(1003)],
      administrativeClusters: [administrativeCluster('legal-dong-1168010800', 'LEGAL_DONG', '논현동')]
    })
    await flushPromises()

    expect(firstClusterer.clear).toHaveBeenCalled()
    expect(firstClusterer.setMap).toHaveBeenCalledWith(null)
    expect(firstAdminOverlay.setMap).toHaveBeenCalledWith(null)
    expect(kakaoMock.maps.MarkerClusterer).toHaveBeenCalledTimes(2)
    expect(kakaoMock.maps.CustomOverlay).toHaveBeenCalledTimes(2)
  })

  it('does not rebuild cluster layers on idle while the map render mode is unchanged', async () => {
    kakaoMock.state.hasKey = true
    kakaoMock.state.mapInstance.getLevel.mockReturnValue(5)
    vi.useFakeTimers()

    try {
      const wrapper = mount(KakaoMapPanel, {
        props: {
          items: [property(1001)],
          selectedPropertyId: null,
          administrativeClusters: [administrativeCluster('legal-dong-idle', 'LEGAL_DONG', 'Yeoksam')]
        }
      })

      await flushPromises()

      expect(kakaoMock.maps.Marker).toHaveBeenCalledTimes(1)
      expect(kakaoMock.maps.MarkerClusterer).toHaveBeenCalledTimes(1)
      expect(kakaoMock.maps.CustomOverlay).toHaveBeenCalledTimes(1)

      kakaoMock.state.idleHandler?.()
      vi.advanceTimersByTime(180)

      expect(kakaoMock.maps.Marker).toHaveBeenCalledTimes(1)
      expect(kakaoMock.maps.MarkerClusterer).toHaveBeenCalledTimes(1)
      expect(kakaoMock.maps.CustomOverlay).toHaveBeenCalledTimes(1)
      expect(wrapper.emitted('boundsChanged')?.[0]).toEqual([
        {
          swLat: 37.48,
          swLng: 127.01,
          neLat: 37.52,
          neLng: 127.06,
          zoomLevel: 5
        }
      ])
    } finally {
      vi.useRealTimers()
    }
  })

  it('renders only sigungu administrative overlays at level 7 and clears them on unmount', async () => {
    kakaoMock.state.hasKey = true
    kakaoMock.state.mapInstance.getLevel.mockReturnValue(7)

    const wrapper = mount(KakaoMapPanel, {
      props: {
        items: [property(2001)],
        selectedPropertyId: null,
        administrativeClusters: [administrativeCluster('sigungu-11680', 'SIGUNGU', '강남구')]
      }
    })

    await flushPromises()

    expect(kakaoMock.maps.MarkerClusterer).not.toHaveBeenCalled()
    expect(kakaoMock.maps.CustomOverlay).toHaveBeenCalledTimes(1)
    expect(kakaoMock.state.overlays[0].content).toContain('강남구')
    expect(kakaoMock.state.overlays[0].content).toContain('map-admin-cluster')

    wrapper.unmount()

    expect(kakaoMock.state.clusterers).toHaveLength(0)
    expect(kakaoMock.state.overlays[0].setMap).toHaveBeenCalledWith(null)
  })

  it('ignores late idle callbacks after unmount without recreating map layers', async () => {
    kakaoMock.state.hasKey = true
    kakaoMock.state.mapInstance.getLevel.mockReturnValue(5)
    vi.useFakeTimers()

    try {
      const wrapper = mount(KakaoMapPanel, {
        props: {
          items: [property(3001)],
          selectedPropertyId: null,
          administrativeClusters: [administrativeCluster('legal-dong-late-idle', 'LEGAL_DONG', 'Yeoksam')]
        }
      })

      await flushPromises()

      const savedIdleHandler = kakaoMock.state.idleHandler
      expect(kakaoMock.maps.Marker).toHaveBeenCalledTimes(1)
      expect(kakaoMock.maps.MarkerClusterer).toHaveBeenCalledTimes(1)
      expect(kakaoMock.maps.CustomOverlay).toHaveBeenCalledTimes(1)

      wrapper.unmount()
      savedIdleHandler?.()
      vi.advanceTimersByTime(180)

      expect(kakaoMock.maps.Marker).toHaveBeenCalledTimes(1)
      expect(kakaoMock.maps.MarkerClusterer).toHaveBeenCalledTimes(1)
      expect(kakaoMock.maps.CustomOverlay).toHaveBeenCalledTimes(1)
    } finally {
      vi.useRealTimers()
    }
  })
})
