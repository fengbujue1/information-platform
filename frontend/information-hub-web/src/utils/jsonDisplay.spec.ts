import { describe, expect, it } from 'vitest'

import { formatJsonForDisplay } from './jsonDisplay'

describe('JSON display formatter', () => {
  it('pretty prints nested standardized JSON deterministically', () => {
    expect(
      formatJsonForDisplay({
        companyName: 'Example company',
        salary: {
          minimum: 20_000,
          maximum: 30_000,
        },
        tags: ['Java', 'Spring'],
      }),
    ).toBe(
      [
        '{',
        '  "companyName": "Example company",',
        '  "salary": {',
        '    "minimum": 20000,',
        '    "maximum": 30000',
        '  },',
        '  "tags": [',
        '    "Java",',
        '    "Spring"',
        '  ]',
        '}',
      ].join('\n'),
    )
  })

  it('keeps HTML-like strings as inert JSON text', () => {
    expect(
      formatJsonForDisplay({
        content: '<script>alert("unsafe")</script>',
      }),
    ).toContain('<script>alert(\\"unsafe\\")</script>')
    expect(formatJsonForDisplay(null)).toBe('null')
  })
})
