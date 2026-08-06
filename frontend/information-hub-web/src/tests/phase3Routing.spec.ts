import { describe, expect, it } from 'vitest'
import { createMemoryHistory } from 'vue-router'

import { createAppRouter } from '@/router'

const user = {
  id: 1,
  username: 'admin',
  displayName: 'Admin',
  timezone: 'Asia/Shanghai',
}

describe('Phase 3 routes', () => {
  it('registers every frozen Phase 3 Web route', () => {
    const router = createAppRouter(createMemoryHistory(), async () => user)
    const paths = router.getRoutes().map((route) => route.path)

    expect(paths).toEqual(expect.arrayContaining([
      '/ai/prompts',
      '/ai/analyze',
      '/ai/analyses/:id',
      '/ai/batches',
      '/ai/batches/:id',
      '/ai/schedules',
      '/ai/usage',
    ]))
  })

  it('protects Phase 3 routes with the existing Session guard', async () => {
    const router = createAppRouter(createMemoryHistory(), async () => null)
    await router.push('/ai/usage')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/ai/usage')
  })
})
