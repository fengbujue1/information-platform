export type AnalysisBatchStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'COMPLETED'
  | 'PARTIAL_FAILED'
  | 'FAILED'
  | 'NOOP'

export type AnalysisBatchItemStatus =
  | 'SELECTED'
  | 'RUNNING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'SKIPPED'
  | 'DEFERRED'

export interface AnalysisPreview {
  windowStart: string
  windowEnd: string
  totalInWindow: number
  eligibleCount: number
  pendingCount: number
  alreadyAnalyzedCount: number
  selectedCount: number
  deferredByItemLimitCount: number
  deferredByTokenBudgetCount: number
  estimatedInputTokens: number
  estimatedOutputTokens: number
  estimatedTotalTokens: number
  estimateMethod: string
  expiresAt: string
  previewToken: string
}

export interface AnalysisLimitRange {
  defaultValue: number
  minimum: number
  maximum: number
}

export interface AnalysisPreviewLimits {
  windowDays: AnalysisLimitRange
  maxCandidates: AnalysisLimitRange
  maxEstimatedTokens: AnalysisLimitRange
}

export interface CreateAnalysisPreviewRequest {
  promptProfileId: number
  windowDays?: number
  maxCandidates?: number
  maxEstimatedTokens?: number
}

export interface AnalysisBatchProgress {
  itemCount: number
  selectedCount: number
  runningCount: number
  succeededCount: number
  failedCount: number
  skippedCount: number
  deferredCount: number
  reportedInvocationCount: number
  unavailableInvocationCount: number
  actualInputTokens: number | null
  actualOutputTokens: number | null
  actualTotalTokens: number | null
}

export interface AnalysisBatchItem {
  id: number
  informationId: number
  snapshotId: number
  analysisId: number | null
  selectionOrder: number
  status: AnalysisBatchItemStatus
  decisionReason: string | null
  estimatedInputTokens: number | null
  estimatedOutputTokens: number | null
  estimatedTotalTokens: number | null
  startedAt: string | null
  completedAt: string | null
}

export interface AnalysisBatch {
  id: number
  triggerType: 'MANUAL' | 'SCHEDULED'
  scheduleId: number | null
  scheduledFor: string | null
  promptProfileId: number
  promptVersionId: number
  informationType: string
  analysisDefinitionKey: string
  analysisDefinitionVersion: number
  windowBasis: string
  requestedWindowDays: number
  windowStart: string
  windowEnd: string
  requestedMaxCandidates: number
  requestedTokenBudget: number
  totalInWindow: number
  eligibleCount: number
  alreadyAnalyzedCount: number
  selectedCount: number
  deferredByItemLimitCount: number
  deferredByTokenBudgetCount: number
  estimatedInputTokens: number | null
  estimatedOutputTokens: number | null
  estimatedTotalTokens: number | null
  estimateMethod: string | null
  status: AnalysisBatchStatus
  skipReason: string | null
  startedAt: string | null
  completedAt: string | null
  createdAt: string
  updatedAt: string
  progress: AnalysisBatchProgress
  items: AnalysisBatchItem[]
}
