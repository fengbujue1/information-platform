import { describe, expect, it } from 'vitest'

import {
  formatAnalysisStatus,
  formatBatchItemStatus,
  formatBatchStatus,
  formatBatchTrigger,
  formatPromptProfileStatus,
  formatReason,
  formatUsageStatus,
} from '@/utils/aiDisplay'

describe('AI display labels', () => {
  it('maps stable protocol values to Chinese labels', () => {
    expect(formatPromptProfileStatus('ACTIVE')).toBe('已启用')
    expect(formatAnalysisStatus('SUCCEEDED')).toBe('已成功')
    expect(formatBatchStatus('COMPLETED')).toBe('已完成')
    expect(formatBatchItemStatus('DEFERRED')).toBe('已延后')
    expect(formatBatchTrigger('MANUAL')).toBe('手动触发')
    expect(formatUsageStatus('UNAVAILABLE')).toBe('未报告')
    expect(formatReason('TOKEN_BUDGET')).toBe('超出 Token 预算')
  })

  it('keeps unknown protocol values visible for diagnosis', () => {
    expect(formatBatchStatus('FUTURE_STATUS')).toBe(
      '未知（FUTURE_STATUS）',
    )
    expect(formatReason(null)).toBe('—')
  })
})
