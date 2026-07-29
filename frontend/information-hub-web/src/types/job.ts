import type { JsonValue } from './json'

export type JobSortBy =
  | 'firstSeenTime'
  | 'lastSeenTime'
  | 'publishTime'
  | 'salaryMinMonthlyYuan'

export type SortDirection = 'asc' | 'desc'

export interface JobQueryParams {
  page?: number | null
  size?: number | null
  keyword?: string | null
  company?: string | null
  city?: string | null
  salaryMin?: number | null
  salaryMax?: number | null
  source?: string | null
  jobStatus?: string | null
  remoteType?: string | null
  sortBy?: JobSortBy | null
  sortDirection?: SortDirection | null
}

export interface JobListItem {
  id: number
  source: string
  sourceItemId: string
  sourceUrl: string | null
  title: string
  companyName: string | null
  salaryText: string | null
  salaryMinMonthlyYuan: number | null
  salaryMaxMonthlyYuan: number | null
  salaryMonths: number | null
  locationName: string | null
  cityName: string | null
  experienceText: string | null
  educationText: string | null
  remoteType: string
  jobStatus: string
  publishTime: string | null
  firstSeenTime: string
  lastSeenTime: string
  currentVersionNo: number
}

export interface JobDetail {
  id: number
  source: string
  sourceItemId: string
  sourceUrl: string | null
  title: string
  content: string | null
  publishTime: string | null
  collectedAt: string
  firstSeenTime: string
  lastSeenTime: string
  currentVersionNo: number
  collectorId: string
  collectorVersion: string
  sourceCompanyId: string | null
  sourceRecruiterId: string | null
  companyName: string | null
  companyUrl: string | null
  companyScaleText: string | null
  companyStageText: string | null
  companyIndustryText: string | null
  salaryText: string | null
  salarySource: string | null
  salaryMinMonthlyYuan: number | null
  salaryMaxMonthlyYuan: number | null
  salaryMonths: number | null
  locationName: string | null
  cityName: string | null
  areaName: string | null
  businessDistrictName: string | null
  experienceText: string | null
  educationText: string | null
  recruiterName: string | null
  recruiterTitle: string | null
  recruiterActiveText: string | null
  remoteType: string
  jobStatus: string
  detailStatus: string
  detailCollectedAt: string | null
  sourceTags: JsonValue[] | null
  sourceSkillTags: JsonValue[] | null
  welfare: JsonValue[] | null
}

export interface JobSnapshot {
  id: number
  versionNo: number
  contentHash: string
  title: string
  content: string | null
  standardizedPayload: JsonValue
  collectedAt: string
  collectorId: string
  collectorVersion: string
  createdAt: string
}
