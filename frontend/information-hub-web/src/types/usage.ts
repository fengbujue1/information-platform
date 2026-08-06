export interface AnalysisUsagePeriod {
  periodStart: string | null
  periodEnd: string | null
  invocationCount: number
  reportedInvocationCount: number
  unavailableInvocationCount: number
  actualInputTokens: number | null
  actualOutputTokens: number | null
  actualTotalTokens: number | null
}

export interface AnalysisUsage {
  timezone: string
  today: AnalysisUsagePeriod
  month: AnalysisUsagePeriod
  allTime: AnalysisUsagePeriod
}
