import { describe, expect, it } from 'vitest'

import { resolveSafeReturnPath } from '@/utils/authRedirect'

describe('resolveSafeReturnPath', () => {
  it('keeps internal routes and rejects external or recursive login paths', () => {
    expect(resolveSafeReturnPath('/jobs/7?from=%2Fjobs')).toBe(
      '/jobs/7?from=%2Fjobs',
    )
    expect(resolveSafeReturnPath('https://evil.test')).toBe('/jobs')
    expect(resolveSafeReturnPath('//evil.test')).toBe('/jobs')
    expect(resolveSafeReturnPath('/login?redirect=/login')).toBe('/jobs')
  })
})
