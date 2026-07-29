// @vitest-environment node

import { describe, expect, it } from 'vitest'

import { createViteConfig } from '../../vite.config'

describe('Vite API proxy', () => {
  it('proxies /api to the configured Information Hub without rewriting paths', () => {
    const config = createViteConfig('http://127.0.0.1:9080')
    const proxy = config.server?.proxy?.['/api']

    expect(proxy).toMatchObject({
      target: 'http://127.0.0.1:9080',
      changeOrigin: true,
    })
    expect(proxy).not.toHaveProperty('rewrite')
  })

  it('uses the local Information Hub default when the target is empty', () => {
    const config = createViteConfig('   ')

    expect(config.server?.proxy?.['/api']).toMatchObject({
      target: 'http://127.0.0.1:8080',
    })
  })
})
