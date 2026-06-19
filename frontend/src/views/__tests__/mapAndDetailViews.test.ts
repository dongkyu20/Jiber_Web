import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { PagedResponse, PropertyDetail, PropertyMapItem, PropertySearchItem } from '@/api/types'
import MapView from '@/views/MapView.vue'
import PropertyDetailView from '@/views/PropertyDetailView.vue'

const propertyApiMock = vi.hoisted(() => ({
  getMapProperties: vi.fn(),
  searchProperties: vi.fn(),
  getProperty: vi.fn(),
  requestValuation: vi.fn(),
  requestShap: vi.fn()
}))

const kakaoLoaderMock = vi.hoisted(() => ({
  hasKey: false,
  hasKakaoMapKey: vi.fn(() => kakaoLoaderMock.hasKey),
  loadKakaoMaps: vi.fn(),
  getKakaoMaps: vi.fn(),
  getKakaoMapFallbackMessage: vi.fn(
    () =>
      '카카오 지도 API 키가 아직 설정되지 않았습니다. frontend/.env에 VITE_KAKAO_MAP_APP_KEY를 설정하면 실제 지도를 불러올 수 있습니다.'
  )
}))

vi.mock('@/api/property', () => ({
  propertyApi: propertyApiMock
}))

vi.mock('@/map/kakaoLoader', () => ({
  hasKakaoMapKey: kakaoLoaderMock.hasKakaoMapKey,
  loadKakaoMaps: kakaoLoaderMock.loadKakaoMaps,
  getKakaoMaps: kakaoLoaderMock.getKakaoMaps,
  getKakaoMapFallbackMessage: kakaoLoaderMock.getKakaoMapFallbackMessage
}))

function mapResponse(items: PropertyMapItem[]) {
  return {
    items,
    bounds: {
      swLat: 37.48,
      swLng: 127.01,
      neLat: 37.52,
      neLng: 127.06
    },
    filters: {
      propertyTypes: ['APARTMENT'],
      transactionTypes: ['SALE'],
      zoomLevel: 5
    }
  }
}

function searchResponse(items: PropertySearchItem[]): PagedResponse<PropertySearchItem> {
  return {
    items,
    page: {
      number: 0,
      size: 20,
      totalElements: items.length,
      totalPages: items.length ? 1 : 0
    }
  }
}

const seedMapItem: PropertyMapItem = {
  propertyId: 1001,
  propertyType: 'APARTMENT',
  name: '샘플 역삼아파트',
  address: '서울특별시 강남구 테헤란로 123',
  lat: 37.5008,
  lng: 127.0366,
  latestTransaction: {
    transactionType: 'SALE',
    dealAmount: 1250000000,
    dealDate: '2026-05-20'
  },
  dealCount: 18,
  aiAvailable: true
}

const importedSearchItem: PropertySearchItem = {
  propertyId: 1912,
  propertyType: 'APARTMENT',
  name: '경희궁롯데캐슬',
  address: '서울특별시 종로구 무악동',
  legalDong: '무악동',
  lat: 37.5738636,
  lng: 126.9594466,
  latestTransaction: {
    transactionType: 'JEONSE',
    dealAmount: 1080000000,
    dealDate: '2026-06-08'
  },
  aiAvailable: true
}

const importedDetail: PropertyDetail = {
  propertyId: 1912,
  propertyType: 'APARTMENT',
  name: '경희궁롯데캐슬',
  address: {
    sido: '서울특별시',
    sigungu: '종로구',
    legalDong: '무악동'
  },
  location: {
    lat: 37.5738636,
    lng: 126.9594466
  },
  summary: {
    builtYear: 2019,
    householdCount: null,
    latestDealAmount: 1080000000,
    latestDealDate: '2026-06-08'
  },
  transactions: [
    {
      transactionId: 5912,
      transactionType: 'JEONSE',
      dealAmount: 1080000000,
      dealDate: '2026-06-08',
      exclusiveAreaM2: 84.8792,
      floor: 10
    }
  ],
  favorite: {
    apartmentFavorited: false,
    areaFavorited: false
  },
  ai: {
    valuationAvailable: true,
    shapAvailable: true
  }
}

function createTestRouter(initialPath = '/map') {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<main />' } },
      { path: '/map', component: MapView },
      { path: '/properties/:propertyId', component: PropertyDetailView }
    ]
  })
}

async function mountMapView() {
  const router = createTestRouter('/map')
  await router.push('/map')
  await router.isReady()

  const wrapper = mount(MapView, {
    global: {
      plugins: [router]
    }
  })
  await flushPromises()

  return { wrapper, router }
}

async function mountPropertyDetailView() {
  const pinia = createPinia()
  setActivePinia(pinia)

  const router = createTestRouter('/properties/1912')
  await router.push('/properties/1912')
  await router.isReady()

  const wrapper = mount(PropertyDetailView, {
    global: {
      plugins: [pinia, router],
      stubs: {
        TransactionChart: true,
        ShapChart: true
      }
    }
  })
  await flushPromises()

  return wrapper
}

beforeEach(() => {
  kakaoLoaderMock.hasKey = false
  propertyApiMock.getMapProperties.mockReset().mockResolvedValue(mapResponse([]))
  propertyApiMock.searchProperties.mockReset().mockResolvedValue(searchResponse([]))
  propertyApiMock.getProperty.mockReset().mockResolvedValue(importedDetail)
  propertyApiMock.requestValuation.mockReset()
  propertyApiMock.requestShap.mockReset()
})

describe('MapView keyword search', () => {
  it('renders a Korean keyword search input', async () => {
    const { wrapper } = await mountMapView()

    expect(wrapper.text()).toContain('단지명 또는 지역 검색')
    expect(wrapper.get('[data-test="map-search-keyword"]').attributes('placeholder')).toContain('경희궁롯데캐슬')
  })

  it('searches imported canonical data and keeps the missing-key fallback usable', async () => {
    propertyApiMock.searchProperties.mockResolvedValueOnce(searchResponse([importedSearchItem]))
    const { wrapper } = await mountMapView()

    await wrapper.get('[data-test="map-search-keyword"]').setValue('경희궁롯데캐슬')
    await wrapper.get('[data-test="map-search-form"]').trigger('submit')
    await flushPromises()

    expect(propertyApiMock.searchProperties).toHaveBeenCalledWith(
      expect.objectContaining({
        keyword: '경희궁롯데캐슬',
        propertyTypes: ['APARTMENT'],
        transactionTypes: ['SALE', 'JEONSE', 'MONTHLY_RENT'],
        size: 20,
        sort: 'relevance,desc'
      })
    )
    expect(wrapper.text()).toContain('카카오 지도 API 키가 아직 설정되지 않았습니다.')
    expect(wrapper.text()).toContain('경희궁롯데캐슬')
    expect(wrapper.text()).toContain('전세')
    expect(wrapper.find('#property-result-1912').classes()).toContain('is-selected')
  })

  it('applies transaction filters to keyword searches and routes from a result item', async () => {
    propertyApiMock.searchProperties.mockResolvedValueOnce(searchResponse([importedSearchItem]))
    const { wrapper, router } = await mountMapView()

    await wrapper.get('input[value="SALE"]').setValue(false)
    await wrapper.get('input[value="MONTHLY_RENT"]').setValue(false)
    await wrapper.get('input[value="JEONSE"]').setValue(true)
    await wrapper.get('[data-test="map-search-keyword"]').setValue('무악동')
    await wrapper.get('[data-test="map-search-form"]').trigger('submit')
    await flushPromises()

    expect(propertyApiMock.searchProperties).toHaveBeenCalledWith(
      expect.objectContaining({
        keyword: '무악동',
        propertyTypes: ['APARTMENT'],
        transactionTypes: ['JEONSE']
      })
    )
    expect(propertyApiMock.searchProperties.mock.calls[0][0]).not.toHaveProperty('zoom')
    expect(propertyApiMock.searchProperties.mock.calls[0][0]).not.toHaveProperty('propertyType')

    await wrapper.get('a[href="/properties/1912"]').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/properties/1912')
  })

  it('returns to map bounds search when an empty keyword is submitted', async () => {
    propertyApiMock.getMapProperties
      .mockResolvedValueOnce(mapResponse([]))
      .mockResolvedValueOnce(mapResponse([seedMapItem]))
    propertyApiMock.searchProperties.mockResolvedValueOnce(searchResponse([importedSearchItem]))
    const { wrapper } = await mountMapView()

    await wrapper.get('[data-test="map-search-keyword"]').setValue('종로구')
    await wrapper.get('[data-test="map-search-form"]').trigger('submit')
    await flushPromises()
    await wrapper.get('[data-test="map-search-keyword"]').setValue('   ')
    await wrapper.get('[data-test="map-search-form"]').trigger('submit')
    await flushPromises()

    expect(propertyApiMock.getMapProperties).toHaveBeenCalledTimes(2)
    expect(propertyApiMock.searchProperties).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('샘플 역삼아파트')
    expect(wrapper.text()).not.toContain('"종로구" 검색 결과')
  })
})

describe('PropertyDetailView transaction summary', () => {
  it('shows the latest transaction type and recent transaction count in Korean', async () => {
    const wrapper = await mountPropertyDetailView()

    expect(propertyApiMock.getProperty).toHaveBeenCalledWith('1912')
    expect(wrapper.text()).toContain('경희궁롯데캐슬')
    expect(wrapper.text()).toContain('최근 거래유형')
    expect(wrapper.text()).toContain('전세')
    expect(wrapper.text()).toContain('최근 거래 1건')
  })
})
