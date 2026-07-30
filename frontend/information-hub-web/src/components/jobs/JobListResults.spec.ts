import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import type { JobListItem } from '@/types/job'
import JobListResults from './JobListResults.vue'

const longTitle = '高级 Java 工程师'.repeat(40)
const item: JobListItem = {
  id: 7,
  source: 'BOSS',
  sourceItemId: 'source-7',
  sourceUrl: null,
  title: longTitle,
  companyName: null,
  salaryText: null,
  salaryMinMonthlyYuan: null,
  salaryMaxMonthlyYuan: null,
  salaryMonths: null,
  locationName: null,
  cityName: null,
  experienceText: null,
  educationText: null,
  remoteType: 'UNKNOWN',
  jobStatus: 'UNKNOWN',
  publishTime: null,
  firstSeenTime: 'invalid-time',
  lastSeenTime: '2026-07-30T01:00:00Z',
  currentVersionNo: 1,
}

function mountResults(
  overrides: Partial<InstanceType<typeof JobListResults>['$props']> = {},
) {
  return mount(JobListResults, {
    props: {
      items: [item],
      loading: false,
      errorMessage: null,
      page: 2,
      size: 20,
      total: 80,
      totalPages: 4,
      ...overrides,
    },
  })
}

describe('JobListResults', () => {
  it('keeps long and malformed values renderable in the keyboard button card', () => {
    const wrapper = mountResults()
    const card = wrapper.get('.job-card')

    expect(card.element.tagName).toBe('BUTTON')
    expect(card.attributes('type')).toBe('button')
    expect(card.text()).toContain(longTitle)
    expect(card.text()).toContain('—')
  })

  it('renders full and compact pagination without horizontal-only navigation', () => {
    const wrapper = mountResults()

    expect(wrapper.find('.jobs-pagination-desktop').exists()).toBe(true)
    expect(wrapper.find('.jobs-pagination-mobile').exists()).toBe(true)
    expect(
      wrapper
        .get('.jobs-pagination-mobile')
        .attributes('aria-label'),
    ).toBe('职位结果紧凑分页')
  })

  it('uses the shared alert state and emits retry once per activation', async () => {
    const wrapper = mountResults({
      items: [],
      errorMessage: '无法连接到服务',
    })

    expect(wrapper.get('[data-state="error"]').attributes('role')).toBe(
      'alert',
    )
    await wrapper.get('[data-test="retry-jobs"]').trigger('click')
    expect(wrapper.emitted('retry')).toHaveLength(1)
  })

  it('uses the shared loading and empty states', () => {
    const loading = mountResults({ items: [], loading: true })
    const empty = mountResults({ items: [], total: 0, totalPages: 0 })

    expect(loading.find('[data-state="loading"]').exists()).toBe(true)
    expect(empty.find('[data-state="empty"]').exists()).toBe(true)
  })
})
