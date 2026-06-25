import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

import NewApartmentAnalysisView from '@/views/NewApartmentAnalysisView.vue'

const propertyApiMock = vi.hoisted(() => ({
  analyzeNewApartment: vi.fn(),
  searchNewApartmentAddresses: vi.fn()
}))

const postcodeMock = vi.hoisted(() => ({
  openPostcodeSearch: vi.fn()
}))

vi.mock('@/api/property', () => ({
  propertyApi: propertyApiMock
}))

vi.mock('@/address/postcode', () => ({
  openPostcodeSearch: postcodeMock.openPostcodeSearch
}))

describe('NewApartmentAnalysisView', () => {
  it('opens Kakao postcode search and requires the selected address before analysis', async () => {
    postcodeMock.openPostcodeSearch.mockResolvedValueOnce({
      fullAddress: '서울특별시 강남구 테헤란로 123',
      roadAddress: '서울특별시 강남구 테헤란로 123',
      jibunAddress: '서울특별시 강남구 삼성동 123',
      sido: '서울특별시',
      sigungu: '강남구',
      legalDong: '삼성동',
      zonecode: '06123'
    })
    propertyApiMock.searchNewApartmentAddresses.mockResolvedValueOnce([
      {
        fullAddress: '서울특별시 강남구 테헤란로 123',
        roadAddress: '서울특별시 강남구 테헤란로 123',
        jibunAddress: '서울특별시 강남구 삼성동 123',
        sido: '서울특별시',
        sigungu: '강남구',
        legalDong: '삼성동',
        latitude: 37.5123,
        longitude: 127.0567
      }
    ])
    propertyApiMock.analyzeNewApartment.mockResolvedValueOnce({
      propertyName: '래미안 삼성',
      valuation: {
        propertyId: 0,
        supported: true,
        estimatedPrice: 1_420_000_000,
        predictionInterval: {
          lower: 1_360_000_000,
          upper: 1_480_000_000
        },
        baselineDate: '2026-06-25',
        message: 'ok'
      },
      shap: {
        propertyId: 0,
        supported: true,
        values: [
          { feature: 'area', labelKo: '전용면적', value: '84.95m²', shapValue: 54_000_000, direction: 'UP' },
          { feature: 'floor', labelKo: '상대층수', value: '10 / 30층', shapValue: 22_000_000, direction: 'UP' },
          { feature: 'age', labelKo: '준공연차', value: '16년', shapValue: -18_000_000, direction: 'DOWN' }
        ],
        message: 'ok'
      },
      message: '분석했습니다.'
    })

    const wrapper = mount(NewApartmentAnalysisView, {
      global: {
        stubs: {
          ShapChart: true
        }
      }
    })

    expect(wrapper.text()).not.toContain('지하철 거리')
    const labels = wrapper.findAll('.form-grid label > span').map((label) => label.text())
    expect(labels).not.toContain('시도')
    expect(labels).not.toContain('시군구')
    expect(labels).not.toContain('법정동')
    expect(labels).not.toContain('위도')
    expect(labels).not.toContain('경도')
    expect(labels).toContain('주소 검색')
    expect(wrapper.text()).toContain('최고 층수')

    await wrapper.get('input[placeholder="예: 래미안 삼성"]').setValue('래미안 삼성')
    await wrapper.get('[data-test="new-analysis-top-floor"]').setValue(30)
    expect(wrapper.get('[data-test="new-analysis-address"]').attributes('readonly')).toBeDefined()

    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(propertyApiMock.analyzeNewApartment).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('카카오 주소 검색에서 정확한 주소를 선택해 주세요.')

    await wrapper.get('[data-test="new-analysis-address-search"]').trigger('click')
    await flushPromises()
    expect(postcodeMock.openPostcodeSearch).toHaveBeenCalled()
    expect(propertyApiMock.searchNewApartmentAddresses).toHaveBeenCalledWith('서울특별시 강남구 테헤란로 123')
    expect(wrapper.text()).toContain('서울특별시 강남구 테헤란로 123')

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(propertyApiMock.analyzeNewApartment).toHaveBeenCalledWith(
      expect.objectContaining({
        sido: '서울특별시',
        sigungu: '강남구',
        legalDong: '삼성동',
        latitude: 37.5123,
        longitude: 127.0567,
        topFloor: 30
      })
    )
    expect(propertyApiMock.analyzeNewApartment.mock.calls[0][0]).not.toHaveProperty('distanceToStationM')

    const chartCard = wrapper.get('[data-test="new-analysis-chart-card"]')
    const estimateCard = chartCard.get('[data-test="new-analysis-estimate-card"]')
    expect(estimateCard.text()).toContain('추정 적정가')
    expect(estimateCard.text()).toContain('14.2억원')
    expect(chartCard.html().indexOf('추정 적정가')).toBeLessThan(chartCard.html().indexOf('shap-chart-stub'))

    const factorList = wrapper.get('[data-test="new-analysis-factor-list"]')
    expect(wrapper.get('[data-test="new-analysis-factor-card"]').classes()).toContain('is-stretched')
    expect(factorList.classes()).toContain('is-compressed')
    expect(factorList.classes()).toContain('fills-card-space')
    expect(factorList.findAll('.factor-row')).toHaveLength(3)
  })
})
