import type { Page, Route } from '@playwright/test'

import type {
  JobDetail,
  JobListItem,
  JobSnapshot,
} from '../../src/types/job'

interface SuccessResponse<T> {
  success: true
  code: string
  data: T
}

interface FailureResponse {
  success: false
  code: string
  message: string
}

interface PageResponse<T> {
  page: number
  size: number
  total: number
  totalPages: number
  items: T[]
}

export interface JobApiMockState {
  listRequests: URL[]
  detailRequests: URL[]
  snapshotRequests: URL[]
}

const firstJob: JobListItem = {
  id: 1001,
  source: 'BOSS',
  sourceItemId: 'boss-job-1001',
  sourceUrl: 'https://example.test/jobs/1001',
  title: '高级 Java 工程师',
  companyName: '示例科技',
  salaryText: '25-35K·14薪',
  salaryMinMonthlyYuan: 25_000,
  salaryMaxMonthlyYuan: 35_000,
  salaryMonths: 14,
  locationName: '上海·浦东新区',
  cityName: '上海',
  experienceText: '5-10年',
  educationText: '本科',
  remoteType: 'ONSITE',
  jobStatus: 'ACTIVE',
  publishTime: '2026-07-28T02:00:00Z',
  firstSeenTime: '2026-07-28T08:00:00Z',
  lastSeenTime: '2026-07-30T08:00:00Z',
  currentVersionNo: 2,
}

const secondJob: JobListItem = {
  ...firstJob,
  id: 1002,
  sourceItemId: 'boss-job-1002',
  title: '平台开发工程师',
  companyName: '云端信息',
  salaryText: '20-30K',
  salaryMinMonthlyYuan: 20_000,
  salaryMaxMonthlyYuan: 30_000,
  currentVersionNo: 1,
}

const pageTwoJob: JobListItem = {
  ...firstJob,
  id: 1003,
  sourceItemId: 'boss-job-1003',
  title: '第二页测试职位',
  companyName: '分页示例公司',
  currentVersionNo: 1,
}

const jobDetail: JobDetail = {
  ...firstJob,
  content: '负责 Information Platform 后端开发。\n维护稳定的数据接入链路。',
  collectedAt: '2026-07-30T08:00:00Z',
  collectorId: 'boss-zhipin-scraper',
  collectorVersion: '1.0.0',
  sourceCompanyId: 'company-1001',
  sourceRecruiterId: 'recruiter-1001',
  companyUrl: 'https://example.test/companies/1001',
  companyScaleText: '100-499人',
  companyStageText: 'B轮',
  companyIndustryText: '互联网',
  salarySource: 'DETAIL',
  areaName: '浦东新区',
  businessDistrictName: '张江',
  recruiterName: '招聘负责人',
  recruiterTitle: '技术招聘',
  recruiterActiveText: '2026-07-30T07:30:00Z',
  detailStatus: 'FETCHED',
  detailCollectedAt: '2026-07-30T08:00:00Z',
  sourceTags: ['5-10年', '本科'],
  sourceSkillTags: ['Java', 'Spring Boot'],
  welfare: ['五险一金', '带薪年假'],
}

const snapshots: JobSnapshot[] = [
  {
    id: 2002,
    versionNo: 2,
    contentHash: 'hash-version-2',
    title: '高级 Java 工程师',
    content: '第二版职位描述',
    standardizedPayload: {
      title: '高级 Java 工程师',
      jobStatus: 'ACTIVE',
    },
    collectedAt: '2026-07-30T08:00:00Z',
    collectorId: 'boss-zhipin-scraper',
    collectorVersion: '1.0.0',
    createdAt: '2026-07-30T08:01:00Z',
  },
  {
    id: 2001,
    versionNo: 1,
    contentHash: 'hash-version-1',
    title: 'Java 工程师',
    content: '第一版职位描述',
    standardizedPayload: {
      title: 'Java 工程师',
      jobStatus: 'ACTIVE',
    },
    collectedAt: '2026-07-28T08:00:00Z',
    collectorId: 'boss-zhipin-scraper',
    collectorVersion: '0.9.0',
    createdAt: '2026-07-28T08:01:00Z',
  },
]

function success<T>(code: string, data: T): SuccessResponse<T> {
  return { success: true, code, data }
}

async function fulfillJson(
  route: Route,
  status: number,
  body: SuccessResponse<unknown> | FailureResponse,
): Promise<void> {
  await route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  })
}

function createListResponse(url: URL): PageResponse<JobListItem> {
  const page = Number(url.searchParams.get('page') ?? '1')
  const size = Number(url.searchParams.get('size') ?? '20')
  const keyword = url.searchParams.get('keyword')

  if (keyword === '空结果') {
    return {
      page,
      size,
      total: 0,
      totalPages: 0,
      items: [],
    }
  }

  if (page === 2) {
    return {
      page,
      size,
      total: 21,
      totalPages: 2,
      items: [pageTwoJob],
    }
  }

  const items = keyword === '平台' ? [secondJob] : [firstJob, secondJob]
  return {
    page,
    size,
    total: 21,
    totalPages: 2,
    items,
  }
}

/**
 * 拦截所有 Job Query API 请求，确保浏览器 E2E 不依赖远程数据库或真实职位顺序。
 */
export async function installJobApiMock(
  page: Page,
): Promise<JobApiMockState> {
  const state: JobApiMockState = {
    listRequests: [],
    detailRequests: [],
    snapshotRequests: [],
  }

  await page.route('**/api/v1/auth/**', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname === '/api/v1/auth/me') {
      await fulfillJson(
        route,
        200,
        success('CURRENT_USER_FOUND', {
          id: 1,
          username: 'admin',
          displayName: 'Admin',
          timezone: 'Asia/Shanghai',
        }),
      )
      return
    }
    if (url.pathname === '/api/v1/auth/csrf') {
      await fulfillJson(
        route,
        200,
        success('CSRF_TOKEN_CREATED', {
          headerName: 'X-CSRF-TOKEN',
          parameterName: '_csrf',
          token: 'e2e-csrf-token',
        }),
      )
      return
    }
    await fulfillJson(route, 404, {
      success: false,
      code: 'AUTH_ROUTE_NOT_MOCKED',
      message: 'auth route not mocked',
    })
  })

  await page.route('**/api/v1/jobs**', async (route) => {
    const url = new URL(route.request().url())

    if (url.pathname === '/api/v1/jobs') {
      state.listRequests.push(url)
      if (url.searchParams.get('keyword') === '接口错误') {
        await fulfillJson(route, 503, {
          success: false,
          code: 'SERVICE_UNAVAILABLE',
          message: 'fixture service unavailable',
        })
        return
      }

      await fulfillJson(
        route,
        200,
        success('JOBS_FOUND', createListResponse(url)),
      )
      return
    }

    if (url.pathname === '/api/v1/jobs/1001/snapshots') {
      state.snapshotRequests.push(url)
      await fulfillJson(route, 200, success('JOB_SNAPSHOTS_FOUND', snapshots))
      return
    }

    if (url.pathname === '/api/v1/jobs/1001') {
      state.detailRequests.push(url)
      await fulfillJson(route, 200, success('JOB_FOUND', jobDetail))
      return
    }

    await fulfillJson(route, 404, {
      success: false,
      code: 'JOB_NOT_FOUND',
      message: 'job not found',
    })
  })

  return state
}
