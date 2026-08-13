import AxiosMockAdapter from 'axios-mock-adapter'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'

import { executeAnalysis, getAnalysis } from '@/api/analysisApi'
import { clearCsrfToken } from '@/api/authApi'
import {
  confirmAnalysisBatch,
  createAnalysisPreview,
  getAnalysisBatch,
  getAnalysisBatchProgress,
  getAnalysisPreviewLimits,
  listAnalysisBatches,
} from '@/api/batchApi'
import { httpClient } from '@/api/httpClient'
import {
  createAnalysisSchedule,
  listAnalysisSchedules,
  previewAnalysisSchedule,
  updateAnalysisSchedule,
  updateAnalysisScheduleStatus,
} from '@/api/scheduleApi'
import { getAnalysisUsage } from '@/api/usageApi'

const success = <T>(code: string, data: T) => ({
  success: true,
  code,
  data,
})

describe('Phase 3 API clients', () => {
  let mock: AxiosMockAdapter

  beforeEach(() => {
    mock = new AxiosMockAdapter(httpClient)
    clearCsrfToken()
    mock.onGet('/v1/auth/csrf').reply(200, success(
      'CSRF_TOKEN_CREATED',
      {
        headerName: 'X-CSRF-TOKEN',
        parameterName: '_csrf',
        token: 'phase3-csrf',
      },
    ))
  })

  afterEach(() => {
    mock.restore()
    clearCsrfToken()
  })

  it('executes and reads an owner-scoped Analysis', async () => {
    const analysis = { id: 31, status: 'SUCCEEDED' }
    mock.onPost('/v1/ai/analyses').reply((config) => {
      expect(config.headers?.['X-CSRF-TOKEN']).toBe('phase3-csrf')
      expect(JSON.parse(config.data)).toEqual({
        informationId: 7,
        promptProfileId: 11,
      })
      return [200, success('INFORMATION_ANALYSIS_RESOLVED', analysis)]
    })
    mock.onGet('/v1/ai/analyses/31').reply(
      200,
      success('INFORMATION_ANALYSIS_FOUND', analysis),
    )

    await expect(executeAnalysis({
      informationId: 7,
      promptProfileId: 11,
    })).resolves.toMatchObject({ id: 31 })
    await expect(getAnalysis(31)).resolves.toMatchObject({
      status: 'SUCCEEDED',
    })
  })

  it('uses CSRF for Preview and Confirm and supports Batch reads', async () => {
    const limits = {
      windowDays: { defaultValue: 3, minimum: 1, maximum: 14 },
      maxCandidates: { defaultValue: 20, minimum: 1, maximum: 50 },
      maxEstimatedTokens: {
        defaultValue: 75_000,
        minimum: 1,
        maximum: 200_000,
      },
    }
    const preview = { previewToken: 'signed', selectedCount: 1 }
    const batch = { id: 41, status: 'PENDING' }
    const progress = { itemCount: 1, selectedCount: 1 }
    mock.onGet('/v1/ai/analysis-batches/limits').reply(
      200,
      success('ANALYSIS_PREVIEW_LIMITS_RETRIEVED', limits),
    )
    mock.onPost('/v1/ai/analysis-batches/preview').reply((config) => {
      expect(config.headers?.['X-CSRF-TOKEN']).toBe('phase3-csrf')
      return [200, success('ANALYSIS_PREVIEW_CREATED', preview)]
    })
    mock.onPost('/v1/ai/analysis-batches/confirm').reply((config) => {
      expect(config.headers?.['X-CSRF-TOKEN']).toBe('phase3-csrf')
      expect(JSON.parse(config.data)).toEqual({ previewToken: 'signed' })
      return [202, success('ANALYSIS_BATCH_ACCEPTED', batch)]
    })
    mock.onGet('/v1/ai/analysis-batches').reply((config) => {
      expect(config.params).toEqual({ limit: 50 })
      return [200, success('ANALYSIS_BATCHES_FOUND', [batch])]
    })
    mock.onGet('/v1/ai/analysis-batches/41').reply(
      200,
      success('ANALYSIS_BATCH_FOUND', batch),
    )
    mock.onGet('/v1/ai/analysis-batches/41/progress').reply(
      200,
      success('ANALYSIS_BATCH_PROGRESS_FOUND', progress),
    )

    await expect(getAnalysisPreviewLimits()).resolves.toEqual(limits)
    await expect(createAnalysisPreview({
      promptProfileId: 11,
    })).resolves.toMatchObject({ previewToken: 'signed' })
    await expect(confirmAnalysisBatch('signed')).resolves.toMatchObject({
      id: 41,
    })
    await expect(listAnalysisBatches(50)).resolves.toHaveLength(1)
    await expect(getAnalysisBatch(41)).resolves.toMatchObject({ id: 41 })
    await expect(getAnalysisBatchProgress(41)).resolves.toMatchObject({
      itemCount: 1,
    })
  })

  it('keeps Schedule config writes separate from status changes', async () => {
    const schedule = { id: 51, enabled: false }
    const assertCsrf = (config: { headers?: Record<string, unknown> }) => {
      expect(config.headers?.['X-CSRF-TOKEN']).toBe('phase3-csrf')
    }
    mock.onGet('/v1/ai/analysis-schedules').reply(
      200,
      success('ANALYSIS_SCHEDULES_FOUND', [schedule]),
    )
    mock.onPost('/v1/ai/analysis-schedules').reply((config) => {
      assertCsrf(config)
      expect(JSON.parse(config.data).enabled).toBe(false)
      return [200, success('ANALYSIS_SCHEDULE_CREATED', schedule)]
    })
    mock.onPut('/v1/ai/analysis-schedules/51').reply((config) => {
      assertCsrf(config)
      expect(JSON.parse(config.data)).not.toHaveProperty('enabled')
      return [200, success('ANALYSIS_SCHEDULE_UPDATED', schedule)]
    })
    mock.onPut('/v1/ai/analysis-schedules/51/status').reply((config) => {
      assertCsrf(config)
      expect(JSON.parse(config.data)).toEqual({ enabled: true })
      return [200, success(
        'ANALYSIS_SCHEDULE_STATUS_UPDATED',
        { ...schedule, enabled: true },
      )]
    })
    mock.onPost('/v1/ai/analysis-schedules/51/preview').reply((config) => {
      assertCsrf(config)
      return [200, success(
        'ANALYSIS_SCHEDULE_PREVIEW_CREATED',
        { selectedCount: 0 },
      )]
    })

    await expect(listAnalysisSchedules()).resolves.toHaveLength(1)
    await createAnalysisSchedule({
      name: 'Daily',
      promptProfileId: 11,
      enabled: false,
    })
    await updateAnalysisSchedule(51, {
      name: 'Daily',
      promptProfileId: 11,
    })
    await updateAnalysisScheduleStatus(51, true)
    await expect(previewAnalysisSchedule(51)).resolves.toMatchObject({
      selectedCount: 0,
    })
  })

  it('reads today, month and all-time Actual Usage without CSRF', async () => {
    const usage = {
      timezone: 'Asia/Shanghai',
      today: { actualTotalTokens: 150 },
      month: { actualTotalTokens: 700 },
      allTime: { actualTotalTokens: 1300 },
    }
    mock.onGet('/v1/ai/usage').reply(
      200,
      success('ANALYSIS_USAGE_FOUND', usage),
    )

    await expect(getAnalysisUsage()).resolves.toMatchObject(usage)
    expect(mock.history.get).toHaveLength(1)
    expect(mock.history.get[0]?.headers?.['X-CSRF-TOKEN']).toBeUndefined()
  })
})
