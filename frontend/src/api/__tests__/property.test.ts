import { afterEach, describe, expect, it, vi } from 'vitest'

import { propertyApi } from '@/api/property'
import { apiClient } from '@/api/client'

describe('propertyApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('uses contract-safe map query names and comma-separated list filters', async () => {
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: { items: [], bounds: null, filters: null }
    })

    await propertyApi.getMapProperties({
      swLat: 37.48,
      swLng: 127.01,
      neLat: 37.52,
      neLng: 127.06,
      zoomLevel: 5,
      propertyTypes: ['APARTMENT', 'OFFICETEL'],
      transactionTypes: ['SALE', 'JEONSE']
    })

    expect(getSpy).toHaveBeenCalledWith('/properties/map', {
      params: {
        swLat: 37.48,
        swLng: 127.01,
        neLat: 37.52,
        neLng: 127.06,
        zoomLevel: 5,
        propertyTypes: 'APARTMENT,OFFICETEL',
        transactionTypes: 'SALE,JEONSE'
      }
    })
    expect(getSpy.mock.calls[0][1]?.params).not.toHaveProperty('zoom')
    expect(getSpy.mock.calls[0][1]?.params).not.toHaveProperty('propertyType')
  })

  it('uses the filter search endpoint for keyword searches with list filters', async () => {
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: { items: [], page: { number: 0, size: 20, totalElements: 0, totalPages: 0 } }
    })

    await propertyApi.searchProperties({
      keyword: '무악동',
      propertyTypes: ['APARTMENT'],
      transactionTypes: ['JEONSE'],
      size: 20,
      sort: 'relevance,desc'
    })

    expect(getSpy).toHaveBeenCalledWith('/properties/search', {
      params: {
        keyword: '무악동',
        propertyTypes: 'APARTMENT',
        transactionTypes: 'JEONSE',
        size: 20,
        sort: 'relevance,desc'
      }
    })
    expect(getSpy.mock.calls[0][1]?.params).not.toHaveProperty('zoom')
    expect(getSpy.mock.calls[0][1]?.params).not.toHaveProperty('propertyType')
  })
})
