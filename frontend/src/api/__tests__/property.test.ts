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
      transactionTypes: ['SALE', 'JEONSE'],
      minSaleAmount: 500_000_000,
      maxSaleAmount: 2_000_000_000,
      minJeonseDepositAmount: 300_000_000,
      maxJeonseDepositAmount: 1_200_000_000
    })

    expect(getSpy).toHaveBeenCalledWith('/properties/map', {
      params: {
        swLat: 37.48,
        swLng: 127.01,
        neLat: 37.52,
        neLng: 127.06,
        zoomLevel: 5,
        propertyTypes: 'APARTMENT,OFFICETEL',
        transactionTypes: 'SALE,JEONSE',
        minSaleAmount: 500_000_000,
        maxSaleAmount: 2_000_000_000,
        minJeonseDepositAmount: 300_000_000,
        maxJeonseDepositAmount: 1_200_000_000
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
      keyword: '무악',
      propertyTypes: ['APARTMENT'],
      transactionTypes: ['JEONSE'],
      minSaleAmount: 500_000_000,
      maxSaleAmount: 2_000_000_000,
      minJeonseDepositAmount: 300_000_000,
      maxJeonseDepositAmount: 1_200_000_000,
      size: 20,
      sort: 'relevance,desc'
    })

    expect(getSpy).toHaveBeenCalledWith('/properties/search', {
      params: {
        keyword: '무악',
        propertyTypes: 'APARTMENT',
        transactionTypes: 'JEONSE',
        minSaleAmount: 500_000_000,
        maxSaleAmount: 2_000_000_000,
        minJeonseDepositAmount: 300_000_000,
        maxJeonseDepositAmount: 1_200_000_000,
        size: 20,
        sort: 'relevance,desc'
      }
    })
    expect(getSpy.mock.calls[0][1]?.params).not.toHaveProperty('zoom')
    expect(getSpy.mock.calls[0][1]?.params).not.toHaveProperty('propertyType')
  })

  it('uses the new apartment analysis endpoint', async () => {
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
      data: { propertyName: '래미안 삼성', valuation: null, shap: null, message: 'ok' }
    })
    const payload = {
      propertyName: '래미안 삼성',
      sido: '서울특별시',
      sigungu: '강남구',
      legalDong: '삼성동',
      exclusiveAreaM2: 84.95,
      floor: 12,
      topFloor: 30,
      builtYear: 2010,
      asOfDate: '2026-06-25'
    }

    await propertyApi.analyzeNewApartment(payload)

    expect(postSpy).toHaveBeenCalledWith('/properties/new-analysis', payload)
  })

  it('uses the backend address search endpoint for new apartment analysis address candidates', async () => {
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: [
        {
          fullAddress: '서울특별시 강남구 테헤란로 123',
          jibunAddress: '서울특별시 강남구 삼성동 123',
          sido: '서울특별시',
          sigungu: '강남구',
          legalDong: '삼성동',
          latitude: 37.5123,
          longitude: 127.0567
        }
      ]
    })

    await propertyApi.searchNewApartmentAddresses('래미안 삼성')

    expect(getSpy).toHaveBeenCalledWith('/properties/new-analysis/address-search', {
      params: { query: '래미안 삼성' }
    })
  })
})
