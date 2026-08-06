import type { JsonValue } from '@/types/json'

export type AnalysisStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'SUCCEEDED'
  | 'FAILED'

export type UsageStatus = 'REPORTED' | 'UNAVAILABLE'

export interface ModelInvocation {
  id: number
  attemptNo: number
  provider: string
  modelName: string
  providerRequestId: string | null
  status: string
  finishReason: string | null
  inputTokens: number | null
  outputTokens: number | null
  totalTokens: number | null
  cachedInputTokens: number | null
  reasoningTokens: number | null
  usageStatus: UsageStatus
  latencyMs: number | null
  errorCode: string | null
  errorMessage: string | null
  startedAt: string | null
  completedAt: string | null
}

export interface InformationAnalysis {
  id: number
  informationId: number
  snapshotId: number
  informationType: string
  analysisDefinitionKey: string
  analysisDefinitionVersion: number
  analysisPurpose: string
  promptProfileId: number
  promptVersionId: number
  status: AnalysisStatus
  resultJson: JsonValue | null
  relevanceScore: number | null
  summary: string | null
  estimatedInputTokens: number | null
  estimatedOutputTokens: number | null
  estimatedTotalTokens: number | null
  estimateMethod: string | null
  failureCode: string | null
  failureMessage: string | null
  startedAt: string | null
  completedAt: string | null
  createdAt: string
  updatedAt: string
  invocations: ModelInvocation[]
}

export interface ExecuteAnalysisRequest {
  informationId: number
  snapshotId?: number
  promptProfileId: number
  retryFailed?: boolean
}
