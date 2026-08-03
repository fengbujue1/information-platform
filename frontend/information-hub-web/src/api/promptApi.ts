import type { AxiosResponse } from 'axios'

import {
  createBusinessApiError,
  createInvalidApiResponseError,
} from '@/api/apiError'
import { getCsrfToken } from '@/api/authApi'
import { httpClient } from '@/api/httpClient'
import type { ApiResponse } from '@/types/api'
import {
  isApiFailureResponse,
  isApiSuccessResponse,
} from '@/types/api'
import type {
  CreatePromptProfileRequest,
  CreatePromptVersionRequest,
  PromptProfile,
  PromptProfileStatus,
  PromptVersion,
} from '@/types/prompt'

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

async function csrfHeaders(): Promise<Record<string, string>> {
  const csrf = await getCsrfToken()
  return { [csrf.headerName]: csrf.token }
}

export function listPromptProfiles(): Promise<PromptProfile[]> {
  return unwrapResponse(
    httpClient.get<ApiResponse<PromptProfile[]>>(
      '/v1/ai/prompt-profiles',
    ),
  )
}

export async function createPromptProfile(
  request: CreatePromptProfileRequest,
): Promise<PromptProfile> {
  return unwrapResponse(
    httpClient.post<ApiResponse<PromptProfile>>(
      '/v1/ai/prompt-profiles',
      request,
      { headers: await csrfHeaders() },
    ),
  )
}

export function getPromptProfile(
  profileId: number,
): Promise<PromptProfile> {
  return unwrapResponse(
    httpClient.get<ApiResponse<PromptProfile>>(
      `/v1/ai/prompt-profiles/${profileId}`,
    ),
  )
}

export async function createPromptVersion(
  profileId: number,
  request: CreatePromptVersionRequest,
): Promise<PromptVersion> {
  return unwrapResponse(
    httpClient.post<ApiResponse<PromptVersion>>(
      `/v1/ai/prompt-profiles/${profileId}/versions`,
      request,
      { headers: await csrfHeaders() },
    ),
  )
}

export function listPromptVersions(
  profileId: number,
): Promise<PromptVersion[]> {
  return unwrapResponse(
    httpClient.get<ApiResponse<PromptVersion[]>>(
      `/v1/ai/prompt-profiles/${profileId}/versions`,
    ),
  )
}

export async function activatePromptVersion(
  profileId: number,
  versionId: number,
): Promise<PromptProfile> {
  return unwrapResponse(
    httpClient.put<ApiResponse<PromptProfile>>(
      `/v1/ai/prompt-profiles/${profileId}/active-version`,
      { versionId },
      { headers: await csrfHeaders() },
    ),
  )
}

export async function updatePromptProfileStatus(
  profileId: number,
  status: PromptProfileStatus,
): Promise<PromptProfile> {
  return unwrapResponse(
    httpClient.put<ApiResponse<PromptProfile>>(
      `/v1/ai/prompt-profiles/${profileId}/status`,
      { status },
      { headers: await csrfHeaders() },
    ),
  )
}
