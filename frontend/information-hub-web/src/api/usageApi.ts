import { unwrapResponse } from '@/api/apiSupport'
import { httpClient } from '@/api/httpClient'
import type { ApiResponse } from '@/types/api'
import type { AnalysisUsage } from '@/types/usage'

export function getAnalysisUsage(): Promise<AnalysisUsage> {
  return unwrapResponse(
    httpClient.get<ApiResponse<AnalysisUsage>>('/v1/ai/usage'),
  )
}
