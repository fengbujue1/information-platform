import { csrfHeaders, unwrapResponse } from '@/api/apiSupport'
import { httpClient } from '@/api/httpClient'
import type { ApiRequestOptions, ApiResponse } from '@/types/api'
import type {
  JobDisposition,
  ManualRecommendationRefresh,
  RecommendationFeed,
  RecommendationFeedbackState,
  RecommendationInformationType,
  RecommendationInteraction,
  RecommendationProfile,
  RecommendationProfileRequest,
  RecommendationRun,
} from '@/types/recommendation'

const JOB: RecommendationInformationType = 'JOB'

export function getRecommendationProfile(
  options: ApiRequestOptions = {},
): Promise<RecommendationProfile> {
  return unwrapResponse(
    httpClient.get<ApiResponse<RecommendationProfile>>(
      `/v1/recommendation/profiles/${JOB}`,
      { signal: options.signal },
    ),
  )
}

export async function saveRecommendationProfile(
  request: RecommendationProfileRequest,
): Promise<RecommendationProfile> {
  return unwrapResponse(
    httpClient.put<ApiResponse<RecommendationProfile>>(
      `/v1/recommendation/profiles/${JOB}`,
      request,
      { headers: await csrfHeaders() },
    ),
  )
}

export function getRecommendationFeed(
  page: number,
  pageSize: number,
  options: ApiRequestOptions = {},
): Promise<RecommendationFeed> {
  return unwrapResponse(
    httpClient.get<ApiResponse<RecommendationFeed>>(
      `/v1/recommendations/${JOB}/feed`,
      { params: { page, pageSize }, signal: options.signal },
    ),
  )
}

export async function refreshRecommendations(): Promise<ManualRecommendationRefresh> {
  return unwrapResponse(
    httpClient.post<ApiResponse<ManualRecommendationRefresh>>(
      `/v1/recommendations/${JOB}/refresh`,
      undefined,
      { headers: await csrfHeaders() },
    ),
  )
}

export function getRecommendationRun(
  runId: number,
  options: ApiRequestOptions = {},
): Promise<RecommendationRun> {
  return unwrapResponse(
    httpClient.get<ApiResponse<RecommendationRun>>(
      `/v1/recommendations/${JOB}/runs/${runId}`,
      { signal: options.signal },
    ),
  )
}

export function listRecommendationRuns(
  limit = 20,
  options: ApiRequestOptions = {},
): Promise<RecommendationRun[]> {
  return unwrapResponse(
    httpClient.get<ApiResponse<RecommendationRun[]>>(
      `/v1/recommendations/${JOB}/runs`,
      { params: { limit }, signal: options.signal },
    ),
  )
}

export async function recordRecommendationView(
  informationId: number,
  recommendationItemId: number,
): Promise<RecommendationInteraction> {
  return unwrapResponse(
    httpClient.post<ApiResponse<RecommendationInteraction>>(
      `/v1/recommendation/interactions/${informationId}/view`,
      { recommendationItemId },
      { headers: await csrfHeaders() },
    ),
  )
}

export async function updateRecommendationFeedback(
  informationId: number,
  recommendationItemId: number,
  feedbackState: RecommendationFeedbackState,
): Promise<RecommendationInteraction> {
  return unwrapResponse(
    httpClient.put<ApiResponse<RecommendationInteraction>>(
      `/v1/recommendation/interactions/${informationId}/feedback`,
      { feedbackState, recommendationItemId },
      { headers: await csrfHeaders() },
    ),
  )
}

export async function updateJobDisposition(
  informationId: number,
  recommendationItemId: number,
  jobDisposition: JobDisposition,
): Promise<RecommendationInteraction> {
  return unwrapResponse(
    httpClient.put<ApiResponse<RecommendationInteraction>>(
      `/v1/recommendation/interactions/${informationId}/job-disposition`,
      { jobDisposition, recommendationItemId },
      { headers: await csrfHeaders() },
    ),
  )
}
