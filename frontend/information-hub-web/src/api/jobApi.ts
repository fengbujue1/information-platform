import type { AxiosResponse } from 'axios'

import {
  createBusinessApiError,
  createInvalidApiResponseError,
} from '@/api/apiError'
import { httpClient } from '@/api/httpClient'
import type {
  ApiRequestOptions,
  ApiResponse,
  PageResponse,
} from '@/types/api'
import {
  isApiFailureResponse,
  isApiSuccessResponse,
} from '@/types/api'
import type {
  JobDetail,
  JobListItem,
  JobQueryParams,
  JobSnapshot,
} from '@/types/job'

type QueryParameterValue = string | number
type RequestQueryParameters = Record<string, QueryParameterValue>

function addNumberParameter(
  target: RequestQueryParameters,
  key: string,
  value: number | null | undefined,
): void {
  if (typeof value === 'number' && Number.isFinite(value)) {
    target[key] = value
  }
}

function addTextParameter(
  target: RequestQueryParameters,
  key: string,
  value: string | null | undefined,
): void {
  const normalized = value?.trim()
  if (normalized) {
    target[key] = normalized
  }
}

export function buildJobQueryParameters(
  params: JobQueryParams,
): RequestQueryParameters {
  const result: RequestQueryParameters = {}

  addNumberParameter(result, 'page', params.page)
  addNumberParameter(result, 'size', params.size)
  addTextParameter(result, 'keyword', params.keyword)
  addTextParameter(result, 'company', params.company)
  addTextParameter(result, 'city', params.city)
  addNumberParameter(result, 'salaryMin', params.salaryMin)
  addNumberParameter(result, 'salaryMax', params.salaryMax)
  addTextParameter(result, 'source', params.source)
  addTextParameter(result, 'jobStatus', params.jobStatus)
  addTextParameter(result, 'remoteType', params.remoteType)
  addTextParameter(result, 'sortBy', params.sortBy)
  addTextParameter(result, 'sortDirection', params.sortDirection)

  return result
}

async function unwrapResponse<T>(
  request: Promise<AxiosResponse<ApiResponse<T>>>,
): Promise<T> {
  const response = await request
  if (isApiSuccessResponse<T>(response.data)) {
    return response.data.data
  }
  if (isApiFailureResponse(response.data)) {
    throw createBusinessApiError(response.data, response.status)
  }
  throw createInvalidApiResponseError()
}

export function getJobs(
  params: JobQueryParams = {},
  options: ApiRequestOptions = {},
): Promise<PageResponse<JobListItem>> {
  return unwrapResponse(
    httpClient.get<ApiResponse<PageResponse<JobListItem>>>('/v1/jobs', {
      params: buildJobQueryParameters(params),
      signal: options.signal,
    }),
  )
}

export function getJobById(
  id: number,
  options: ApiRequestOptions = {},
): Promise<JobDetail> {
  return unwrapResponse(
    httpClient.get<ApiResponse<JobDetail>>(`/v1/jobs/${id}`, {
      signal: options.signal,
    }),
  )
}

export function getJobSnapshots(
  id: number,
  options: ApiRequestOptions = {},
): Promise<JobSnapshot[]> {
  return unwrapResponse(
    httpClient.get<ApiResponse<JobSnapshot[]>>(`/v1/jobs/${id}/snapshots`, {
      signal: options.signal,
    }),
  )
}
