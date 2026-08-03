import type { AxiosResponse } from 'axios'

import {
  createBusinessApiError,
  createInvalidApiResponseError,
} from '@/api/apiError'
import { httpClient } from '@/api/httpClient'
import type { ApiResponse } from '@/types/api'
import {
  isApiFailureResponse,
  isApiSuccessResponse,
} from '@/types/api'
import type {
  CsrfToken,
  CurrentUser,
  LoginRequest,
} from '@/types/identity'

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

let csrfToken: CsrfToken | null = null

export async function getCsrfToken(
  forceRefresh = false,
): Promise<CsrfToken> {
  if (!forceRefresh && csrfToken) {
    return csrfToken
  }
  csrfToken = await unwrapResponse(
    httpClient.get<ApiResponse<CsrfToken>>('/v1/auth/csrf'),
  )
  return csrfToken
}

export async function login(
  request: LoginRequest,
): Promise<CurrentUser> {
  const csrf = await getCsrfToken()
  return unwrapResponse(
    httpClient.post<ApiResponse<CurrentUser>>(
      '/v1/auth/login',
      request,
      { headers: { [csrf.headerName]: csrf.token } },
    ),
  )
}

export function getCurrentUser(): Promise<CurrentUser> {
  return unwrapResponse(
    httpClient.get<ApiResponse<CurrentUser>>('/v1/auth/me'),
  )
}

export async function logout(): Promise<void> {
  const csrf = await getCsrfToken()
  await unwrapResponse(
    httpClient.post<ApiResponse<{ loggedOut: boolean }>>(
      '/v1/auth/logout',
      null,
      { headers: { [csrf.headerName]: csrf.token } },
    ),
  )
  csrfToken = null
}

export function clearCsrfToken(): void {
  csrfToken = null
}
