import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

import { describe, expect, it } from 'vitest'

const testDirectory = dirname(fileURLToPath(import.meta.url))
const baseCss = readFileSync(resolve(testDirectory, '../styles/base.css'), 'utf8')

function cssRule(selector: string, occurrence: 'first' | 'last' = 'first'): string {
  const escapedSelector = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const matches = Array.from(baseCss.matchAll(new RegExp(`${escapedSelector}\\s*\\{(?<body>[^}]*)\\}`, 'gm')))
  const match = occurrence === 'last' ? matches.at(-1) : matches[0]

  expect(match, `${selector} rule should exist`).toBeDefined()
  return match?.groups?.body ?? ''
}

describe('base map marker styles', () => {
  it('keeps default property speech-bubble markers slightly compact', () => {
    const marker = cssRule('.map-property-marker')
    const title = cssRule('.map-property-marker strong')
    const price = cssRule('.map-property-marker span', 'last')

    expect(marker).toContain('min-width: 112px')
    expect(marker).toContain('max-width: 160px')
    expect(marker).toContain('padding: 7px 9px')
    expect(title).toContain('font-size: 0.78rem')
    expect(price).toContain('font-size: 0.71rem')
  })

  it('adds subtle hover color feedback to default and minimized property markers', () => {
    const hoveredMarker = cssRule('.map-property-marker:is(:hover, :focus-visible)')
    const hoveredMarkerTail = cssRule('.map-property-marker:is(:hover, :focus-visible)::after')
    const expandedMinimizedDetail = cssRule(
      '.map-property-marker.is-minimized:is(:hover, :focus, :focus-visible) .map-property-marker-detail'
    )
    const expandedMinimizedTail = cssRule(
      '.map-property-marker.is-minimized:is(:hover, :focus, :focus-visible) .map-property-marker-detail::after'
    )

    expect(hoveredMarker).toContain('background: rgba(244, 232, 204, 0.99)')
    expect(hoveredMarker).toContain('border-color: rgba(210, 170, 105, 0.62)')
    expect(hoveredMarkerTail).toContain('background: rgba(244, 232, 204, 0.99)')
    expect(expandedMinimizedDetail).toContain('background: rgba(244, 232, 204, 0.99)')
    expect(expandedMinimizedDetail).toContain('border-color: rgba(210, 170, 105, 0.62)')
    expect(expandedMinimizedTail).toContain('background: rgba(244, 232, 204, 0.99)')
  })

  it('expands minimized property markers in place instead of showing a tooltip above', () => {
    const minimizedMarker = cssRule('.map-property-marker.is-minimized')
    const minimizedDetail = cssRule('.map-property-marker.is-minimized .map-property-marker-detail')
    const expandedMarker = cssRule('.map-property-marker.is-minimized:is(:hover, :focus, :focus-visible)')
    const hiddenCompactTail = cssRule('.map-property-marker.is-minimized:is(:hover, :focus, :focus-visible)::after')
    const hiddenDot = cssRule(
      '.map-property-marker.is-minimized:is(:hover, :focus, :focus-visible) .map-property-marker-dot'
    )
    const expandedDetail = cssRule(
      '.map-property-marker.is-minimized:is(:hover, :focus, :focus-visible) .map-property-marker-detail'
    )

    expect(minimizedMarker).toContain('width: 46px')
    expect(minimizedMarker).toContain('height: 28px')
    expect(minimizedDetail).toContain('display: none')
    expect(minimizedDetail).toContain('position: absolute')
    expect(minimizedDetail).toContain('bottom: 0')
    expect(minimizedDetail).toContain('left: 50%')
    expect(minimizedDetail).toContain('transform: translateX(-50%)')
    expect(minimizedDetail).not.toContain('bottom: calc(100% + 10px)')
    expect(expandedMarker).toContain('width: 46px')
    expect(expandedMarker).toContain('height: 28px')
    expect(expandedMarker).not.toContain('min-width: 132px')
    expect(hiddenCompactTail).toContain('content: none')
    expect(hiddenDot).toContain('display: none')
    expect(expandedDetail).toContain('display: grid')
    expect(expandedDetail).toContain('position: absolute')
    expect(expandedDetail).toContain('bottom: 0')
    expect(expandedDetail).toContain('transform: translateX(-50%)')
  })
})

describe('property detail card styles', () => {
  it('keeps summary, AI, transaction, and chart cards readable with internal spacing', () => {
    const detailCard = cssRule('.property-detail-card')
    const detailCardHeading = cssRule('.property-detail-card h2')
    const detailParagraphs = cssRule(
      '.property-detail-card .muted,\n.property-detail-card .helper-text,\n.property-detail-card .inline-error'
    )
    const summaryRow = cssRule('.property-detail-card .summary-list div')
    const transactionTableWrap = cssRule('.transaction-history-panel .transaction-table-wrap')

    expect(detailCard).toContain('display: grid')
    expect(detailCard).toContain('gap: 16px')
    expect(detailCard).toContain('padding: 22px')
    expect(detailCardHeading).toContain('margin: 0')
    expect(detailParagraphs).toContain('line-height: 1.72')
    expect(summaryRow).toContain('align-items: flex-start')
    expect(summaryRow).toContain('padding-bottom: 12px')
    expect(transactionTableWrap).toContain('margin-top: 0')
    expect(baseCss).toMatch(
      /@media \(max-width: 560px\)[\s\S]*?\.property-detail-card \.summary-list div\s*\{[^}]*flex-direction: column/
    )
  })
})
