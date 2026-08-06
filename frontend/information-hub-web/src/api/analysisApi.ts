import { csrfHeaders, unwrapResponse } from '@/api/apiSupport'
import { httpClient } from '@/api/httpClient'
import type { ApiResponse } from '@/types/api'
import type {
  ExecuteAnalysisRequest,
  InformationAnalysis,
} from '@/types/analysis'

export async function executeAnalysis(
  request: ExecuteAnalysisRequest,
): Promise<InformationAnalysis> {
  return unwrapResponse(
    httpClient.post<ApiResponse<InformationAnalysis>>(
      '/v1/ai/analyses',
      request,
      { headers: await csrfHeaders() },
    ),
  )
}

export function getAnalysis(
  analysisId: number,
): Promise<InformationAnalysis> {
  return unwrapResponse(
    httpClient.get<ApiResponse<InformationAnalysis>>(
      `/v1/ai/analyses/${analysisId}`,
    ),
  )
}
