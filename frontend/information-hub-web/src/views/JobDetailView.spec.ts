import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory } from 'vue-router'

import { ApiClientError } from '@/api/apiError'
import { createAppRouter } from '@/router'
import type { JobDetail } from '@/types/job'
import JobDetailView from './JobDetailView.vue'

const getJobByIdMock = vi.hoisted(() => vi.fn())

vi.mock('@/api/jobApi', () => ({
  getJobById: getJobByIdMock,
}))

const detail: JobDetail = {
  id: 7,
  source: 'BOSS',
  sourceItemId: 'source-7',
  sourceUrl: 'https://example.test/jobs/7',
  title: 'Java developer',
  content: '第一行\n<strong>仅作为文本</strong>',
  publishTime: null,
  collectedAt: '2026-07-28T01:00:00Z',
  firstSeenTime: '2026-07-28T01:00:00Z',
  lastSeenTime: '2026-07-28T02:00:00Z',
  currentVersionNo: 2,
  collectorId: 'boss-collector',
  collectorVersion: '1.0',
  sourceCompanyId: 'company-1',
  sourceRecruiterId: 'recruiter-1',
  companyName: 'Example company',
  companyUrl: 'https://example.test/companies/1',
  companyScaleText: '100-499人',
  companyStageText: null,
  companyIndustryText: '互联网',
  salaryText: '20-30K',
  salarySource: 'SOURCE',
  salaryMinMonthlyYuan: 20_000,
  salaryMaxMonthlyYuan: 30_000,
  salaryMonths: 13,
  locationName: '成都·武侯区',
  cityName: '成都',
  areaName: '武侯区',
  businessDistrictName: null,
  experienceText: '3-5年',
  educationText: '本科',
  recruiterName: null,
  recruiterTitle: 'HR',
  recruiterActiveText: '2026-07-28T03:00:00+08:00',
  remoteType: 'ONSITE',
  jobStatus: 'ACTIVE',
  detailStatus: 'FETCHED',
  detailCollectedAt: null,
  sourceTags: ['3-5年', { label: '本科' }],
  sourceSkillTags: ['Java', 'Spring'],
  welfare: ['五险一金'],
}

async function mountAt(path: string) {
  const router = createAppRouter(createMemoryHistory())
  await router.push(path)
  await router.isReady()

  const wrapper = mount(JobDetailView, {
    global: {
      plugins: [router],
    },
  })
  await flushPromises()
  return { router, wrapper }
}

beforeEach(() => {
  getJobByIdMock.mockReset()
})

describe('JobDetailView', () => {
  it('loads and renders standardized detail fields as safe text', async () => {
    getJobByIdMock.mockResolvedValue(detail)

    const { wrapper } = await mountAt(
      '/jobs/7?from=%2Fjobs%3Fpage%3D2%26keyword%3DJava',
    )

    expect(getJobByIdMock).toHaveBeenCalledWith(
      7,
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    )
    expect(wrapper.text()).toContain('Java developer')
    expect(wrapper.text()).toContain('Example company')
    expect(wrapper.text()).toContain('boss-collector')
    expect(wrapper.text()).toContain('五险一金')
    expect(wrapper.get('.job-description').text()).toContain(
      '<strong>仅作为文本</strong>',
    )
    expect(wrapper.find('.job-description strong').exists()).toBe(false)

    const sourceLink = wrapper
      .findAll('a')
      .find((link) => link.text().includes('查看来源职位'))
    expect(sourceLink?.attributes('target')).toBe('_blank')
    expect(sourceLink?.attributes('rel')).toBe('noopener noreferrer')

    const returnLink = wrapper
      .findAll('a')
      .find((link) => link.text().includes('返回职位列表'))
    expect(returnLink?.attributes('href')).toContain('/jobs?page=2')
    expect(returnLink?.attributes('href')).toContain('keyword=Java')

    const snapshotsLink = wrapper
      .findAll('a')
      .find((link) => link.text().includes('查看历史快照'))
    expect(snapshotsLink?.attributes('href')).toContain(
      '/jobs/7/snapshots',
    )
    expect(snapshotsLink?.attributes('href')).toContain('from=')
  })

  it('shows nullable values without crashing', async () => {
    getJobByIdMock.mockResolvedValue({
      ...detail,
      sourceUrl: null,
      companyUrl: null,
      content: null,
      sourceTags: null,
      sourceSkillTags: null,
      welfare: null,
      recruiterActiveText: null,
    })

    const { wrapper } = await mountAt('/jobs/7')

    expect(wrapper.text()).toContain('—')
    expect(wrapper.text()).not.toContain('查看来源职位')
    expect(wrapper.get('.job-description').text()).toBe('—')
  })

  it('shows a distinct not-found state', async () => {
    getJobByIdMock.mockRejectedValue(
      new ApiClientError({
        kind: 'business',
        code: 'JOB_NOT_FOUND',
        message: '职位不存在或已不可用',
        status: 404,
      }),
    )

    const { wrapper } = await mountAt('/jobs/404')

    expect(wrapper.text()).toContain('职位不存在')
    expect(wrapper.text()).not.toContain('职位详情加载失败')
  })

  it('retries a network failure without changing the route', async () => {
    getJobByIdMock
      .mockRejectedValueOnce(
        new ApiClientError({
          kind: 'network',
          code: 'NETWORK_ERROR',
          message: '无法连接到服务，请检查网络后重试',
        }),
      )
      .mockResolvedValueOnce(detail)

    const { router, wrapper } = await mountAt(
      '/jobs/7?from=%2Fjobs%3Fpage%3D2',
    )
    expect(wrapper.text()).toContain('职位详情加载失败')

    await wrapper.get('[data-test="retry-detail"]').trigger('click')
    await flushPromises()

    expect(getJobByIdMock).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Java developer')
    expect(router.currentRoute.value.fullPath).toContain('/jobs/7')
  })

  it('rejects an invalid route id without calling the API', async () => {
    const { wrapper } = await mountAt('/jobs/not-a-number')

    expect(wrapper.text()).toContain('职位地址无效')
    expect(getJobByIdMock).not.toHaveBeenCalled()
  })

  it('aborts the old request and ignores its late response', async () => {
    let resolveFirst: ((value: JobDetail) => void) | undefined
    const firstRequest = new Promise<JobDetail>((resolve) => {
      resolveFirst = resolve
    })
    getJobByIdMock
      .mockReturnValueOnce(firstRequest)
      .mockResolvedValueOnce({
        ...detail,
        id: 8,
        title: 'New detail',
      })

    const { router, wrapper } = await mountAt('/jobs/7')
    const firstSignal = getJobByIdMock.mock.calls[0]?.[1]
      ?.signal as AbortSignal

    await router.push('/jobs/8')
    await flushPromises()

    expect(firstSignal.aborted).toBe(true)
    expect(wrapper.text()).toContain('New detail')

    resolveFirst?.({ ...detail, title: 'Stale detail' })
    await flushPromises()
    expect(wrapper.text()).toContain('New detail')
    expect(wrapper.text()).not.toContain('Stale detail')
  })
})
