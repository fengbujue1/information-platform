import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'

import BatchDetailView from '@/views/BatchDetailView.vue'

const getBatchMock = vi.hoisted(() => vi.fn())
const getProgressMock = vi.hoisted(() => vi.fn())

vi.mock('@/api/batchApi', () => ({
  getAnalysisBatch: getBatchMock,
  getAnalysisBatchProgress: getProgressMock,
}))

describe('BatchDetailView', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    getBatchMock.mockReset()
    getProgressMock.mockReset()
    getBatchMock.mockResolvedValue({
      id: 41,
      triggerType: 'MANUAL',
      scheduleId: null,
      scheduledFor: null,
      promptProfileId: 11,
      promptVersionId: 21,
      informationType: 'JOB',
      analysisDefinitionKey: 'JOB_USER_RELEVANCE',
      analysisDefinitionVersion: 1,
      windowBasis: 'FIRST_INGESTED',
      requestedWindowDays: 3,
      windowStart: '2026-08-01T00:00:00Z',
      windowEnd: '2026-08-04T00:00:00Z',
      requestedMaxCandidates: 20,
      requestedTokenBudget: 75_000,
      totalInWindow: 1,
      eligibleCount: 1,
      alreadyAnalyzedCount: 0,
      selectedCount: 1,
      deferredByItemLimitCount: 0,
      deferredByTokenBudgetCount: 0,
      estimatedInputTokens: 120,
      estimatedOutputTokens: 1000,
      estimatedTotalTokens: 1120,
      estimateMethod: 'UTF8_BYTES_DIV3_MARGIN20_V1',
      status: 'COMPLETED',
      skipReason: null,
      startedAt: '2026-08-04T00:00:01Z',
      completedAt: '2026-08-04T00:00:02Z',
      createdAt: '2026-08-04T00:00:00Z',
      updatedAt: '2026-08-04T00:00:02Z',
      progress: {
        itemCount: 1,
        selectedCount: 0,
        runningCount: 0,
        succeededCount: 1,
        failedCount: 0,
        skippedCount: 0,
        deferredCount: 0,
        reportedInvocationCount: 1,
        unavailableInvocationCount: 0,
        actualInputTokens: 120,
        actualOutputTokens: 30,
        actualTotalTokens: 150,
      },
      items: [],
    })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('treats COMPLETED as terminal and does not keep polling', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/ai/batches/:id', component: BatchDetailView },
        { path: '/ai/batches', component: { template: '<div />' } },
      ],
    })
    await router.push('/ai/batches/41')
    await router.isReady()

    const wrapper = mount(BatchDetailView, {
      global: { plugins: [router] },
    })
    await flushPromises()
    await vi.advanceTimersByTimeAsync(3_000)

    expect(wrapper.text()).toContain('已完成')
    expect(wrapper.text()).toContain('150')
    expect(getBatchMock).toHaveBeenCalledTimes(1)
    expect(getProgressMock).not.toHaveBeenCalled()
  })
})
