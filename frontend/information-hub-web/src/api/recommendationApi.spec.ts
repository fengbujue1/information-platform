import { AxiosHeaders } from 'axios'
import MockAdapter from 'axios-mock-adapter'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'

import { clearCsrfToken } from '@/api/authApi'
import { httpClient } from '@/api/httpClient'
import {
  getRecommendationFeed,
  getRecommendationProfile,
  refreshRecommendations,
  saveRecommendationProfile,
  updateJobDisposition,
  updateRecommendationFeedback,
} from '@/api/recommendationApi'

const mock = new MockAdapter(httpClient)

function success<T>(code: string, data: T) {
  return { success: true, code, data }
}

function csrfHeader(headers: unknown): string | undefined {
  if (headers instanceof AxiosHeaders) {
    const value = headers.get('X-CSRF-TOKEN')
    return typeof value === 'string' ? value : undefined
  }
  if (typeof headers === 'object' && headers !== null) {
    const value = (headers as Record<string, unknown>)['X-CSRF-TOKEN']
    return typeof value === 'string' ? value : undefined
  }
  return undefined
}

beforeEach(() => {
  clearCsrfToken()
  mock.reset()
  mock.onGet('/v1/auth/csrf').reply(200, success('CSRF_TOKEN_CREATED', {
    headerName: 'X-CSRF-TOKEN',
    parameterName: '_csrf',
    token: 'recommendation-test-token',
  }))
})

afterEach(() => mock.reset())

describe('recommendationApi', () => {
  it('reads the JOB profile and paginated precomputed feed', async () => {
    mock.onGet('/v1/recommendation/profiles/JOB').reply(200, success(
      'RECOMMENDATION_PROFILE_FOUND',
      { id: 1, informationType: 'JOB' },
    ))
    mock.onGet('/v1/recommendations/JOB/feed', {
      params: { page: 2, pageSize: 20 },
    }).reply(200, success('RECOMMENDATION_FEED_FOUND', {
      run: null,
      page: 2,
      pageSize: 20,
      total: 0,
      items: [],
    }))

    await expect(getRecommendationProfile()).resolves.toMatchObject({ id: 1 })
    await expect(getRecommendationFeed(2, 20)).resolves.toMatchObject({
      page: 2,
      total: 0,
    })
  })

  it('adds CSRF to profile, refresh, feedback and disposition writes', async () => {
    const requests: Array<{ url?: string; csrf?: string }> = []
    mock.onAny().reply((config) => {
      if (config.url === '/v1/auth/csrf') {
        return [200, success('CSRF_TOKEN_CREATED', {
          headerName: 'X-CSRF-TOKEN',
          parameterName: '_csrf',
          token: 'recommendation-test-token',
        })]
      }
      requests.push({
        url: config.url,
        csrf: csrfHeader(config.headers),
      })
      if (config.url?.endsWith('/refresh')) {
        return [202, success('RECOMMENDATION_RUN_ACCEPTED', { runId: 31, status: 'PENDING' })]
      }
      if (config.url?.endsWith('/feedback') || config.url?.endsWith('/job-disposition')) {
        return [200, success('RECOMMENDATION_INTERACTION_UPDATED', {
          informationId: 88,
          feedbackState: 'INTERESTED',
          jobDisposition: 'CONTACTED',
        })]
      }
      return [200, success('RECOMMENDATION_PROFILE_UPDATED', { id: 1 })]
    })

    await saveRecommendationProfile({
      analysisPromptProfileId: 12,
      windowDays: 7,
      topN: 50,
      targetRoles: ['Java 后端'],
      preferredSkills: ['Java'],
      preferredCities: ['成都'],
      preferredRemoteTypes: ['REMOTE'],
      salaryMinMonthlyYuan: 15_000,
      excludedKeywords: ['纯销售'],
    })
    await refreshRecommendations()
    await updateRecommendationFeedback(88, 1001, 'INTERESTED')
    await updateJobDisposition(88, 1001, 'CONTACTED')

    expect(requests).toHaveLength(4)
    expect(requests.every((request) => request.csrf === 'recommendation-test-token')).toBe(true)
  })
})
