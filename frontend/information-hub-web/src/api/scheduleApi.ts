import { csrfHeaders, unwrapResponse } from '@/api/apiSupport'
import { httpClient } from '@/api/httpClient'
import type { ApiResponse } from '@/types/api'
import type { AnalysisPreview } from '@/types/batch'
import type {
  AnalysisSchedule,
  SaveAnalysisScheduleRequest,
  UpdateAnalysisScheduleRequest,
} from '@/types/schedule'

export function listAnalysisSchedules(): Promise<AnalysisSchedule[]> {
  return unwrapResponse(
    httpClient.get<ApiResponse<AnalysisSchedule[]>>(
      '/v1/ai/analysis-schedules',
    ),
  )
}

export async function createAnalysisSchedule(
  request: SaveAnalysisScheduleRequest,
): Promise<AnalysisSchedule> {
  return unwrapResponse(
    httpClient.post<ApiResponse<AnalysisSchedule>>(
      '/v1/ai/analysis-schedules',
      request,
      { headers: await csrfHeaders() },
    ),
  )
}

export async function updateAnalysisSchedule(
  scheduleId: number,
  request: UpdateAnalysisScheduleRequest,
): Promise<AnalysisSchedule> {
  return unwrapResponse(
    httpClient.put<ApiResponse<AnalysisSchedule>>(
      `/v1/ai/analysis-schedules/${scheduleId}`,
      request,
      { headers: await csrfHeaders() },
    ),
  )
}

export async function updateAnalysisScheduleStatus(
  scheduleId: number,
  enabled: boolean,
): Promise<AnalysisSchedule> {
  return unwrapResponse(
    httpClient.put<ApiResponse<AnalysisSchedule>>(
      `/v1/ai/analysis-schedules/${scheduleId}/status`,
      { enabled },
      { headers: await csrfHeaders() },
    ),
  )
}

export async function previewAnalysisSchedule(
  scheduleId: number,
): Promise<AnalysisPreview> {
  return unwrapResponse(
    httpClient.post<ApiResponse<AnalysisPreview>>(
      `/v1/ai/analysis-schedules/${scheduleId}/preview`,
      null,
      { headers: await csrfHeaders() },
    ),
  )
}
