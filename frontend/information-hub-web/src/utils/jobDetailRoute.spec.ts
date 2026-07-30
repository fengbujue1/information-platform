import { describe, expect, it } from 'vitest'

import {
  createJobDetailTarget,
  createJobSnapshotsTarget,
  parseJobDetailId,
  resolveJobListReturnTarget,
} from './jobDetailRoute'

describe('job detail route utilities', () => {
  it('accepts only safe positive integer job ids', () => {
    expect(parseJobDetailId('7')).toBe(7)
    expect(parseJobDetailId(['9'])).toBe(9)
    expect(parseJobDetailId('0')).toBeNull()
    expect(parseJobDetailId('-1')).toBeNull()
    expect(parseJobDetailId('1.5')).toBeNull()
    expect(parseJobDetailId('9007199254740992')).toBeNull()
    expect(parseJobDetailId(undefined)).toBeNull()
  })

  it('accepts only the jobs list and its query as return targets', () => {
    expect(resolveJobListReturnTarget('/jobs')).toBe('/jobs')
    expect(
      resolveJobListReturnTarget('/jobs?page=2&keyword=Java'),
    ).toBe('/jobs?page=2&keyword=Java')
    expect(resolveJobListReturnTarget(['/jobs?size=50'])).toBe(
      '/jobs?size=50',
    )

    expect(resolveJobListReturnTarget('/jobs/7')).toBe('/jobs')
    expect(resolveJobListReturnTarget('https://evil.example/jobs')).toBe(
      '/jobs',
    )
    expect(resolveJobListReturnTarget('//evil.example')).toBe('/jobs')
    expect(resolveJobListReturnTarget(undefined)).toBe('/jobs')
  })

  it('preserves the safe return target across detail and snapshot routes', () => {
    const returnTarget = '/jobs?page=2&keyword=Java'

    expect(createJobSnapshotsTarget(7, returnTarget)).toEqual({
      name: 'job-snapshots',
      params: { id: '7' },
      query: { from: returnTarget },
    })
    expect(createJobDetailTarget(7, returnTarget)).toEqual({
      name: 'job-detail',
      params: { id: '7' },
      query: { from: returnTarget },
    })
  })
})
