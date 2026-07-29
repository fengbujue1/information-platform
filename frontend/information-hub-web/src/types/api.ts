export interface ApiSuccessResponse<T> {
  success: true
  code: string
  data: T
  message?: never
}

export interface ApiFailureResponse {
  success: false
  code: string
  message: string
  data?: never
}

export type ApiResponse<T> = ApiSuccessResponse<T> | ApiFailureResponse

export interface PageResponse<T> {
  page: number
  size: number
  total: number
  totalPages: number
  items: T[]
}

export interface ApiRequestOptions {
  signal?: AbortSignal
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

export function isApiSuccessResponse<T>(
  value: unknown,
): value is ApiSuccessResponse<T> {
  return (
    isRecord(value) &&
    value.success === true &&
    typeof value.code === 'string' &&
    'data' in value
  )
}

export function isApiFailureResponse(
  value: unknown,
): value is ApiFailureResponse {
  return (
    isRecord(value) &&
    value.success === false &&
    typeof value.code === 'string' &&
    typeof value.message === 'string'
  )
}
