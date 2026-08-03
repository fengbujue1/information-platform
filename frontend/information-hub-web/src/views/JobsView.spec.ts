import { flushPromises, mount } from '@vue/test-utils'
import {
  defineComponent,
  nextTick,
  type Component,
} from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory } from 'vue-router'

import { ApiClientError } from '@/api/apiError'
import { createAppRouter } from '@/router'
import type { PageResponse } from '@/types/api'
import type { JobListItem } from '@/types/job'
import JobsView from './JobsView.vue'

const getJobsMock = vi.hoisted(() => vi.fn())

vi.mock('@/api/jobApi', () => ({
  getJobs: getJobsMock,
}))

const listItem: JobListItem = {
  id: 7,
  source: 'BOSS',
  sourceItemId: 'source-7',
  sourceUrl: null,
  title: 'Java developer',
  companyName: 'Example company',
  salaryText: '20-30K',
  salaryMinMonthlyYuan: 20_000,
  salaryMaxMonthlyYuan: 30_000,
  salaryMonths: 12,
  locationName: '成都·武侯区',
  cityName: '成都',
  experienceText: '3-5年',
  educationText: '本科',
  remoteType: 'ONSITE',
  jobStatus: 'ACTIVE',
  publishTime: null,
  firstSeenTime: '2026-07-28T01:00:00Z',
  lastSeenTime: '2026-07-28T02:00:00Z',
  currentVersionNo: 2,
}

function page(
  overrides: Partial<PageResponse<JobListItem>> = {},
): PageResponse<JobListItem> {
  return {
    page: 1,
    size: 20,
    total: 1,
    totalPages: 1,
    items: [listItem],
    ...overrides,
  }
}

const FilterStub = defineComponent({
  name: 'JobFilterForm',
  props: ['state', 'loading'],
  emits: ['search', 'reset'],
  template: `
    <div>
      <span data-test="filter-keyword">{{ state.keyword }}</span>
      <button
        data-test="search"
        @click="$emit('search', {
          keyword: 'Vue',
          company: '',
          city: '上海',
          salaryMin: null,
          salaryMax: null,
          source: '',
          jobStatus: '',
          remoteType: '',
          sortBy: 'lastSeenTime',
          sortDirection: 'asc'
        })"
      >
        search
      </button>
      <button data-test="reset" @click="$emit('reset')">reset</button>
    </div>
  `,
})

const ResultsStub = defineComponent({
  name: 'JobListResults',
  props: [
    'items',
    'loading',
    'errorMessage',
    'page',
    'size',
    'total',
    'totalPages',
  ],
  emits: [
    'retry',
    'reset',
    'pageChange',
    'sizeChange',
    'selectJob',
  ],
  template: `
    <div>
      <span data-test="loading">{{ loading }}</span>
      <span data-test="error">{{ errorMessage }}</span>
      <span data-test="title">{{ items[0]?.title }}</span>
      <button data-test="retry" @click="$emit('retry')">retry</button>
      <button data-test="page" @click="$emit('pageChange', 3)">page</button>
      <button data-test="size" @click="$emit('sizeChange', 50)">size</button>
      <button data-test="job" @click="$emit('selectJob', 7)">job</button>
    </div>
  `,
})

async function mountAt(path: string) {
  const router = createAppRouter(createMemoryHistory(), async () => ({
    id: 1,
    username: 'admin',
    displayName: 'Admin',
    timezone: 'Asia/Shanghai',
  }))
  await router.push(path)
  await router.isReady()

  const wrapper = mount(JobsView, {
    global: {
      plugins: [router],
      stubs: {
        JobFilterForm: FilterStub as Component,
        JobListResults: ResultsStub as Component,
      },
    },
  })
  await flushPromises()
  return { router, wrapper }
}

beforeEach(() => {
  getJobsMock.mockReset()
})

describe('JobsView', () => {
  it('restores URL filters and performs only one initial request', async () => {
    getJobsMock.mockResolvedValue(
      page({ page: 2, size: 50, total: 100, totalPages: 2 }),
    )

    const { wrapper } = await mountAt(
      '/jobs?page=2&size=50&keyword=Java&city=成都&sortBy=lastSeenTime&sortDirection=asc',
    )

    expect(getJobsMock).toHaveBeenCalledTimes(1)
    expect(getJobsMock).toHaveBeenCalledWith(
      expect.objectContaining({
        page: 2,
        size: 50,
        keyword: 'Java',
        city: '成都',
        sortBy: 'lastSeenTime',
        sortDirection: 'asc',
      }),
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    )
    expect(wrapper.get('[data-test="filter-keyword"]').text()).toBe('Java')
  })

  it('writes search, page, size and reset actions to the URL', async () => {
    getJobsMock.mockResolvedValue(
      page({ total: 200, totalPages: 10 }),
    )
    const { router, wrapper } = await mountAt(
      '/jobs?page=4&size=20&keyword=Java&sortBy=firstSeenTime&sortDirection=desc',
    )

    await wrapper.get('[data-test="search"]').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query).toMatchObject({
      page: '1',
      size: '20',
      keyword: 'Vue',
      city: '上海',
      sortBy: 'lastSeenTime',
      sortDirection: 'asc',
    })

    await wrapper.get('[data-test="page"]').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query.page).toBe('3')

    await wrapper.get('[data-test="size"]').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query).toMatchObject({
      page: '1',
      size: '50',
    })

    await wrapper.get('[data-test="reset"]').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({
      page: '1',
      size: '50',
      sortBy: 'firstSeenTime',
      sortDirection: 'desc',
    })
  })

  it('ignores a late response after the URL starts a newer request', async () => {
    let resolveFirst:
      | ((value: PageResponse<JobListItem>) => void)
      | undefined
    const firstRequest = new Promise<PageResponse<JobListItem>>((resolve) => {
      resolveFirst = resolve
    })
    getJobsMock
      .mockReturnValueOnce(firstRequest)
      .mockResolvedValueOnce(
        page({
          items: [{ ...listItem, title: 'New result' }],
        }),
      )

    const { router, wrapper } = await mountAt(
      '/jobs?keyword=old&page=1&size=20&sortBy=firstSeenTime&sortDirection=desc',
    )
    const firstSignal = getJobsMock.mock.calls[0]?.[1]?.signal as AbortSignal

    await router.push(
      '/jobs?keyword=new&page=1&size=20&sortBy=firstSeenTime&sortDirection=desc',
    )
    await flushPromises()
    expect(firstSignal.aborted).toBe(true)
    expect(wrapper.get('[data-test="title"]').text()).toBe('New result')

    resolveFirst?.(
      page({
        items: [{ ...listItem, title: 'Stale result' }],
      }),
    )
    await flushPromises()
    expect(wrapper.get('[data-test="title"]').text()).toBe('New result')
  })

  it('keeps the URL on error and retries the same query', async () => {
    getJobsMock
      .mockRejectedValueOnce(
        new ApiClientError({
          kind: 'network',
          code: 'NETWORK_ERROR',
          message: '无法连接到服务，请检查网络后重试',
        }),
      )
      .mockResolvedValueOnce(page())

    const { router, wrapper } = await mountAt(
      '/jobs?keyword=Java&page=1&size=20&sortBy=firstSeenTime&sortDirection=desc',
    )
    expect(wrapper.get('[data-test="error"]').text()).toContain(
      '无法连接到服务',
    )

    await wrapper.get('[data-test="retry"]').trigger('click')
    await flushPromises()
    expect(getJobsMock).toHaveBeenCalledTimes(2)
    expect(getJobsMock.mock.calls[1]?.[0]).toMatchObject({
      keyword: 'Java',
    })
    expect(router.currentRoute.value.query.keyword).toBe('Java')
  })

  it('corrects an out-of-range page once and preserves the list return URL', async () => {
    getJobsMock
      .mockResolvedValueOnce(
        page({
          page: 5,
          total: 21,
          totalPages: 2,
        }),
      )
      .mockResolvedValueOnce(
        page({
          page: 2,
          total: 21,
          totalPages: 2,
        }),
      )
    const { router, wrapper } = await mountAt(
      '/jobs?page=5&size=20&keyword=Java&sortBy=firstSeenTime&sortDirection=desc',
    )

    expect(router.currentRoute.value.query.page).toBe('2')
    expect(getJobsMock).toHaveBeenCalledTimes(2)

    await wrapper.get('[data-test="job"]').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.name).toBe('job-detail')
    expect(router.currentRoute.value.params.id).toBe('7')
    expect(router.currentRoute.value.query.from).toContain(
      '/jobs?page=2&size=20',
    )
    expect(router.currentRoute.value.query.from).toContain('keyword=Java')
  })

  it('restores the previous URL state when navigating back', async () => {
    getJobsMock.mockResolvedValue(page())
    const { router, wrapper } = await mountAt(
      '/jobs?page=1&size=20&keyword=Java&sortBy=firstSeenTime&sortDirection=desc',
    )

    await wrapper.get('[data-test="search"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-test="filter-keyword"]').text()).toBe('Vue')

    router.back()
    await nextTick()
    await flushPromises()
    expect(wrapper.get('[data-test="filter-keyword"]').text()).toBe('Java')
    expect(getJobsMock.mock.calls.at(-1)?.[0]).toMatchObject({
      keyword: 'Java',
    })
  })
})
