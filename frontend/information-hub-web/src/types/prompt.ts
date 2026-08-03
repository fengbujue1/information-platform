export type PromptProfileStatus = 'ACTIVE' | 'DISABLED'

export type PromptAnalysisDefinitionKey = 'JOB_USER_RELEVANCE'

export interface PromptProfile {
  id: number
  name: string
  analysisDefinitionKey: PromptAnalysisDefinitionKey
  activeVersionId: number | null
  status: PromptProfileStatus
  createdAt: string
  updatedAt: string
}

export interface PromptVersion {
  id: number
  promptProfileId: number
  versionNo: number
  content: string
  contentHash: string
  createdAt: string
}

export interface CreatePromptProfileRequest {
  name: string
  analysisDefinitionKey: PromptAnalysisDefinitionKey
}

export interface CreatePromptVersionRequest {
  content: string
}
