import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type {
  AdministrativeCluster,
  PagedResponse,
  PropertyDetail,
  PropertyMapItem,
  PropertyMapResponse,
  PropertySearchItem
} from '@/api/types'
import KakaoMapPanel from '@/components/KakaoMapPanel.vue'
import { useAuthStore } from '@/stores/auth'
import MapView from '@/views/MapView.vue'
import PropertyDetailView from '@/views/PropertyDetailView.vue'

const userSession = {
  accessToken: 'memory-only-detail-token',
  user: {
    userId: 7,
    email: 'detail@example.com',
    displayName: '상세 사용자',
    roles: ['USER' as const]
  }
}

const propertyApiMock = vi.hoisted(() => ({
  getMapProperties: vi.fn(),
  searchProperties: vi.fn(),
  getProperty: vi.fn(),
  requestValuation: vi.fn(),
  requestShap: vi.fn()
}))

const favoritesApiMock = vi.hoisted(() => ({
  addApartment: vi.fn(),
  removeApartment: vi.fn(),
  addArea: vi.fn()
}))

const kakaoLoaderMock = vi.hoisted(() => ({
  hasKey: false,
  hasKakaoMapKey: vi.fn(() => kakaoLoaderMock.hasKey),
  loadKakaoMaps: vi.fn(),
  getKakaoMaps: vi.fn(),
  getKakaoMapFallbackMessage: vi.fn(
    () =>
'카카오 지도 API 키가 아직 설정되지 않았습니다. root .env 또는 frontend 실행 환경에 VITE_KAKAO_MAP_APP_KEY를 설정하면 실제 지도를 불러올 수 있습니다.'
  )
}))

vi.mock('@/api/property', () => ({
  propertyApi: propertyApiMock
}))

vi.mock('@/api/favorites', () => ({
  favoritesApi: favoritesApiMock
}))

vi.mock('@/map/kakaoLoader', () => ({
  hasKakaoMapKey: kakaoLoaderMock.hasKakaoMapKey,
  loadKakaoMaps: kakaoLoaderMock.loadKakaoMaps,
  getKakaoMaps: kakaoLoaderMock.getKakaoMaps,
  getKakaoMapFallbackMessage: kakaoLoaderMock.getKakaoMapFallbackMessage
}))

function mapResponse(items: PropertyMapItem[], administrativeClusters: AdministrativeCluster[] = []): PropertyMapResponse {
  return {
    items,
    administrativeClusters,
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
      size: 100,
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
  recentTransactionCount: 4,
  aiAvailable: true
}

const seedAdministrativeCluster: AdministrativeCluster = {
  clusterId: 'legal-dong-1168010100',
  level: 'LEGAL_DONG',
  sido: 'Seoul',
  sigungu: 'Gangnam-gu',
  legalDong: 'Yeoksam-dong',
  label: 'Yeoksam-dong',
  centerLat: 37.5008,
  centerLng: 127.0366,
  propertyCount: 12,
  transactionCount: 34,
  averageDealAmount: 1230000000
}

const importedSearchItem: PropertySearchItem = {
  propertyId: 1001,
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

const monthlyRentSearchItem: PropertySearchItem = {
  ...importedSearchItem,
  propertyId: 1002,
  name: '월세 샘플아파트',
  latestTransaction: {
    transactionType: 'MONTHLY_RENT',
    dealAmount: 550000000,
    depositAmount: 550000000,
    monthlyRent: 1200000,
    dealDate: '2026-06-10'
  }
}

function searchResponsePage(
  items: PropertySearchItem[],
  page: { number: number; size: number; totalElements: number; totalPages: number }
): PagedResponse<PropertySearchItem> {
  return {
    items,
    page
  }
}

const importedDetail: PropertyDetail = {
  propertyId: 1001,
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
      dealAmount: null,
      depositAmount: 1080000000,
      monthlyRent: 0,
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
      { path: '/chat', component: { template: '<main />' } },
      { path: '/login', component: { template: '<main />' } },
      { path: '/properties/:propertyId', component: PropertyDetailView }
    ]
  })
}

async function mountMapView(options: { authenticated?: boolean; path?: string } = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createTestRouter('/map')
  await router.push(options.path ?? '/map')
  await router.isReady()

  if (options.authenticated) {
    useAuthStore().setSession(userSession)
  }

  const wrapper = mount(MapView, {
    global: {
      plugins: [pinia, router]
    }
  })
  await flushPromises()

  return { wrapper, router }
}

function detailWithFavorite(apartmentFavorited: boolean): PropertyDetail {
  return {
    ...importedDetail,
    favorite: {
      apartmentFavorited,
      areaFavorited: false
    }
  }
}

function detailWithTransactionMix(): PropertyDetail {
  return {
    ...importedDetail,
    transactions: [
      {
        transactionId: 7001,
        transactionType: 'SALE',
        dealAmount: 1250000000,
        dealDate: '2026-06-09',
        exclusiveAreaM2: 84.95,
        floor: 15
      },
      {
        transactionId: 5912,
        transactionType: 'JEONSE',
        dealAmount: null,
        depositAmount: 1080000000,
        monthlyRent: 0,
        dealDate: '2026-06-08',
        exclusiveAreaM2: 84.8792,
        floor: 10
      },
      {
        transactionId: 7003,
        transactionType: 'MONTHLY_RENT',
        dealAmount: null,
        depositAmount: 500000000,
        monthlyRent: 1200000,
        dealDate: '2026-05-01',
        exclusiveAreaM2: 59.97,
        floor: 4
      }
    ]
  }
}

function createApiError(code: string) {
  return {
    isAxiosError: true,
    response: {
      data: {
        code,
        message: '요청을 처리하지 못했습니다.',
        path: '/api/v1/favorites/apartments',
        timestamp: '2026-06-19T10:00:00+09:00'
      }
    }
  }
}

async function mountPropertyDetailView(options: { authenticated?: boolean; detail?: PropertyDetail } = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)
  propertyApiMock.getProperty.mockResolvedValueOnce(options.detail ?? importedDetail)

  const router = createTestRouter('/properties/1001')
  await router.push('/properties/1001')
  await router.isReady()

  if (options.authenticated) {
    useAuthStore().setSession(userSession)
  }

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
  favoritesApiMock.addApartment.mockReset().mockResolvedValue({
    favoriteId: 1,
    propertyId: 1001,
    createdAt: '2026-06-19T10:00:00+09:00',
    message: '저장했습니다.'
  })
  favoritesApiMock.removeApartment.mockReset().mockResolvedValue({
    propertyId: 1001,
    message: '삭제했습니다.'
  })
  favoritesApiMock.addArea.mockReset().mockResolvedValue({
    favoriteAreaId: 801,
    label: '현재 지도 영역',
    createdAt: '2026-06-19T10:00:00+09:00',
    message: '저장했습니다.'
  })
})

describe('MapView keyword search', () => {
  it('normalizes duplicate transaction-like map rows into one property result with a sale-average marker payload', async () => {
    const runtimeYear = new Date().getFullYear()
    const duplicateTransactionRows: PropertyMapItem[] = [
      {
        ...seedMapItem,
        dealCount: 1,
        recentTransactionCount: 1,
        latestTransaction: {
          transactionType: 'SALE',
          dealAmount: 1000000000,
          dealDate: `${runtimeYear}-06-01`
        }
      },
      {
        ...seedMapItem,
        dealCount: 1,
        recentTransactionCount: 1,
        latestTransaction: {
          transactionType: 'JEONSE',
          dealAmount: 1200000000,
          dealDate: `${runtimeYear - 1}-07-01`
        }
      }
    ]
    propertyApiMock.getMapProperties.mockResolvedValueOnce(mapResponse(duplicateTransactionRows))

    const { wrapper } = await mountMapView()
    const panelItems = wrapper.findComponent(KakaoMapPanel).props('items') as PropertyMapItem[]

    expect(panelItems).toHaveLength(1)
    expect(panelItems[0].propertyId).toBe(seedMapItem.propertyId)
    expect(panelItems[0].recentTransactionCount).toBe(2)
    expect(panelItems[0].recentYearAverageDealAmount).toBe(1000000000)
    expect(wrapper.findAll('.result-list li')).toHaveLength(1)
  })

  it('passes administrative clusters from map search to KakaoMapPanel and clears them for keyword search', async () => {
    propertyApiMock.getMapProperties.mockResolvedValueOnce(mapResponse([seedMapItem], [seedAdministrativeCluster]))
    propertyApiMock.searchProperties.mockResolvedValueOnce(searchResponse([importedSearchItem]))
    const { wrapper } = await mountMapView()

    expect(wrapper.findComponent(KakaoMapPanel).props('administrativeClusters')).toEqual([seedAdministrativeCluster])

    await wrapper.get('[data-test="map-search-keyword"]').setValue('keyword')
    await wrapper.get('[data-test="map-search-form"]').trigger('submit')
    await flushPromises()

    expect(wrapper.findComponent(KakaoMapPanel).props('administrativeClusters')).toEqual([])
  })

  it('clears administrative clusters when map bounds search fails', async () => {
    propertyApiMock.getMapProperties
      .mockResolvedValueOnce(mapResponse([seedMapItem], [seedAdministrativeCluster]))
      .mockRejectedValueOnce(new Error('map failed'))
    const { wrapper } = await mountMapView()

    expect(wrapper.findComponent(KakaoMapPanel).props('administrativeClusters')).toEqual([seedAdministrativeCluster])

    wrapper.findComponent(KakaoMapPanel).vm.$emit('loadError')
    await flushPromises()

    expect(wrapper.findComponent(KakaoMapPanel).props('administrativeClusters')).toEqual([])
    expect(wrapper.text()).toContain('실거래 데이터 API 연결에 실패했습니다.')
    expect(wrapper.text()).toContain('VITE_API_BASE_URL')
    expect(wrapper.text()).toContain('DB_PORT')
  })

  it('shows separate diagnostics for missing Kakao key and backend map API failure', async () => {
    propertyApiMock.getMapProperties.mockRejectedValueOnce(new Error('backend failed'))

    const { wrapper } = await mountMapView()

    expect(wrapper.text()).toContain('지도 SDK')
    expect(wrapper.text()).toContain('VITE_KAKAO_MAP_APP_KEY')
    expect(wrapper.text()).toContain('목록 검색은 계속 사용할 수 있습니다.')
    expect(wrapper.text()).toContain('실거래 데이터 API 연결에 실패했습니다.')
    expect(wrapper.text()).toContain('VITE_API_BASE_URL')
    expect(wrapper.text()).toContain('DB_PORT')
  })

  it('renders a Korean keyword search input', async () => {
    const { wrapper } = await mountMapView()

    expect(wrapper.text()).toContain('단지명 또는 지역 검색')
    expect(wrapper.get('[data-test="map-search-keyword"]').attributes('placeholder')).toContain('경희궁롯데캐슬')
  })

  it('keeps map search apartment-only without showing property type filters', async () => {
    const { wrapper } = await mountMapView()

    expect(wrapper.text()).not.toContain('부동산 유형')
    expect(wrapper.find('input[value="OFFICETEL"]').exists()).toBe(false)
    expect(wrapper.find('input[value="VILLA"]').exists()).toBe(false)
    expect(wrapper.find('input[value="HOUSE"]').exists()).toBe(false)
    expect(propertyApiMock.getMapProperties).toHaveBeenCalledWith(
      expect.objectContaining({
        propertyTypes: ['APARTMENT']
      })
    )
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
        size: 100,
        sort: 'relevance,desc'
      })
    )
    expect(wrapper.text()).toContain('카카오 지도 API 키가 아직 설정되지 않았습니다.')
    expect(wrapper.text()).toContain('경희궁롯데캐슬')
    expect(wrapper.text()).toContain('전세')
    expect(wrapper.find('#property-result-1001').classes()).toContain('is-selected')
  })

  it('shows monthly rent search results with separate deposit and monthly rent labels', async () => {
    propertyApiMock.searchProperties.mockResolvedValueOnce(searchResponse([monthlyRentSearchItem]))
    const { wrapper } = await mountMapView()

    await wrapper.get('[data-test="map-search-keyword"]').setValue('월세')
    await wrapper.get('[data-test="map-search-form"]').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('월세')
    expect(wrapper.text()).toContain('보증금 5.5억 원 / 월세 120만 원')
  })

  it('loads additional keyword result pages instead of stopping at the first page', async () => {
    const secondSearchItem: PropertySearchItem = {
      ...importedSearchItem,
      propertyId: 1003,
      name: '두번째 검색아파트'
    }
    propertyApiMock.searchProperties
      .mockResolvedValueOnce(
        searchResponsePage([importedSearchItem], {
          number: 0,
          size: 100,
          totalElements: 2,
          totalPages: 2
        })
      )
      .mockResolvedValueOnce(
        searchResponsePage([secondSearchItem], {
          number: 1,
          size: 100,
          totalElements: 2,
          totalPages: 2
        })
      )
    const { wrapper } = await mountMapView()

    await wrapper.get('[data-test="map-search-keyword"]').setValue('아파트')
    await wrapper.get('[data-test="map-search-form"]').trigger('submit')
    await flushPromises()
    await wrapper.get('[data-test="keyword-load-more"]').trigger('click')
    await flushPromises()

    expect(propertyApiMock.searchProperties).toHaveBeenLastCalledWith(
      expect.objectContaining({
        keyword: '아파트',
        page: 1,
        size: 100
      })
    )
    expect(wrapper.text()).toContain('경희궁롯데캐슬')
    expect(wrapper.text()).toContain('두번째 검색아파트')
  })

  it('suggests cached apartment keywords through the search trie', async () => {
    propertyApiMock.getMapProperties.mockResolvedValueOnce(mapResponse([seedMapItem]))
    propertyApiMock.searchProperties.mockResolvedValueOnce(searchResponse([seedMapItem as unknown as PropertySearchItem]))
    const { wrapper } = await mountMapView()

    await wrapper.get('[data-test="map-search-keyword"]').setValue('샘플')

    expect(wrapper.get('[data-test="map-search-suggestions"]').text()).toContain('샘플 역삼아파트')
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

    await wrapper.get('a[href="/properties/1001"]').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/properties/1001')
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

  it('adds the current map area as a favorite for authenticated users', async () => {
    const { wrapper } = await mountMapView({ authenticated: true })

    await wrapper.get('[data-test="area-favorite-button"]').trigger('click')
    await flushPromises()

    expect(favoritesApiMock.addArea).toHaveBeenCalledWith(
      expect.objectContaining({
        label: '현재 지도 영역',
        centerLat: 37.5,
        centerLng: 127.035,
        zoomLevel: 5
      })
    )
    expect(wrapper.text()).toContain('관심 지역에 추가했습니다.')
  })

  it('does not call the area favorite API for anonymous users', async () => {
    const { wrapper } = await mountMapView()

    await wrapper.get('[data-test="area-favorite-button"]').trigger('click')
    await flushPromises()

    expect(favoritesApiMock.addArea).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('로그인 후 관심 지역을 저장할 수 있습니다.')
  })

  it('handles duplicate and validation area favorite errors in Korean', async () => {
    favoritesApiMock.addArea.mockRejectedValueOnce(createApiError('FAVORITE_AREA_ALREADY_EXISTS'))
    const duplicateWrapper = await mountMapView({ authenticated: true })

    await duplicateWrapper.wrapper.get('[data-test="area-favorite-button"]').trigger('click')
    await flushPromises()

    expect(duplicateWrapper.wrapper.text()).toContain('이미 관심 지역에 저장되어 있습니다.')

    favoritesApiMock.addArea.mockRejectedValueOnce(createApiError('VALIDATION_FAILED'))
    const validationWrapper = await mountMapView({ authenticated: true })

    await validationWrapper.wrapper.get('[data-test="area-favorite-button"]').trigger('click')
    await flushPromises()

    expect(validationWrapper.wrapper.text()).toContain('관심 지역 정보를 저장할 수 없습니다.')
  })

  it('uses the active keyword as the area favorite label and selected result as center', async () => {
    propertyApiMock.searchProperties.mockResolvedValueOnce(searchResponse([importedSearchItem]))
    const { wrapper } = await mountMapView({ authenticated: true })

    await wrapper.get('[data-test="map-search-keyword"]').setValue('무악동')
    await wrapper.get('[data-test="map-search-form"]').trigger('submit')
    await flushPromises()
    await wrapper.get('[data-test="area-favorite-button"]').trigger('click')
    await flushPromises()

    expect(favoritesApiMock.addArea).toHaveBeenCalledWith(
      expect.objectContaining({
        label: '검색: 무악동',
        centerLat: importedSearchItem.lat,
        centerLng: importedSearchItem.lng,
        zoomLevel: 5
      })
    )
  })

  it('restores a favorite area from map query parameters without a Kakao key', async () => {
    const { wrapper } = await mountMapView({
      path: '/map?areaLabel=%EA%B2%80%EC%83%89%3A%20%EB%AC%B4%EC%95%85%EB%8F%99&centerLat=37.5738636&centerLng=126.9594466&zoomLevel=6'
    })

    expect(propertyApiMock.getMapProperties).toHaveBeenCalledWith(
      expect.objectContaining({
        swLat: 37.5538636,
        swLng: 126.9344466,
        neLat: 37.5938636,
        neLng: 126.9844466,
        zoomLevel: 6
      })
    )
    expect(wrapper.text()).toContain('검색: 무악동 관심 지역을 지도로 불러왔습니다.')
  })
})

describe('PropertyDetailView transaction summary', () => {
  it('shows the latest transaction type and recent transaction count in Korean', async () => {
    const wrapper = await mountPropertyDetailView()

    expect(propertyApiMock.getProperty).toHaveBeenCalledWith('1001')
    expect(wrapper.text()).toContain('경희궁롯데캐슬')
    expect(wrapper.text()).toContain('최근 거래유형')
    expect(wrapper.text()).toContain('전세')
    expect(wrapper.text()).toContain('최근 거래 1건')
  })

  it('replaces the transaction chart with a transaction history table', async () => {
    const wrapper = await mountPropertyDetailView({ detail: detailWithTransactionMix() })

    expect(wrapper.text()).toContain('거래 내역')
    expect(wrapper.findComponent({ name: 'TransactionChart' }).exists()).toBe(false)
    expect(wrapper.findAll('[data-test="transaction-row"]')).toHaveLength(3)
    expect(wrapper.text()).toContain('거래일')
    expect(wrapper.text()).toContain('거래유형')
    expect(wrapper.text()).toContain('거래금액')
    expect(wrapper.text()).toContain('전용면적')
    expect(wrapper.text()).toContain('층수')
    expect(wrapper.text()).toContain('10.8억 원')
    expect(wrapper.text()).toContain('보증금 5억 원 / 월세 120만 원')
  })

  it('filters the transaction history by selected transaction types', async () => {
    const wrapper = await mountPropertyDetailView({ detail: detailWithTransactionMix() })

    await wrapper.get('[data-test="transaction-type-filter-SALE"]').setValue(false)
    await wrapper.get('[data-test="transaction-type-filter-MONTHLY_RENT"]').setValue(false)

    const rows = wrapper.findAll('[data-test="transaction-row"]')
    expect(rows).toHaveLength(1)
    expect(rows[0].text()).toContain('전세')
    expect(rows[0].text()).toContain('2026년 6월 8일')
  })

  it('sorts the transaction history by clickable column headers', async () => {
    const wrapper = await mountPropertyDetailView({ detail: detailWithTransactionMix() })

    await wrapper.get('[data-test="transaction-sort-dealAmount"]').trigger('click')
    expect(wrapper.findAll('[data-test="transaction-row"]')[0].text()).toContain('월세')

    await wrapper.get('[data-test="transaction-sort-dealAmount"]').trigger('click')
    expect(wrapper.findAll('[data-test="transaction-row"]')[0].text()).toContain('매매')
  })

  it('reflects initial apartment favorite states', async () => {
    const unfavorited = await mountPropertyDetailView({ authenticated: true, detail: detailWithFavorite(false) })
    expect(unfavorited.text()).toContain('관심 아파트 추가')

    const favorited = await mountPropertyDetailView({ authenticated: true, detail: detailWithFavorite(true) })
    expect(favorited.text()).toContain('관심 아파트 삭제')
  })

  it('adds an apartment favorite and updates the button state', async () => {
    const wrapper = await mountPropertyDetailView({ authenticated: true, detail: detailWithFavorite(false) })

    await wrapper.get('[data-test="apartment-favorite-button"]').trigger('click')
    await flushPromises()

    expect(favoritesApiMock.addApartment).toHaveBeenCalledWith(1001)
    expect(wrapper.text()).toContain('관심 아파트에 추가했습니다.')
    expect(wrapper.text()).toContain('관심 아파트 삭제')
  })

  it('removes an apartment favorite and updates the button state', async () => {
    const wrapper = await mountPropertyDetailView({ authenticated: true, detail: detailWithFavorite(true) })

    await wrapper.get('[data-test="apartment-favorite-button"]').trigger('click')
    await flushPromises()

    expect(favoritesApiMock.removeApartment).toHaveBeenCalledWith(1001)
    expect(wrapper.text()).toContain('관심 아파트에서 삭제했습니다.')
    expect(wrapper.text()).toContain('관심 아파트 추가')
  })

  it('guides anonymous users to log in before saving favorites', async () => {
    const wrapper = await mountPropertyDetailView({ detail: detailWithFavorite(false) })

    await wrapper.get('[data-test="apartment-favorite-button"]').trigger('click')
    await flushPromises()

    expect(favoritesApiMock.addApartment).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('로그인 후 관심 아파트를 저장할 수 있습니다.')
  })

  it('handles duplicate favorite creation safely', async () => {
    favoritesApiMock.addApartment.mockRejectedValueOnce(createApiError('FAVORITE_ALREADY_EXISTS'))
    const wrapper = await mountPropertyDetailView({ authenticated: true, detail: detailWithFavorite(false) })

    await wrapper.get('[data-test="apartment-favorite-button"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('이미 관심 아파트에 저장되어 있습니다.')
    expect(wrapper.text()).toContain('관심 아파트 삭제')
  })

  it('handles missing favorite deletion safely', async () => {
    favoritesApiMock.removeApartment.mockRejectedValueOnce(createApiError('FAVORITE_NOT_FOUND'))
    const wrapper = await mountPropertyDetailView({ authenticated: true, detail: detailWithFavorite(true) })

    await wrapper.get('[data-test="apartment-favorite-button"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('이미 삭제된 관심 아파트입니다.')
    expect(wrapper.text()).toContain('관심 아파트 추가')
  })

  it('requests valuation and SHAP from the selected transaction input', async () => {
    propertyApiMock.requestValuation.mockResolvedValueOnce({
      propertyId: 1001,
      supported: true,
      estimatedPrice: 1200000000,
      currency: 'KRW',
      modelVersion: 'model-v1',
      baselineDate: '2026-06-24',
      featureSetVersion: 'features-v1',
      message: '추정가를 계산했습니다.'
    })
    propertyApiMock.requestShap.mockResolvedValueOnce({
      propertyId: 1001,
      supported: true,
      baseValue: 1000000000,
      prediction: 1200000000,
      currency: 'KRW',
      values: [
        {
          feature: 'area',
          labelKo: '면적',
          value: 84.8792,
          shapValue: 120000000,
          direction: 'UP'
        }
      ],
      modelVersion: 'model-v1',
      baselineDate: '2026-06-24',
      featureSetVersion: 'features-v1',
      message: '요인을 계산했습니다.'
    })
    const wrapper = await mountPropertyDetailView({ authenticated: true })

    await wrapper.get('[data-test="property-ai-button"]').trigger('click')
    await flushPromises()

    expect(propertyApiMock.requestValuation).toHaveBeenCalledWith('1001', {
      exclusiveAreaM2: 84.8792,
      floor: 10,
      asOfDate: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/)
    })
    expect(propertyApiMock.requestShap).toHaveBeenCalledWith('1001', {
      exclusiveAreaM2: 84.8792,
      floor: 10,
      asOfDate: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/)
    })
    expect(wrapper.text()).toContain('추정가 12억 원')
    expect(wrapper.text()).toContain('요인을 계산했습니다.')
  })
})
