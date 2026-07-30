import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory } from 'vue-router'

import { ApiClientError } from '@/api/apiError'
import { createAppRouter } from '@/router'
import type { JobSnapshot } from '@/types/job'
import JobSnapshotsView from './JobSnapshotsView.vue'

const getJobSnapshotsMock = vi.hoisted(() => vi.fn())

vi.mock('@/api/jobApi', () => ({
  getJobSnapshots: getJobSnapshotsMock,
}))

const latestSnapshot: JobSnapshot = {
  id: 50,
  versionNo: 5,
  contentHash: 'a'.repeat(64),
  title: 'Latest title',
  content: '最新正文\n第二行',
  standardizedPayload: {
    companyName: 'Example company',
    sourceText: '<script>alert(',
  },
  collectedAt: '2026-07-28T03:00:00Z',
  collectorId: 'boss-collector',
  collectorVersion: '2.0',
  createdAt: '2026-07-28T04:00:00Z',
}

const olderSnapshot: JobSnapshot = {
  id: 20,
  versionNo: 2,
  contentHash: 'b'.repeat(64),
  title: 'Older title',
  content: '旧版本正文',
  standardizedPayload: {
    companyName: 'Old company',
  },
  collectedAt: '2026-07-27T03:00:00Z',
  collectorId: 'boss-collector',
  collectorVersion: '1.0',
  createdAt: '2026-07-27T04:00:00Z',
}

async function mountAt(path: string) {
  const router = createAppRouter(createMemoryHistory())
  await router.push(path)
  await router.isReady()

  const wrapper = mount(JobSnapshotsView, {
    global: {
      plugins: [router],
    },
  })
  await flushPromises()
  return { router, wrapper }
}

beforeEach(() => {
  getJobSnapshotsMock.mockReset()
})

describe('JobSnapshotsView', () => {
  it('keeps backend order, selects the newest item and switches gaps safely', async () => {
    getJobSnapshotsMock.mockResolvedValue([
      latestSnapshot,
      olderSnapshot,
    ])

    const { wrapper } = await mountAt(
      '/jobs/7/snapshots?from=%2Fjobs%3Fpage%3D2%26keyword%3DJava',
    )

    expect(getJobSnapshotsMock).toHaveBeenCalledWith(
      7,
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    )
    const versionButtons = wrapper.findAll('.snapshot-option')
    expect(versionButtons.map((button) => button.text())).toEqual([
      expect.stringContaining('版本 5'),
      expect.stringContaining('版本 2'),
    ])
    expect(wrapper.text()).toContain('Latest title')
    expect(wrapper.text()).toContain('最新正文')
    expect(wrapper.text()).toContain('Example company')
    expect(wrapper.get('.snapshot-json').text()).toContain(
      '<script>alert(',
    )
    expect(wrapper.find('.snapshot-json script').exists()).toBe(false)

    await wrapper.get('[data-test="snapshot-option-2"]').trigger('click')

    expect(wrapper.text()).toContain('Older title')
    expect(wrapper.text()).toContain('旧版本正文')
    expect(wrapper.text()).toContain('Old company')
    expect(wrapper.text()).not.toContain('最新正文')
  })

  it('shows a loading state while the request is pending', async () => {
    getJobSnapshotsMock.mockReturnValue(new Promise(() => undefined))

    const { wrapper } = await mountAt('/jobs/7/snapshots')

    expect(
      wrapper.find('[aria-label="正在加载历史快照"]').exists(),
    ).toBe(true)
  })
  it('shows an empty state without treating it as an error', async () => {
    getJobSnapshotsMock.mockResolvedValue([])

    const { wrapper } = await mountAt('/jobs/7/snapshots')

    expect(wrapper.text()).toContain('当前职位暂无历史版本')
    expect(wrapper.text()).not.toContain('历史快照加载失败')
  })

  it('preserves navigation to the current detail and original list', async () => {
    getJobSnapshotsMock.mockResolvedValue([latestSnapshot])

    const { wrapper } = await mountAt(
      '/jobs/7/snapshots?from=%2Fjobs%3Fpage%3D3%26city%3D%E6%88%90%E9%83%BD',
    )

    const detailLink = wrapper
      .findAll('a')
      .find((link) => link.text().includes('返回职位详情'))
    expect(detailLink?.attributes('href')).toContain('/jobs/7')
    expect(detailLink?.attributes('href')).toContain('from=')

    const listLink = wrapper
      .findAll('a')
      .find((link) => link.text().includes('返回职位列表'))
    expect(listLink?.attributes('href')).toContain('/jobs?page=3')
    expect(listLink?.attributes('href')).toContain(
      'city=成都',
    )
  })

  it('shows a distinct not-found state', async () => {
    getJobSnapshotsMock.mockRejectedValue(
      new ApiClientError({
        kind: 'business',
        code: 'JOB_NOT_FOUND',
        message: '职位不存在或已不可用',
        status: 404,
      }),
    )

    const { wrapper } = await mountAt('/jobs/404/snapshots')

    expect(wrapper.text()).toContain('职位不存在')
    expect(wrapper.text()).not.toContain('历史快照加载失败')
  })

  it('retries a network error on the same route', async () => {
    getJobSnapshotsMock
      .mockRejectedValueOnce(
        new ApiClientError({
          kind: 'network',
          code: 'NETWORK_ERROR',
          message: '无法连接到服务，请检查网络后重试',
        }),
      )
      .mockResolvedValueOnce([latestSnapshot])

    const { router, wrapper } = await mountAt('/jobs/7/snapshots')
    expect(wrapper.text()).toContain('历史快照加载失败')

    await wrapper.get('[data-test="retry-snapshots"]').trigger('click')
    await flushPromises()

    expect(getJobSnapshotsMock).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Latest title')
    expect(router.currentRoute.value.fullPath).toBe('/jobs/7/snapshots')
  })

  it('rejects an invalid route id without calling the API', async () => {
    const { wrapper } = await mountAt('/jobs/not-a-number/snapshots')

    expect(wrapper.text()).toContain('职位地址无效')
    expect(getJobSnapshotsMock).not.toHaveBeenCalled()
  })

  it('aborts the old request and ignores its late response', async () => {
    let resolveFirst: ((value: JobSnapshot[]) => void) | undefined
    const firstRequest = new Promise<JobSnapshot[]>((resolve) => {
      resolveFirst = resolve
    })
    getJobSnapshotsMock
      .mockReturnValueOnce(firstRequest)
      .mockResolvedValueOnce([
        {
          ...latestSnapshot,
          id: 80,
          title: 'New job snapshot',
        },
      ])

    const { router, wrapper } = await mountAt('/jobs/7/snapshots')
    const firstSignal = getJobSnapshotsMock.mock.calls[0]?.[1]
      ?.signal as AbortSignal

    await router.push('/jobs/8/snapshots')
    await flushPromises()

    expect(firstSignal.aborted).toBe(true)
    expect(wrapper.text()).toContain('New job snapshot')

    resolveFirst?.([
      {
        ...olderSnapshot,
        title: 'Stale job snapshot',
      },
    ])
    await flushPromises()
    expect(wrapper.text()).toContain('New job snapshot')
    expect(wrapper.text()).not.toContain('Stale job snapshot')
  })
})
