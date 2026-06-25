import { describe, expect, it } from 'vitest'

import { renderMarkdown } from '@/utils/markdown'

describe('renderMarkdown', () => {
  it('renders common assistant markdown safely', () => {
    const html = renderMarkdown([
      '### 확인할 점',
      '- **보증금** 확인',
      '- `월세` 확인',
      '',
      '```',
      '<script>alert(1)</script>',
      '```'
    ].join('\n'))

    expect(html).toContain('<h5>확인할 점</h5>')
    expect(html).toContain('<ul>')
    expect(html).toContain('<strong>보증금</strong>')
    expect(html).toContain('<code>월세</code>')
    expect(html).toContain('&lt;script&gt;alert(1)&lt;/script&gt;')
    expect(html).not.toContain('<script>')
  })
})
