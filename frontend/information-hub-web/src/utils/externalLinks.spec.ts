import { describe, expect, it } from 'vitest'

import { resolveSafeExternalUrl } from './externalLinks'

describe('external link utilities', () => {
  it('allows absolute HTTP and HTTPS links', () => {
    expect(resolveSafeExternalUrl('https://example.test/jobs/7')).toBe(
      'https://example.test/jobs/7',
    )
    expect(resolveSafeExternalUrl(' http://example.test/company ')).toBe(
      'http://example.test/company',
    )
  })

  it('rejects unsafe, relative and malformed links', () => {
    expect(resolveSafeExternalUrl('javascript:alert(1)')).toBeNull()
    expect(resolveSafeExternalUrl('data:text/html,test')).toBeNull()
    expect(resolveSafeExternalUrl('/jobs/7')).toBeNull()
    expect(resolveSafeExternalUrl('not a url')).toBeNull()
    expect(resolveSafeExternalUrl(null)).toBeNull()
  })
})
