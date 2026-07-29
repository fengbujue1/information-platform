import { describe, expect, it } from 'vitest'

import {
  DEFAULT_JOB_LIST_QUERY,
  parseJobListQuery,
  toJobListRouteQuery,
} from './jobListQuery'

describe('job list URL query', () => {
  it('uses the accepted defaults when the URL has no query', () => {
    expect(parseJobListQuery({})).toEqual({
      state: DEFAULT_JOB_LIST_QUERY,
      hasInvalidParameters: false,
    })
  })

  it('restores every supported filter and uses the first array value', () => {
    const parsed = parseJobListQuery({
      page: '3',
      size: ['50', '100'],
      keyword: '  Java  ',
      company: 'Example',
      city: '成都',
      salaryMin: '20000',
      salaryMax: '30000',
      source: 'BOSS',
      jobStatus: 'ACTIVE',
      remoteType: 'REMOTE',
      sortBy: 'lastSeenTime',
      sortDirection: 'asc',
    })

    expect(parsed.hasInvalidParameters).toBe(false)
    expect(parsed.state).toEqual({
      page: 3,
      size: 50,
      keyword: 'Java',
      company: 'Example',
      city: '成都',
      salaryMin: 20_000,
      salaryMax: 30_000,
      source: 'BOSS',
      jobStatus: 'ACTIVE',
      remoteType: 'REMOTE',
      sortBy: 'lastSeenTime',
      sortDirection: 'asc',
    })
  })

  it('replaces invalid numeric, salary and sort parameters with safe values', () => {
    const parsed = parseJobListQuery({
      page: '0',
      size: '101',
      salaryMin: '30000',
      salaryMax: '20000',
      sortBy: 'dropTable',
      sortDirection: 'sideways',
    })

    expect(parsed.hasInvalidParameters).toBe(true)
    expect(parsed.state).toMatchObject({
      page: 1,
      size: 20,
      salaryMin: null,
      salaryMax: null,
      sortBy: 'firstSeenTime',
      sortDirection: 'desc',
    })
  })

  it('omits empty filters while keeping explicit pagination and sorting', () => {
    expect(
      toJobListRouteQuery({
        ...DEFAULT_JOB_LIST_QUERY,
        keyword: '  Vue  ',
        salaryMin: 0,
      }),
    ).toEqual({
      page: '1',
      size: '20',
      keyword: 'Vue',
      salaryMin: '0',
      sortBy: 'firstSeenTime',
      sortDirection: 'desc',
    })
  })
})
