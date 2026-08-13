import { ElMessage } from 'element-plus'

import { toApiClientError } from '@/api/apiError'

const MESSAGE_DURATION_MS = 3_000
const MESSAGE_OFFSET_PX = 88

interface FeedbackOptions {
  type: 'success' | 'warning' | 'error' | 'info'
  message: string
}

function showFeedback(options: FeedbackOptions): void {
  ElMessage({
    ...options,
    duration: MESSAGE_DURATION_MS,
    offset: MESSAGE_OFFSET_PX,
    grouping: true,
    showClose: true,
    customClass: 'app-global-message',
  })
}

export function showSuccess(message: string): void {
  showFeedback({ type: 'success', message })
}

export function showWarning(message: string): void {
  showFeedback({ type: 'warning', message })
}

export function showError(error: unknown): void {
  showFeedback({
    type: 'error',
    message: toApiClientError(error).message,
  })
}

export function showErrorMessage(message: string): void {
  showFeedback({ type: 'error', message })
}
