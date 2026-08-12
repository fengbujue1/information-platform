import type {
  AnalysisStatus,
  UsageStatus,
} from '@/types/analysis'
import type {
  AnalysisBatchItemStatus,
  AnalysisBatchStatus,
} from '@/types/batch'
import type { PromptProfileStatus } from '@/types/prompt'

const PROMPT_PROFILE_STATUS_LABELS: Record<PromptProfileStatus, string> = {
  ACTIVE: '已启用',
  DISABLED: '已停用',
}

const ANALYSIS_STATUS_LABELS: Record<AnalysisStatus, string> = {
  PENDING: '等待执行',
  RUNNING: '执行中',
  SUCCEEDED: '已成功',
  FAILED: '已失败',
}

const BATCH_STATUS_LABELS: Record<AnalysisBatchStatus, string> = {
  PENDING: '等待执行',
  RUNNING: '执行中',
  COMPLETED: '已完成',
  PARTIAL_FAILED: '部分失败',
  FAILED: '已失败',
  NOOP: '无需执行',
}

const BATCH_ITEM_STATUS_LABELS: Record<AnalysisBatchItemStatus, string> = {
  SELECTED: '已选择',
  RUNNING: '执行中',
  SUCCEEDED: '已成功',
  FAILED: '已失败',
  SKIPPED: '已跳过',
  DEFERRED: '已延后',
}

const BATCH_TRIGGER_LABELS = {
  MANUAL: '手动触发',
  SCHEDULED: '定时触发',
} as const

const USAGE_STATUS_LABELS: Record<UsageStatus, string> = {
  REPORTED: '已报告',
  UNAVAILABLE: '未报告',
}

const REASON_LABELS: Readonly<Record<string, string>> = {
  ALREADY_ANALYZED: '已有成功分析',
  TOKEN_BUDGET: '超出 Token 预算',
  NO_EXECUTABLE_ITEMS: '没有可执行项目',
  CONCURRENT_RUN: '已有批次正在执行',
  MISFIRE: '错过计划执行时间',
  WORKER_DISABLED: '分析 Worker 未启用',
  PROVIDER_UNAVAILABLE: '模型服务不可用',
}

function fallbackLabel(value: string | null | undefined): string {
  return value ? `未知（${value}）` : '—'
}

export function formatPromptProfileStatus(
  status: PromptProfileStatus | string | null | undefined,
): string {
  return PROMPT_PROFILE_STATUS_LABELS[status as PromptProfileStatus]
    ?? fallbackLabel(status)
}

export function formatAnalysisStatus(
  status: AnalysisStatus | string | null | undefined,
): string {
  return ANALYSIS_STATUS_LABELS[status as AnalysisStatus]
    ?? fallbackLabel(status)
}

export function formatBatchStatus(
  status: AnalysisBatchStatus | string | null | undefined,
): string {
  return BATCH_STATUS_LABELS[status as AnalysisBatchStatus]
    ?? fallbackLabel(status)
}

export function formatBatchItemStatus(
  status: AnalysisBatchItemStatus | string | null | undefined,
): string {
  return BATCH_ITEM_STATUS_LABELS[status as AnalysisBatchItemStatus]
    ?? fallbackLabel(status)
}

export function formatBatchTrigger(
  trigger: keyof typeof BATCH_TRIGGER_LABELS | string | null | undefined,
): string {
  return BATCH_TRIGGER_LABELS[trigger as keyof typeof BATCH_TRIGGER_LABELS]
    ?? fallbackLabel(trigger)
}

export function formatUsageStatus(
  status: UsageStatus | string | null | undefined,
): string {
  return USAGE_STATUS_LABELS[status as UsageStatus]
    ?? fallbackLabel(status)
}

export function formatExecutionStatus(
  status: string | null | undefined,
): string {
  if (status === 'PENDING') return '等待执行'
  if (status === 'RUNNING') return '执行中'
  if (status === 'SUCCEEDED' || status === 'COMPLETED') return '已成功'
  if (status === 'PARTIAL_FAILED') return '部分失败'
  if (status === 'FAILED') return '已失败'
  if (status === 'UNKNOWN') return '结果未知'
  if (status === 'NOOP') return '无需执行'
  return fallbackLabel(status)
}

export function formatReason(
  reason: string | null | undefined,
): string {
  if (!reason) return '—'
  return REASON_LABELS[reason] ?? fallbackLabel(reason)
}
