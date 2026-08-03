import axios from 'axios'

import { toApiClientError } from './apiError'

const DEFAULT_API_BASE_URL = '/api'
const REQUEST_TIMEOUT_MILLISECONDS = 10_000
let unauthorizedHandler: ((requestUrl: string) => void) | null = null

export function setUnauthorizedHandler(
  handler: ((requestUrl: string) => void) | null,
): void {
  unauthorizedHandler = handler
}

export function resolveApiBaseUrl(value?: string): string {
  const normalized = value?.trim() || DEFAULT_API_BASE_URL
  return normalized.length > 1 ? normalized.replace(/\/+$/, '') : normalized
}

export const httpClient = axios.create({
  baseURL: resolveApiBaseUrl(import.meta.env.VITE_API_BASE_URL),
  timeout: REQUEST_TIMEOUT_MILLISECONDS,
  withCredentials: true,
  headers: {
    Accept: 'application/json',
  },
})

httpClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401 &&
      unauthorizedHandler
    ) {
      unauthorizedHandler(error.config?.url ?? '')
    }
    return Promise.reject(toApiClientError(error))
  },
)
