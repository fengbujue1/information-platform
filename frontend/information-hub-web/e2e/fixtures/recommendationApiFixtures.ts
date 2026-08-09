import type { Page, Route } from '@playwright/test'

import type {
  JobDisposition,
  RecommendationFeed,
  RecommendationFeedbackState,
  RecommendationRun,
} from '../../src/types/recommendation'

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

export interface RecommendationApiMockState {
  profileSaveCount: number
  refreshCount: number
  interactionCsrfHeaders: string[]
  failFeed: boolean
}

const profile = {
  id: 7,
  informationType: 'JOB',
  analysisPromptProfileId: 12,
  windowDays: 7,
  topN: 50,
  targetRoles: ['Java 后端'],
  preferredSkills: ['Java', 'Spring Boot'],
  preferredCities: ['成都'],
  preferredRemoteTypes: ['REMOTE'],
  salaryMinMonthlyYuan: 15_000,
  excludedKeywords: ['纯销售'],
  contentHash: 'a'.repeat(64),
  createdAt: '2026-08-09T01:00:00',
  updatedAt: '2026-08-09T01:00:00',
}

const initialFeed: RecommendationFeed = {
  run: {
    id: 31,
    informationType: 'JOB',
    triggerType: 'ANALYSIS_BATCH_COMPLETED',
    completedAt: '2026-08-09T02:00:00Z',
    algorithmKey: 'JOB_RECOMMENDATION',
    algorithmVersion: 1,
    profileChangedSinceRun: true,
  },
  page: 1,
  pageSize: 20,
  total: 2,
  items: [
    {
      recommendationItemId: 1001,
      informationId: 88,
      snapshotId: 102,
      rank: 1,
      finalScore: 92.4,
      scoreBreakdown: {
        aiRelevanceScore: 95,
        profileMatchScore: 86,
        freshnessScore: 90,
      },
      reasons: ['AI 相关度高', '支持远程办公'],
      feedbackState: 'NONE',
      jobDisposition: 'NONE',
      viewed: false,
      job: {
        title: 'Java Backend Engineer',
        companyName: 'Example',
        salaryText: '20-35K',
        locationName: '成都',
        remoteType: 'REMOTE',
        sourceUrl: 'https://example.test/jobs/88',
      },
    },
    {
      recommendationItemId: 1002,
      informationId: 89,
      snapshotId: 103,
      rank: 2,
      finalScore: 88,
      scoreBreakdown: {
        aiRelevanceScore: 90,
        profileMatchScore: 80,
        freshnessScore: 85,
      },
      reasons: ['符合目标岗位偏好'],
      feedbackState: 'INTERESTED',
      jobDisposition: 'CONTACTED',
      viewed: true,
      job: {
        title: 'Spring Boot Engineer',
        companyName: 'Contacted Co',
        salaryText: '18-30K',
        locationName: '成都',
        remoteType: 'HYBRID',
        sourceUrl: 'https://example.test/jobs/89',
      },
    },
  ],
}

function completedRun(): RecommendationRun {
  return {
    id: 32,
    informationType: 'JOB',
    triggerType: 'MANUAL',
    sourceAnalysisBatchId: null,
    profileId: 7,
    profileContentHash: profile.contentHash,
    promptProfileId: 12,
    promptVersionId: 22,
    algorithmKey: 'JOB_RECOMMENDATION',
    algorithmVersion: 1,
    windowStart: '2026-08-02T02:00:00Z',
    windowEnd: '2026-08-09T02:00:00Z',
    candidateCount: 2,
    eligibleCount: 2,
    resultCount: 2,
    status: 'COMPLETED',
    skipReason: null,
    failureCode: null,
    failureMessage: null,
    startedAt: '2026-08-09T02:00:00Z',
    completedAt: '2026-08-09T02:00:01Z',
    createdAt: '2026-08-09T02:00:00Z',
    updatedAt: '2026-08-09T02:00:01Z',
  }
}

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

export async function installRecommendationApiMock(
  page: Page,
): Promise<RecommendationApiMockState> {
  const state: RecommendationApiMockState = {
    profileSaveCount: 0,
    refreshCount: 0,
    interactionCsrfHeaders: [],
    failFeed: false,
  }
  const feedback = new Map<number, RecommendationFeedbackState>()
  const dispositions = new Map<number, JobDisposition>([[89, 'CONTACTED']])
  let runPollCount = 0

  await page.route('**/api/v1/auth/**', async (route) => {
    const path = new URL(route.request().url()).pathname
    if (path === '/api/v1/auth/me') {
      await fulfillJson(route, 200, success('CURRENT_USER_FOUND', {
        id: 1,
        username: 'admin',
        displayName: 'Admin',
        timezone: 'Asia/Shanghai',
      }))
      return
    }
    if (path === '/api/v1/auth/csrf') {
      await fulfillJson(route, 200, success('CSRF_TOKEN_CREATED', {
        headerName: 'X-CSRF-TOKEN',
        parameterName: '_csrf',
        token: 'recommendation-e2e-token',
      }))
      return
    }
    await route.abort()
  })

  await page.route('**/api/v1/ai/prompt-profiles', async (route) => {
    await fulfillJson(route, 200, success('PROMPT_PROFILES_FOUND', [{
      id: 12,
      name: 'JOB relevance',
      analysisDefinitionKey: 'JOB_USER_RELEVANCE',
      activeVersionId: 22,
      status: 'ACTIVE',
      createdAt: '2026-08-01T00:00:00Z',
      updatedAt: '2026-08-01T00:00:00Z',
    }]))
  })

  await page.route('**/api/v1/recommendation/profiles/JOB', async (route) => {
    if (route.request().method() === 'PUT') state.profileSaveCount += 1
    await fulfillJson(route, 200, success('RECOMMENDATION_PROFILE_FOUND', profile))
  })

  await page.route('**/api/v1/recommendations/JOB/**', async (route) => {
    const path = new URL(route.request().url()).pathname
    if (path.endsWith('/feed')) {
      if (state.failFeed) {
        await fulfillJson(route, 500, {
          success: false,
          code: 'RECOMMENDATION_FEED_PERSISTENCE_FAILED',
          message: 'fixture failure',
        })
        return
      }
      const visibleItems = initialFeed.items
        .filter((item) => feedback.get(item.informationId) !== 'NOT_INTERESTED')
        .filter((item) => dispositions.get(item.informationId) !== 'CONTACTED_NOT_SUITABLE')
        .map((item) => ({
          ...item,
          feedbackState: feedback.get(item.informationId) ?? item.feedbackState,
          jobDisposition: dispositions.get(item.informationId) ?? item.jobDisposition,
        }))
      await fulfillJson(route, 200, success('RECOMMENDATION_FEED_FOUND', {
        ...initialFeed,
        total: visibleItems.length,
        items: visibleItems,
      }))
      return
    }
    if (path.endsWith('/refresh')) {
      state.refreshCount += 1
      await fulfillJson(route, 202, success('RECOMMENDATION_RUN_ACCEPTED', {
        runId: 32,
        status: 'PENDING',
      }))
      return
    }
    if (path.endsWith('/runs/32')) {
      runPollCount += 1
      const run = completedRun()
      if (runPollCount === 1) run.status = 'RUNNING'
      await fulfillJson(route, 200, success('RECOMMENDATION_RUN_FOUND', run))
      return
    }
    if (path.endsWith('/runs')) {
      await fulfillJson(route, 200, success('RECOMMENDATION_RUNS_FOUND', []))
      return
    }
    await route.abort()
  })

  await page.route('**/api/v1/recommendation/interactions/**', async (route) => {
    const path = new URL(route.request().url()).pathname
    const informationId = Number(path.split('/')[5])
    state.interactionCsrfHeaders.push(
      route.request().headers()['x-csrf-token'] ?? '',
    )
    const body = route.request().postDataJSON() as {
      feedbackState?: RecommendationFeedbackState
      jobDisposition?: JobDisposition
    }
    if (path.endsWith('/feedback') && body.feedbackState) {
      feedback.set(informationId, body.feedbackState)
    }
    if (path.endsWith('/job-disposition') && body.jobDisposition) {
      dispositions.set(informationId, body.jobDisposition)
    }
    await fulfillJson(route, 200, success('RECOMMENDATION_INTERACTION_UPDATED', {
      informationId,
      viewCount: path.endsWith('/view') ? 1 : 0,
      lastViewedAt: null,
      feedbackState: feedback.get(informationId) ?? 'NONE',
      feedbackUpdatedAt: null,
      jobDisposition: dispositions.get(informationId) ?? 'NONE',
      dispositionUpdatedAt: null,
      lastRecommendationItemId: informationId === 88 ? 1001 : 1002,
      updatedAt: '2026-08-09T03:00:00',
    }))
  })

  return state
}
