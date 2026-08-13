import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiClientError } from '@/api/apiError'
import { showError, showSuccess } from '@/utils/userFeedback'

const messageMock = vi.hoisted(() => vi.fn())

vi.mock('element-plus', () => ({ ElMessage: messageMock }))

describe('global user feedback', () => {
  beforeEach(() => messageMock.mockReset())

  it('uses a fixed three-second global message configuration', () => {
    showSuccess('保存成功')

    expect(messageMock).toHaveBeenCalledWith(expect.objectContaining({
      type: 'success',
      message: '保存成功',
      duration: 3_000,
      offset: 88,
      grouping: true,
      customClass: 'app-global-message',
    }))
  })

  it('converts API failures before showing them', () => {
    showError(new ApiClientError({
      kind: 'business',
      code: 'ANALYSIS_SCHEDULE_PROFILE_DISABLED',
      message: '所选提示词方案已停用，请先启用后再保存定时分析',
    }))

    expect(messageMock).toHaveBeenCalledWith(expect.objectContaining({
      type: 'error',
      message: '所选提示词方案已停用，请先启用后再保存定时分析',
    }))
  })
})
