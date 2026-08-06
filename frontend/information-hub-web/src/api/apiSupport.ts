import type { AxiosResponse } from 'axios'

import {
  createBusinessApiError,
  createInvalidApiResponseError,
} from '@/api/apiError'
import { getCsrfToken } from '@/api/authApi'
import type { ApiResponse } from '@/types/api'
import {
  isApiFailureResponse,
  isApiSuccessResponse,
} from '@/types/api'

export async function unwrapResponse<T>(
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

export async function csrfHeaders(): Promise<Record<string, string>> {
  const csrf = await getCsrfToken()
  return { [csrf.headerName]: csrf.token }
}
