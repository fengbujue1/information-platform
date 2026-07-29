import { describe, expect, it } from 'vitest'

import {
  EMPTY_VALUE_PLACEHOLDER,
  formatDateTime,
  formatNullableValue,
  formatSalary,
} from './formatters'

describe('formatNullableValue', () => {
  it('keeps meaningful values and replaces empty values', () => {
    expect(formatNullableValue('  Example  ')).toBe('Example')
    expect(formatNullableValue(0)).toBe('0')
    expect(formatNullableValue('   ')).toBe(EMPTY_VALUE_PLACEHOLDER)
    expect(formatNullableValue(null)).toBe(EMPTY_VALUE_PLACEHOLDER)
  })
})

describe('formatDateTime', () => {
  it('formats UTC input in the requested display timezone', () => {
    const formatted = formatDateTime('2026-07-28T08:00:00Z', {
      locale: 'zh-CN',
      timeZone: 'Asia/Shanghai',
    })

    expect(formatted).toMatch(/2026.*07.*28.*16.*00.*00/)
  })

  it('uses the common placeholder for missing or invalid input', () => {
    expect(formatDateTime(null)).toBe(EMPTY_VALUE_PLACEHOLDER)
    expect(formatDateTime('not-a-date')).toBe(EMPTY_VALUE_PLACEHOLDER)
  })
})

describe('formatSalary', () => {
  it('prefers the source salary text', () => {
    expect(
      formatSalary({
        salaryText: '  20-30K·13薪  ',
        salaryMinMonthlyYuan: 20_000,
        salaryMaxMonthlyYuan: 30_000,
      }),
    ).toBe('20-30K·13薪')
  })

  it('formats a standardized salary range only when source text is absent', () => {
    const formatted = formatSalary({
      salaryText: ' ',
      salaryMinMonthlyYuan: 20_000,
      salaryMaxMonthlyYuan: 30_000,
      salaryMonths: 13,
    })

    expect(formatted).toContain('20,000')
    expect(formatted).toContain('30,000')
    expect(formatted).toContain('/月')
    expect(formatted).toContain('13薪')
  })

  it('formats a one-sided maximum and replaces an unknown salary', () => {
    expect(
      formatSalary({
        salaryMaxMonthlyYuan: 30_000,
      }),
    ).toContain('最高')
    expect(formatSalary({})).toBe(EMPTY_VALUE_PLACEHOLDER)
  })
})
