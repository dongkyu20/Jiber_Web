import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { ShapValue } from '@/api/types'
import ShapChart from '@/charts/ShapChart.vue'

const echartsMock = vi.hoisted(() => {
  const setOption = vi.fn()
  const dispose = vi.fn()
  const init = vi.fn((element: HTMLElement) => ({
    setOption,
    dispose,
    getDom: () => element
  }))

  return {
    dispose,
    init,
    setOption,
    use: vi.fn()
  }
})

vi.mock('echarts/core', () => ({
  init: echartsMock.init,
  use: echartsMock.use
}))

vi.mock('echarts/charts', () => ({
  BarChart: {}
}))

vi.mock('echarts/components', () => ({
  GridComponent: {},
  TooltipComponent: {}
}))

vi.mock('echarts/renderers', () => ({
  CanvasRenderer: {}
}))

function shapValue(shapValue: number): ShapValue {
  return {
    feature: `feature-${shapValue}`,
    labelKo: `요인 ${shapValue}`,
    value: '',
    shapValue,
    direction: shapValue >= 0 ? 'UP' : 'DOWN'
  }
}

describe('ShapChart', () => {
  beforeEach(() => {
    echartsMock.dispose.mockClear()
    echartsMock.init.mockClear()
    echartsMock.setOption.mockClear()
  })

  it('keeps small SHAP axis tick labels distinct in eok units', async () => {
    mount(ShapChart, {
      props: {
        values: [
          shapValue(20_000_000),
          shapValue(40_000_000),
          shapValue(60_000_000),
          shapValue(100_000_000)
        ]
      }
    })
    await flushPromises()

    const option = echartsMock.setOption.mock.calls.at(-1)?.[0] as {
      xAxis: { axisLabel: { formatter: (value: number) => string } }
    }
    const formatter = option.xAxis.axisLabel.formatter

    expect([0, 20_000_000, 40_000_000, 60_000_000, 100_000_000].map(formatter)).toEqual([
      '0억',
      '0.2억',
      '0.4억',
      '0.6억',
      '1억'
    ])
    expect(formatter(-40_000_000)).toBe('-0.4억')
  })
})
