import axios from 'axios'

import { toApiClientError } from './apiError'

const DEFAULT_API_BASE_URL = '/api'
const REQUEST_TIMEOUT_MILLISECONDS = 10_000

export function resolveApiBaseUrl(value?: string): string {
  const normalized = value?.trim() || DEFAULT_API_BASE_URL
  return normalized.length > 1 ? normalized.replace(/\/+$/, '') : normalized
}

export const httpClient = axios.create({
  baseURL: resolveApiBaseUrl(import.meta.env.VITE_API_BASE_URL),
  timeout: REQUEST_TIMEOUT_MILLISECONDS,
  headers: {
    Accept: 'application/json',
  },
})

httpClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => Promise.reject(toApiClientError(error)),
)
