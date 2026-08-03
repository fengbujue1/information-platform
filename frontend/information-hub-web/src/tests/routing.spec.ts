import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory } from 'vue-router'

import App from '@/App.vue'
import { createAppRouter } from '@/router'
import { setAuthenticatedUserForTest } from '@/stores/authSession'

const testUser = {
  id: 1,
  username: 'admin',
  displayName: 'Admin',
  timezone: 'Asia/Shanghai',
}

const getJobsMock = vi.hoisted(() => vi.fn())
const getJobByIdMock = vi.hoisted(() => vi.fn())
const getJobSnapshotsMock = vi.hoisted(() => vi.fn())

vi.mock('@/api/jobApi', () => ({
  getJobs: getJobsMock,
  getJobById: getJobByIdMock,
  getJobSnapshots: getJobSnapshotsMock,
}))

beforeEach(() => {
  getJobsMock.mockReset()
  getJobByIdMock.mockReset()
  getJobSnapshotsMock.mockReset()
  getJobSnapshotsMock.mockResolvedValue([])
  getJobsMock.mockResolvedValue({
    page: 1,
    size: 20,
    total: 0,
    totalPages: 0,
    items: [],
  })
  getJobByIdMock.mockResolvedValue({
    id: 7,
    source: 'BOSS',
    sourceItemId: 'source-7',
    sourceUrl: null,
    title: 'Java developer',
    content: 'Job description',
    publishTime: null,
    collectedAt: '2026-07-28T01:00:00Z',
    firstSeenTime: '2026-07-28T01:00:00Z',
    lastSeenTime: '2026-07-28T02:00:00Z',
    currentVersionNo: 1,
    collectorId: 'collector',
    collectorVersion: '1.0',
    sourceCompanyId: null,
    sourceRecruiterId: null,
    companyName: 'Example company',
    companyUrl: null,
    companyScaleText: null,
    companyStageText: null,
    companyIndustryText: null,
    salaryText: null,
    salarySource: null,
    salaryMinMonthlyYuan: null,
    salaryMaxMonthlyYuan: null,
    salaryMonths: null,
    locationName: null,
    cityName: null,
    areaName: null,
    businessDistrictName: null,
    experienceText: null,
    educationText: null,
    recruiterName: null,
    recruiterTitle: null,
    recruiterActiveText: null,
    remoteType: 'UNKNOWN',
    jobStatus: 'ACTIVE',
    detailStatus: 'FETCHED',
    detailCollectedAt: null,
    sourceTags: null,
    sourceSkillTags: null,
    welfare: null,
  })
})

async function mountAt(path: string) {
  setAuthenticatedUserForTest(testUser)
  const router = createAppRouter(
    createMemoryHistory(),
    async () => testUser,
  )
  await router.push(path)
  await router.isReady()

  const wrapper = mount(App, {
    global: {
      plugins: [router],
    },
  })
  await flushPromises()

  return { router, wrapper }
}

describe('application routing', () => {
  it('renders the jobs list at /jobs', async () => {
    const { wrapper } = await mountAt('/jobs')

    expect(wrapper.text()).toContain('职位浏览')
    expect(wrapper.text()).toContain('筛选职位')
  })

  it('redirects the root path to /jobs', async () => {
    const { router, wrapper } = await mountAt('/')

    expect(router.currentRoute.value.fullPath).toBe('/jobs')
    expect(wrapper.text()).toContain('职位浏览')
  })

  it('renders the real detail page and loads its route id', async () => {
    const { wrapper } = await mountAt(
      '/jobs/7?from=%2Fjobs%3Fkeyword%3DJava',
    )

    expect(wrapper.text()).toContain('Java developer')
    expect(wrapper.text()).toContain('Job description')
    expect(getJobByIdMock).toHaveBeenCalledWith(
      7,
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    )
    expect(getJobsMock).not.toHaveBeenCalled()
  })

  it('renders the real snapshots page and loads its route id', async () => {
    const { wrapper } = await mountAt(
      '/jobs/7/snapshots?from=%2Fjobs%3Fkeyword%3DJava',
    )

    expect(wrapper.text()).toContain('历史快照')
    expect(wrapper.text()).toContain('当前职位暂无历史版本')
    expect(getJobSnapshotsMock).toHaveBeenCalledWith(
      7,
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    )
    expect(getJobsMock).not.toHaveBeenCalled()
    expect(getJobByIdMock).not.toHaveBeenCalled()
  })

  it('renders the 404 placeholder for an unknown path', async () => {
    const { wrapper } = await mountAt('/missing-page')

    expect(wrapper.text()).toContain('404')
    expect(wrapper.text()).toContain('未找到请求的页面')
  })
})
