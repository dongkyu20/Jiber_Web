import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { FavoriteApartmentItem, FavoriteAreaItem } from '@/api/types'
import FavoritesView from '@/views/FavoritesView.vue'

const favoritesApiMock = vi.hoisted(() => ({
  listApartments: vi.fn(),
  removeApartment: vi.fn(),
  listAreas: vi.fn(),
  removeArea: vi.fn()
}))

vi.mock('@/api/favorites', () => ({
  favoritesApi: favoritesApiMock
}))

const favoriteItem: FavoriteApartmentItem = {
  favoriteId: 11,
  propertyId: 1001,
  propertyType: 'APARTMENT',
  name: 'Gyeonghui Palace Castle',
  address: '89 Muak-dong, Jongno-gu, Seoul',
  lat: 37.5738636,
  lng: 126.9594466,
  latestTransaction: {
    transactionType: 'JEONSE',
    dealAmount: 1080000000,
    dealDate: '2026-06-08'
  },
  createdAt: '2026-06-19T10:00:00+09:00'
}

const favoriteAreaItem: FavoriteAreaItem = {
  favoriteAreaId: 801,
  label: 'Search: Muak-dong',
  centerLat: 37.5738636,
  centerLng: 126.9594466,
  zoomLevel: 6,
  createdAt: '2026-06-20T10:00:00+09:00'
}

function createApiError(code: string) {
  return {
    isAxiosError: true,
    response: {
      data: {
        code,
        message: 'Request failed.',
        path: '/api/v1/favorites/areas',
        timestamp: '2026-06-20T10:00:00+09:00'
      }
    }
  }
}

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/favorites', component: FavoritesView },
      { path: '/map', component: { template: '<main />' } },
      { path: '/chat', component: { template: '<main />' } },
      { path: '/properties/:propertyId', component: { template: '<main />' } }
    ]
  })
}

async function mountFavoritesView() {
  const router = createTestRouter()
  await router.push('/favorites')
  await router.isReady()

  const wrapper = mount(FavoritesView, {
    global: {
      plugins: [router]
    }
  })
  await flushPromises()

  return { wrapper, router }
}

async function openAreasTab(wrapper: ReturnType<typeof mount>) {
  await wrapper.findAll('.fav-tab')[1].trigger('click')
  await flushPromises()
}

beforeEach(() => {
  favoritesApiMock.listApartments.mockReset().mockResolvedValue({ items: [] })
  favoritesApiMock.removeApartment.mockReset().mockResolvedValue({
    propertyId: 1001,
    message: 'Removed.'
  })
  favoritesApiMock.listAreas.mockReset().mockResolvedValue({ items: [] })
  favoritesApiMock.removeArea.mockReset().mockResolvedValue({
    favoriteAreaId: 801,
    message: 'Removed.'
  })
})

describe('FavoritesView', () => {
  it('renders apartment and area favorite items in their tabs', async () => {
    favoritesApiMock.listApartments.mockResolvedValueOnce({ items: [favoriteItem] })
    favoritesApiMock.listAreas.mockResolvedValueOnce({ items: [favoriteAreaItem] })

    const { wrapper } = await mountFavoritesView()

    expect(favoritesApiMock.listApartments).toHaveBeenCalledTimes(1)
    expect(favoritesApiMock.listAreas).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Gyeonghui Palace Castle')
    expect(wrapper.text()).toContain('10.8')

    await openAreasTab(wrapper)

    expect(wrapper.text()).toContain('Search: Muak-dong')
    expect(wrapper.text()).toContain('37.5739, 126.9594')
  })

  it('shows empty states when there are no favorites', async () => {
    const { wrapper } = await mountFavoritesView()

    expect(wrapper.find('.empty-state').exists()).toBe(true)

    await openAreasTab(wrapper)
    expect(wrapper.find('.empty-state').exists()).toBe(true)
  })

  it('links apartment favorites to the property detail page', async () => {
    favoritesApiMock.listApartments.mockResolvedValueOnce({ items: [favoriteItem] })

    const { wrapper, router } = await mountFavoritesView()

    await wrapper.get('a[href="/properties/1001"]').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/properties/1001')
  })

  it('links area favorites back to the map with restore query parameters', async () => {
    favoritesApiMock.listAreas.mockResolvedValueOnce({ items: [favoriteAreaItem] })

    const { wrapper, router } = await mountFavoritesView()
    await openAreasTab(wrapper)

    await wrapper.get('a[href^="/map"]').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/map')
    expect(router.currentRoute.value.query).toMatchObject({
      areaLabel: 'Search: Muak-dong',
      centerLat: '37.5738636',
      centerLng: '126.9594466',
      zoomLevel: '6'
    })
  })

  it('deletes an apartment favorite and refreshes the list', async () => {
    favoritesApiMock.listApartments.mockResolvedValueOnce({ items: [favoriteItem] }).mockResolvedValueOnce({ items: [] })

    const { wrapper } = await mountFavoritesView()

    await wrapper.get('.heart-btn').trigger('click')
    await flushPromises()

    expect(favoritesApiMock.removeApartment).toHaveBeenCalledWith(1001)
    expect(favoritesApiMock.listApartments).toHaveBeenCalledTimes(2)
    expect(wrapper.find('.empty-state').exists()).toBe(true)
  })

  it('deletes an area favorite and refreshes the list', async () => {
    favoritesApiMock.listAreas.mockResolvedValueOnce({ items: [favoriteAreaItem] }).mockResolvedValueOnce({ items: [] })

    const { wrapper } = await mountFavoritesView()
    await openAreasTab(wrapper)

    await wrapper.get('.area-del-btn').trigger('click')
    await flushPromises()

    expect(favoritesApiMock.removeArea).toHaveBeenCalledWith(801)
    expect(favoritesApiMock.listAreas).toHaveBeenCalledTimes(2)
    expect(wrapper.find('.empty-state').exists()).toBe(true)
  })

  it('handles already deleted area favorites safely', async () => {
    favoritesApiMock.listAreas.mockResolvedValueOnce({ items: [favoriteAreaItem] })
    favoritesApiMock.removeArea.mockRejectedValueOnce(createApiError('FAVORITE_AREA_NOT_FOUND'))

    const { wrapper } = await mountFavoritesView()
    await openAreasTab(wrapper)

    await wrapper.get('.area-del-btn').trigger('click')
    await flushPromises()

    expect(wrapper.find('.empty-state').exists()).toBe(true)
  })
})
