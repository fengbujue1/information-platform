import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'

import ManualAnalysisView from '@/views/ManualAnalysisView.vue'

const listProfilesMock = vi.hoisted(() => vi.fn())
const previewMock = vi.hoisted(() => vi.fn())
const confirmMock = vi.hoisted(() => vi.fn())

vi.mock('@/api/promptApi', () => ({
  listPromptProfiles: listProfilesMock,
}))

vi.mock('@/api/batchApi', () => ({
  createAnalysisPreview: previewMock,
  confirmAnalysisBatch: confirmMock,
}))

vi.mock('@/api/analysisApi', () => ({
  executeAnalysis: vi.fn(),
}))

describe('ManualAnalysisView', () => {
  beforeEach(() => {
    listProfilesMock.mockReset()
    previewMock.mockReset()
    confirmMock.mockReset()
    listProfilesMock.mockResolvedValue([
      {
        id: 11,
        name: 'Default',
        activeVersionId: 21,
        status: 'ACTIVE',
      },
    ])
    previewMock.mockResolvedValue({
      windowStart: '2026-08-01T00:00:00Z',
      windowEnd: '2026-08-04T00:00:00Z',
      totalInWindow: 10,
      eligibleCount: 8,
      pendingCount: 8,
      alreadyAnalyzedCount: 2,
      selectedCount: 3,
      deferredByItemLimitCount: 5,
      deferredByTokenBudgetCount: 0,
      estimatedInputTokens: 300,
      estimatedOutputTokens: 3000,
      estimatedTotalTokens: 3300,
      estimateMethod: 'UTF8_BYTES_DIV3_MARGIN20_V1',
      expiresAt: '2026-08-06T02:00:00Z',
      previewToken: 'signed-preview',
    })
    confirmMock.mockResolvedValue({ id: 41 })
  })

  it('separates no-provider Preview from explicit Batch confirmation', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/ai/analyze', component: ManualAnalysisView },
        { path: '/ai/batches/:id', component: { template: '<div />' } },
        { path: '/ai/prompts', component: { template: '<div />' } },
      ],
    })
    await router.push('/ai/analyze')
    await router.isReady()
    const wrapper = mount(ManualAnalysisView, {
      global: { plugins: [router] },
    })
    await flushPromises()

    const previewButton = wrapper.findAll('button').find(
      (button) => button.text() === 'Preview',
    )
    await previewButton!.trigger('click')
    await flushPromises()

    expect(previewMock).toHaveBeenCalledWith({
      promptProfileId: 11,
      windowDays: 3,
      maxCandidates: 20,
      maxEstimatedTokens: 75_000,
    })
    expect(wrapper.text()).toContain('Preview 没有 Actual Token')
    expect(wrapper.text()).toContain('3300')
    expect(confirmMock).not.toHaveBeenCalled()

    const confirmButton = wrapper.findAll('button').find(
      (button) => button.text().includes('确认并创建 Batch'),
    )
    await confirmButton!.trigger('click')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalledWith('signed-preview')
    expect(router.currentRoute.value.fullPath).toBe('/ai/batches/41')
  })
})
