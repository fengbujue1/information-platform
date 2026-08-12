import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import BatchListView from '@/views/BatchListView.vue'

const listBatchesMock = vi.hoisted(() => vi.fn())

vi.mock('@/api/batchApi', () => ({
  listAnalysisBatches: listBatchesMock,
}))

describe('BatchListView', () => {
  beforeEach(() => {
    listBatchesMock.mockReset()
    listBatchesMock.mockResolvedValue([
      {
        id: 41,
        triggerType: 'MANUAL',
        status: 'COMPLETED',
        selectedCount: 3,
        estimatedTotalTokens: 3300,
        createdAt: '2026-08-04T00:00:00Z',
        progress: { actualTotalTokens: 150 },
      },
    ])
  })

  it('renders batch protocol values with Chinese labels', async () => {
    const wrapper = mount(BatchListView, {
      global: {
        stubs: { RouterLink: true },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('\u5206\u6790\u6279\u6b21')
    expect(wrapper.text()).toContain('\u624b\u52a8\u89e6\u53d1')
    expect(wrapper.text()).toContain('\u5df2\u5b8c\u6210')
    expect(wrapper.text()).not.toContain('MANUAL')
    expect(wrapper.text()).not.toContain('COMPLETED')
  })
})
