import MockAdapter from 'axios-mock-adapter'
import { afterAll, afterEach, describe, expect, it } from 'vitest'

import { ApiClientError } from '@/api/apiError'
import { httpClient } from '@/api/httpClient'
import {
  getJobById,
  getJobs,
  getJobSnapshots,
} from '@/api/jobApi'
import type {
  ApiFailureResponse,
  ApiSuccessResponse,
  PageResponse,
} from '@/types/api'
import type {
  JobDetail,
  JobListItem,
  JobSnapshot,
} from '@/types/job'

const listItem: JobListItem = {
  id: 7,
  source: 'BOSS',
  sourceItemId: 'source-7',
  sourceUrl: 'https://example.test/jobs/7',
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
  remoteType: 'REMOTE',
  jobStatus: 'ACTIVE',
  publishTime: null,
  firstSeenTime: '2026-07-28T01:00:00Z',
  lastSeenTime: '2026-07-28T02:00:00Z',
  currentVersionNo: 2,
}

const detail: JobDetail = {
  ...listItem,
  content: 'Job description',
  collectedAt: '2026-07-28T01:00:00Z',
  collectorId: 'collector',
  collectorVersion: '1.0',
  sourceCompanyId: 'company-1',
  sourceRecruiterId: 'recruiter-1',
  companyUrl: null,
  companyScaleText: null,
  companyStageText: null,
  companyIndustryText: null,
  salarySource: 'SOURCE',
  areaName: '武侯区',
  businessDistrictName: null,
  recruiterName: null,
  recruiterTitle: 'HR',
  recruiterActiveText: null,
  detailStatus: 'FETCHED',
  detailCollectedAt: null,
  sourceTags: null,
  sourceSkillTags: null,
  welfare: null,
}

const snapshot: JobSnapshot = {
  id: 2,
  versionNo: 2,
  contentHash: 'a'.repeat(64),
  title: 'Java developer',
  content: 'Job description',
  standardizedPayload: {
    companyName: 'Example company',
  },
  collectedAt: '2026-07-28T01:00:00Z',
  collectorId: 'collector',
  collectorVersion: '1.0',
  createdAt: '2026-07-28T02:00:00Z',
}

const mock = new MockAdapter(httpClient)

afterEach(() => {
  mock.reset()
})

afterAll(() => {
  mock.restore()
})

describe('job API client', () => {
  it('returns a typed job page and omits empty query parameters', async () => {
    const response: ApiSuccessResponse<PageResponse<JobListItem>> = {
      success: true,
      code: 'JOBS_FOUND',
      data: {
        page: 1,
        size: 20,
        total: 1,
        totalPages: 1,
        items: [listItem],
      },
    }

    mock.onGet('/v1/jobs').reply((config) => {
      expect(config.baseURL).toBe('/api')
      expect(config.params).toEqual({
        page: 1,
        keyword: 'Java',
        salaryMin: 0,
        sortBy: 'firstSeenTime',
      })
      return [200, response]
    })

    const page = await getJobs({
      page: 1,
      size: null,
      keyword: '  Java  ',
      company: '   ',
      salaryMin: 0,
      source: null,
      sortBy: 'firstSeenTime',
    })

    expect(page.items).toEqual([listItem])
    expect(page.total).toBe(1)
  })

  it('returns the current job detail', async () => {
    const response: ApiSuccessResponse<JobDetail> = {
      success: true,
      code: 'JOB_FOUND',
      data: detail,
    }
    mock.onGet('/v1/jobs/7').reply(200, response)

    await expect(getJobById(7)).resolves.toEqual(detail)
  })

  it('returns job snapshots in the backend response order', async () => {
    const response: ApiSuccessResponse<JobSnapshot[]> = {
      success: true,
      code: 'JOB_SNAPSHOTS_FOUND',
      data: [snapshot],
    }
    mock.onGet('/v1/jobs/7/snapshots').reply(200, response)

    await expect(getJobSnapshots(7)).resolves.toEqual([snapshot])
  })

  it('converts a missing job response to a stable client error', async () => {
    const response: ApiFailureResponse = {
      success: false,
      code: 'JOB_NOT_FOUND',
      message: 'Job does not exist',
    }
    mock.onGet('/v1/jobs/404').reply(404, response)

    await expect(getJobById(404)).rejects.toMatchObject({
      name: 'ApiClientError',
      kind: 'business',
      code: 'JOB_NOT_FOUND',
      message: '职位不存在或已不可用',
      status: 404,
    })
  })

  it('maps a backend parameter error without exposing Axios internals', async () => {
    const response: ApiFailureResponse = {
      success: false,
      code: 'INVALID_JOB_PAGE_SIZE',
      message: 'size must be between 1 and 100',
    }
    mock.onGet('/v1/jobs').reply(400, response)

    const error = await getJobs({ size: 101 }).catch(
      (reason: unknown) => reason,
    )

    expect(error).toBeInstanceOf(ApiClientError)
    expect(error).toMatchObject({
      kind: 'business',
      code: 'INVALID_JOB_PAGE_SIZE',
      message: '每页数量必须在 1 到 100 之间',
      status: 400,
    })
    expect(error).not.toHaveProperty('config')
    expect(error).not.toHaveProperty('request')
    expect(error).not.toHaveProperty('response')
  })

  it('maps a network failure to a retryable network error', async () => {
    mock.onGet('/v1/jobs/7/snapshots').networkError()

    await expect(getJobSnapshots(7)).rejects.toMatchObject({
      name: 'ApiClientError',
      kind: 'network',
      code: 'NETWORK_ERROR',
      message: '无法连接到服务，请检查网络后重试',
      status: null,
    })
  })

  it('rejects an unrecognized success payload as a protocol error', async () => {
    mock.onGet('/v1/jobs').reply(200, {
      code: 'JOBS_FOUND',
      data: {},
    })

    await expect(getJobs()).rejects.toMatchObject({
      kind: 'protocol',
      code: 'INVALID_API_RESPONSE',
      message: '服务返回了无法识别的响应',
    })
  })
})
