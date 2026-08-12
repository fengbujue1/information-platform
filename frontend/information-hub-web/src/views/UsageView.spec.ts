import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import UsageView from '@/views/UsageView.vue'

const getAnalysisUsageMock = vi.hoisted(() => vi.fn())

vi.mock('@/api/usageApi', () => ({
  getAnalysisUsage: getAnalysisUsageMock,
}))

const period = (
  actualTotalTokens: number | null,
  invocationCount: number,
) => ({
  periodStart: null,
  periodEnd: null,
  invocationCount,
  reportedInvocationCount: actualTotalTokens === null ? 0 : invocationCount,
  unavailableInvocationCount: actualTotalTokens === null ? invocationCount : 0,
  actualInputTokens:
    actualTotalTokens === null ? null : actualTotalTokens - 50,
  actualOutputTokens: actualTotalTokens === null ? null : 50,
  actualTotalTokens,
})

describe('UsageView', () => {
  beforeEach(() => {
    getAnalysisUsageMock.mockReset()
  })

  it('renders today, month and all-time Provider Actual Usage', async () => {
    getAnalysisUsageMock.mockResolvedValue({
      timezone: 'Asia/Shanghai',
      today: period(150, 1),
      month: period(700, 4),
      allTime: period(1300, 7),
    })

    const wrapper = mount(UsageView)
    await flushPromises()

    expect(wrapper.text()).toContain('自然日/月时区：Asia/Shanghai')
    expect(wrapper.text()).toContain('今日')
    expect(wrapper.text()).toContain('本月')
    expect(wrapper.text()).toContain('累计')
    expect(wrapper.text()).toContain('150')
    expect(wrapper.text()).toContain('700')
    expect(wrapper.text()).toContain('1300')
    expect(wrapper.text()).toContain('预估 Token 不会补入实际用量')
    expect(wrapper.text()).toContain('实际 Token 总数')
    expect(wrapper.text()).toContain('调用次数')
  })

  it('keeps unknown Actual Usage visibly unavailable', async () => {
    getAnalysisUsageMock.mockResolvedValue({
      timezone: 'UTC',
      today: period(null, 1),
      month: period(null, 1),
      allTime: period(null, 1),
    })

    const wrapper = mount(UsageView)
    await flushPromises()

    expect(wrapper.text()).toContain('未报告 1')
    expect(wrapper.text()).toContain('—')
  })
})
