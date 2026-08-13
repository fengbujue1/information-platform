import { describe, expect, it } from 'vitest'

import { createBusinessApiError, getApiErrorMessage } from '@/api/apiError'

describe('API error localization', () => {
  it('maps the disabled schedule profile error to actionable Chinese', () => {
    expect(getApiErrorMessage('ANALYSIS_SCHEDULE_PROFILE_DISABLED')).toBe(
      '所选提示词方案已停用，请先启用后再保存定时分析',
    )
  })

  it('accepts a controlled Chinese server fallback but rejects raw English', () => {
    expect(createBusinessApiError({
      success: false,
      code: 'FUTURE_ERROR',
      message: '新的安全中文提示',
    }).message).toBe('新的安全中文提示')
    expect(createBusinessApiError({
      success: false,
      code: 'FUTURE_ERROR',
      message: 'Internal implementation failed',
    }).message).toBe('请求失败，请稍后重试')
  })
})
