import { describe, expect, it } from 'vitest'
import { createMemoryHistory } from 'vue-router'

import { createAppRouter } from '@/router'

const user = {
  id: 1,
  username: 'admin',
  displayName: 'Admin',
  timezone: 'Asia/Shanghai',
}

describe('Phase 4 routes', () => {
  it('registers the Recommendation page and protects it with Session', async () => {
    const authenticatedRouter = createAppRouter(
      createMemoryHistory(),
      async () => user,
    )
    expect(authenticatedRouter.getRoutes().map((route) => route.path)).toContain(
      '/recommendations',
    )

    const anonymousRouter = createAppRouter(createMemoryHistory(), async () => null)
    await anonymousRouter.push('/recommendations')
    await anonymousRouter.isReady()

    expect(anonymousRouter.currentRoute.value.name).toBe('login')
    expect(anonymousRouter.currentRoute.value.query.redirect).toBe('/recommendations')
  })
})
