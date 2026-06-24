import { describe, expect, it } from 'vitest'

import { getKakaoMapFallbackMessage, hasKakaoMapKey } from '@/map/kakaoLoader'

describe('kakaoLoader', () => {
  it('reports a Korean fallback message when the Kakao Maps key is missing', () => {
    expect(hasKakaoMapKey('')).toBe(false)
    expect(getKakaoMapFallbackMessage()).toContain('카카오 지도')
    expect(getKakaoMapFallbackMessage()).toBe(
      '카카오 지도 API 키가 아직 설정되지 않았습니다. 루트 .env에 VITE_KAKAO_MAP_APP_KEY를 설정하면 실제 지도를 불러올 수 있습니다.'
    )
    expect(getKakaoMapFallbackMessage()).toContain('VITE_KAKAO_MAP_APP_KEY')
  })

  it('accepts a non-empty Kakao Maps key', () => {
    expect(hasKakaoMapKey('fake-local-key')).toBe(true)
  })
})
