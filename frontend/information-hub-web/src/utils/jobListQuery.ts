import type { LocationQuery, LocationQueryRaw } from 'vue-router'

import type {
  JobQueryParams,
  JobSortBy,
  SortDirection,
} from '@/types/job'

export const DEFAULT_JOB_LIST_QUERY: Readonly<JobListQueryState> = {
  page: 1,
  size: 20,
  keyword: '',
  company: '',
  city: '',
  salaryMin: null,
  salaryMax: null,
  source: '',
  jobStatus: '',
  remoteType: '',
  sortBy: 'firstSeenTime',
  sortDirection: 'desc',
}

export const JOB_SORT_OPTIONS: ReadonlyArray<{
  label: string
  value: JobSortBy
}> = [
  { label: '首次发现时间', value: 'firstSeenTime' },
  { label: '最后发现时间', value: 'lastSeenTime' },
  { label: '发布时间', value: 'publishTime' },
  { label: '最低月薪', value: 'salaryMinMonthlyYuan' },
]

const ALLOWED_SORT_FIELDS = new Set<JobSortBy>(
  JOB_SORT_OPTIONS.map((option) => option.value),
)
const ALLOWED_SORT_DIRECTIONS = new Set<SortDirection>(['asc', 'desc'])

export interface JobListQueryState {
  page: number
  size: number
  keyword: string
  company: string
  city: string
  salaryMin: number | null
  salaryMax: number | null
  source: string
  jobStatus: string
  remoteType: string
  sortBy: JobSortBy
  sortDirection: SortDirection
}

export interface JobListFilterDraft {
  keyword: string
  company: string
  city: string
  salaryMin: number | null
  salaryMax: number | null
  source: string
  jobStatus: string
  remoteType: string
  sortBy: JobSortBy
  sortDirection: SortDirection
}

export interface ParsedJobListQuery {
  state: JobListQueryState
  hasInvalidParameters: boolean
}

function firstQueryValue(
  value: LocationQuery[string],
): string | undefined {
  const candidate = Array.isArray(value) ? value[0] : value
  return candidate ?? undefined
}

function parseInteger(
  value: LocationQuery[string],
  minimum: number,
  maximum?: number,
): { value: number | null; invalid: boolean } {
  const rawValue = firstQueryValue(value)
  if (rawValue === undefined || rawValue.trim() === '') {
    return { value: null, invalid: false }
  }

  const parsed = Number(rawValue)
  const invalid =
    !Number.isInteger(parsed) ||
    parsed < minimum ||
    (maximum !== undefined && parsed > maximum)
  return { value: invalid ? null : parsed, invalid }
}

function parseText(value: LocationQuery[string]): string {
  return firstQueryValue(value)?.trim() ?? ''
}

/**
 * 将不可信的 URL Query 收敛为后端允许的列表参数。
 * 无效参数恢复默认值，避免刷新或粘贴链接时向后端发送明显非法的请求。
 */
export function parseJobListQuery(query: LocationQuery): ParsedJobListQuery {
  const page = parseInteger(query.page, 1)
  const size = parseInteger(query.size, 1, 100)
  const salaryMin = parseInteger(query.salaryMin, 0)
  const salaryMax = parseInteger(query.salaryMax, 0)

  const sortByCandidate = firstQueryValue(query.sortBy)
  const sortBy = ALLOWED_SORT_FIELDS.has(sortByCandidate as JobSortBy)
    ? (sortByCandidate as JobSortBy)
    : DEFAULT_JOB_LIST_QUERY.sortBy
  const invalidSortBy =
    sortByCandidate !== undefined &&
    sortByCandidate.trim() !== '' &&
    sortByCandidate !== sortBy

  const sortDirectionCandidate = firstQueryValue(query.sortDirection)
  const sortDirection = ALLOWED_SORT_DIRECTIONS.has(
    sortDirectionCandidate as SortDirection,
  )
    ? (sortDirectionCandidate as SortDirection)
    : DEFAULT_JOB_LIST_QUERY.sortDirection
  const invalidSortDirection =
    sortDirectionCandidate !== undefined &&
    sortDirectionCandidate.trim() !== '' &&
    sortDirectionCandidate !== sortDirection

  const invalidSalaryRange =
    salaryMin.value !== null &&
    salaryMax.value !== null &&
    salaryMin.value > salaryMax.value

  return {
    state: {
      page: page.value ?? DEFAULT_JOB_LIST_QUERY.page,
      size: size.value ?? DEFAULT_JOB_LIST_QUERY.size,
      keyword: parseText(query.keyword),
      company: parseText(query.company),
      city: parseText(query.city),
      salaryMin: invalidSalaryRange ? null : salaryMin.value,
      salaryMax: invalidSalaryRange ? null : salaryMax.value,
      source: parseText(query.source),
      jobStatus: parseText(query.jobStatus),
      remoteType: parseText(query.remoteType),
      sortBy,
      sortDirection,
    },
    hasInvalidParameters:
      page.invalid ||
      size.invalid ||
      salaryMin.invalid ||
      salaryMax.invalid ||
      invalidSalaryRange ||
      invalidSortBy ||
      invalidSortDirection,
  }
}

export function createJobListFilterDraft(
  state: JobListQueryState,
): JobListFilterDraft {
  return {
    keyword: state.keyword,
    company: state.company,
    city: state.city,
    salaryMin: state.salaryMin,
    salaryMax: state.salaryMax,
    source: state.source,
    jobStatus: state.jobStatus,
    remoteType: state.remoteType,
    sortBy: state.sortBy,
    sortDirection: state.sortDirection,
  }
}

export function toJobQueryParams(
  state: JobListQueryState,
): JobQueryParams {
  return { ...state }
}

/**
 * 统一生成可复制的列表 URL。空筛选不进入地址栏，分页和排序始终显式保留。
 */
export function toJobListRouteQuery(
  state: JobListQueryState,
): LocationQueryRaw {
  const query: LocationQueryRaw = {
    page: String(state.page),
    size: String(state.size),
    sortBy: state.sortBy,
    sortDirection: state.sortDirection,
  }

  const textEntries: ReadonlyArray<
    [keyof LocationQueryRaw, string]
  > = [
    ['keyword', state.keyword.trim()],
    ['company', state.company.trim()],
    ['city', state.city.trim()],
    ['source', state.source.trim()],
    ['jobStatus', state.jobStatus.trim()],
    ['remoteType', state.remoteType.trim()],
  ]
  for (const [key, value] of textEntries) {
    if (value) {
      query[key] = value
    }
  }

  if (state.salaryMin !== null) {
    query.salaryMin = String(state.salaryMin)
  }
  if (state.salaryMax !== null) {
    query.salaryMax = String(state.salaryMax)
  }

  return query
}
