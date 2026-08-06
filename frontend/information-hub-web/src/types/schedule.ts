export interface AnalysisScheduleRun {
  batchId: number
  scheduledFor: string
  status: string
  skipReason: string | null
  completedAt: string | null
}

export interface AnalysisSchedule {
  id: number
  name: string
  promptProfileId: number
  enabled: boolean
  localTime: string
  timezone: string
  windowDays: number
  maxCandidates: number
  maxEstimatedTokens: number
  nextRunAt: string | null
  createdAt: string
  updatedAt: string
  lastRun: AnalysisScheduleRun | null
}

export interface SaveAnalysisScheduleRequest {
  name: string
  promptProfileId: number
  localTime?: string
  timezone?: string
  windowDays?: number
  maxCandidates?: number
  maxEstimatedTokens?: number
  enabled?: boolean
}

export type UpdateAnalysisScheduleRequest = Omit<
  SaveAnalysisScheduleRequest,
  'enabled'
>
