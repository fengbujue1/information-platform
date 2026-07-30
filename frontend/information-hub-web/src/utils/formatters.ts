import type { JsonValue } from '@/types/json'

export const EMPTY_VALUE_PLACEHOLDER = '—'

export interface DateTimeFormatOptions {
  locale?: string
  timeZone?: string
}

export interface SalaryFormatInput {
  salaryText?: string | null
  salaryMinMonthlyYuan?: number | null
  salaryMaxMonthlyYuan?: number | null
  salaryMonths?: number | null
}

export function formatNullableValue(
  value: string | number | null | undefined,
): string {
  if (typeof value === 'string') {
    return value.trim() || EMPTY_VALUE_PLACEHOLDER
  }
  return value === null || value === undefined
    ? EMPTY_VALUE_PLACEHOLDER
    : String(value)
}

export function formatDateTime(
  value: string | null | undefined,
  options: DateTimeFormatOptions = {},
): string {
  if (!value?.trim()) {
    return EMPTY_VALUE_PLACEHOLDER
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return EMPTY_VALUE_PLACEHOLDER
  }

  return new Intl.DateTimeFormat(options.locale ?? 'zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
    timeZone: options.timeZone,
  }).format(date)
}

export function formatDateTimeOrText(
  value: string | null | undefined,
): string {
  const candidate = value?.trim()
  if (!candidate) {
    return EMPTY_VALUE_PLACEHOLDER
  }

  const formatted = formatDateTime(candidate)
  return formatted === EMPTY_VALUE_PLACEHOLDER ? candidate : formatted
}

export function formatJsonValueList(
  values: JsonValue[] | null | undefined,
): string[] {
  if (!values) {
    return []
  }

  return values.flatMap((value) => {
    if (value === null) {
      return []
    }
    if (typeof value === 'string') {
      const normalized = value.trim()
      return normalized ? [normalized] : []
    }
    if (typeof value === 'number' || typeof value === 'boolean') {
      return [String(value)]
    }
    return [JSON.stringify(value)]
  })
}
function isValidSalaryValue(value: number | null | undefined): value is number {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0
}

function formatYuan(value: number): string {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 0,
  }).format(value)
}

export function formatSalary(input: SalaryFormatInput): string {
  const sourceText = input.salaryText?.trim()
  if (sourceText) {
    return sourceText
  }

  const minimum = input.salaryMinMonthlyYuan
  const maximum = input.salaryMaxMonthlyYuan
  const hasMinimum = isValidSalaryValue(minimum)
  const hasMaximum = isValidSalaryValue(maximum)
  if (!hasMinimum && !hasMaximum) {
    return EMPTY_VALUE_PLACEHOLDER
  }

  let result: string
  if (hasMinimum && hasMaximum) {
    result = `${formatYuan(minimum)}–${formatYuan(maximum)}/月`
  } else if (hasMinimum) {
    result = `${formatYuan(minimum)}+/月`
  } else {
    result = `最高 ${formatYuan(maximum!)}/月`
  }

  if (
    typeof input.salaryMonths === 'number' &&
    Number.isInteger(input.salaryMonths) &&
    input.salaryMonths > 0
  ) {
    result += ` · ${input.salaryMonths}薪`
  }

  return result
}
