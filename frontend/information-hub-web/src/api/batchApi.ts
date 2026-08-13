import { csrfHeaders, unwrapResponse } from '@/api/apiSupport'
import { httpClient } from '@/api/httpClient'
import type { ApiResponse } from '@/types/api'
import type {
  AnalysisBatch,
  AnalysisBatchProgress,
  AnalysisPreview,
  AnalysisPreviewLimits,
  CreateAnalysisPreviewRequest,
} from '@/types/batch'

export function getAnalysisPreviewLimits(): Promise<AnalysisPreviewLimits> {
  return unwrapResponse(
    httpClient.get<ApiResponse<AnalysisPreviewLimits>>(
      '/v1/ai/analysis-batches/limits',
    ),
  )
}

export async function createAnalysisPreview(
  request: CreateAnalysisPreviewRequest,
): Promise<AnalysisPreview> {
  return unwrapResponse(
    httpClient.post<ApiResponse<AnalysisPreview>>(
      '/v1/ai/analysis-batches/preview',
      request,
      { headers: await csrfHeaders() },
    ),
  )
}

export async function confirmAnalysisBatch(
  previewToken: string,
): Promise<AnalysisBatch> {
  return unwrapResponse(
    httpClient.post<ApiResponse<AnalysisBatch>>(
      '/v1/ai/analysis-batches/confirm',
      { previewToken },
      { headers: await csrfHeaders() },
    ),
  )
}

export function listAnalysisBatches(
  limit = 20,
): Promise<AnalysisBatch[]> {
  return unwrapResponse(
    httpClient.get<ApiResponse<AnalysisBatch[]>>(
      '/v1/ai/analysis-batches',
      { params: { limit } },
    ),
  )
}

export function getAnalysisBatch(batchId: number): Promise<AnalysisBatch> {
  return unwrapResponse(
    httpClient.get<ApiResponse<AnalysisBatch>>(
      `/v1/ai/analysis-batches/${batchId}`,
    ),
  )
}

export function getAnalysisBatchProgress(
  batchId: number,
): Promise<AnalysisBatchProgress> {
  return unwrapResponse(
    httpClient.get<ApiResponse<AnalysisBatchProgress>>(
      `/v1/ai/analysis-batches/${batchId}/progress`,
    ),
  )
}
