import { describe, expect, it } from 'vitest'

import { getOAuthStartUrl } from '@/api/auth'

describe('authApi OAuth start URL', () => {
  it('builds the Naver backend OAuth start URL without nesting under /api/v1', () => {
    const startUrl = getOAuthStartUrl('naver')

    expect(startUrl).toMatch(/\/oauth2\/authorization\/naver$/)
    expect(startUrl).not.toContain('/api/v1/oauth2')
  })
})
