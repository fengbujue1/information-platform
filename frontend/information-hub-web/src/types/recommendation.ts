export type RecommendationInformationType = 'JOB'

export type RecommendationRemoteType = 'ONSITE' | 'HYBRID' | 'REMOTE'

export type RecommendationFeedbackState =
  | 'NONE'
  | 'INTERESTED'
  | 'NOT_INTERESTED'

export type JobDisposition =
  | 'NONE'
  | 'CONTACTED'
  | 'CONTACTED_NOT_SUITABLE'

export type RecommendationRunStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'NOOP'

export interface RecommendationProfileRequest {
  analysisPromptProfileId: number
  windowDays: number
  topN: number
  targetRoles: string[]
  preferredSkills: string[]
  preferredCities: string[]
  preferredRemoteTypes: RecommendationRemoteType[]
  salaryMinMonthlyYuan: number | null
  excludedKeywords: string[]
}

export interface RecommendationProfile extends RecommendationProfileRequest {
  id: number
  informationType: RecommendationInformationType
  contentHash: string
  createdAt: string
  updatedAt: string
}

export interface ManualRecommendationRefresh {
  runId: number
  status: 'PENDING'
}

export interface RecommendationRun {
  id: number
  informationType: RecommendationInformationType
  triggerType: 'MANUAL' | 'ANALYSIS_BATCH_COMPLETED'
  sourceAnalysisBatchId: number | null
  profileId: number
  profileContentHash: string
  promptProfileId: number
  promptVersionId: number
  algorithmKey: string
  algorithmVersion: number
  windowStart: string
  windowEnd: string
  candidateCount: number
  eligibleCount: number
  resultCount: number
  status: RecommendationRunStatus
  skipReason: string | null
  failureCode: string | null
  failureMessage: string | null
  startedAt: string | null
  completedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface RecommendationFeedRun {
  id: number
  informationType: RecommendationInformationType
  triggerType: 'MANUAL' | 'ANALYSIS_BATCH_COMPLETED'
  completedAt: string
  algorithmKey: string
  algorithmVersion: number
  profileChangedSinceRun: boolean
}

export interface RecommendationScoreBreakdown {
  aiRelevanceScore: number
  profileMatchScore: number
  freshnessScore: number
}

export interface RecommendationJobSummary {
  title: string
  companyName: string | null
  salaryText: string | null
  locationName: string | null
  remoteType: string | null
  sourceUrl: string | null
}

export interface RecommendationFeedItem {
  recommendationItemId: number
  informationId: number
  snapshotId: number
  rank: number
  finalScore: number
  scoreBreakdown: RecommendationScoreBreakdown
  reasons: string[]
  feedbackState: RecommendationFeedbackState
  jobDisposition: JobDisposition
  viewed: boolean
  job: RecommendationJobSummary
}

export interface RecommendationFeed {
  run: RecommendationFeedRun | null
  page: number
  pageSize: number
  total: number
  items: RecommendationFeedItem[]
}

export interface RecommendationInteraction {
  informationId: number
  viewCount: number
  lastViewedAt: string | null
  feedbackState: RecommendationFeedbackState
  feedbackUpdatedAt: string | null
  jobDisposition: JobDisposition
  dispositionUpdatedAt: string | null
  lastRecommendationItemId: number | null
  updatedAt: string
}
