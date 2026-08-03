import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory } from 'vue-router'

import App from '@/App.vue'
import { createAppRouter } from '@/router'
import { setAuthenticatedUserForTest } from '@/stores/authSession'

const testUser = {
  id: 1,
  username: 'admin',
  displayName: 'Admin',
  timezone: 'Asia/Shanghai',
}

const getJobsMock = vi.hoisted(() => vi.fn())

vi.mock('@/api/jobApi', () => ({
  getJobs: getJobsMock,
}))

beforeEach(() => {
  getJobsMock.mockReset()
  getJobsMock.mockResolvedValue({
    page: 1,
    size: 20,
    total: 0,
    totalPages: 0,
    items: [],
  })
})

async function mountAt(path: string) {
  setAuthenticatedUserForTest(testUser)
  const router = createAppRouter(
    createMemoryHistory(),
    async () => testUser,
  )
  await router.push(path)
  await router.isReady()
  const wrapper = mount(App, {
    global: {
      plugins: [router],
    },
  })
  await flushPromises()
  return wrapper
}

describe('application shell', () => {
  it('provides keyboard navigation and a main content target', async () => {
    const wrapper = await mountAt('/jobs')

    expect(wrapper.get('.skip-link').attributes('href')).toBe(
      '#main-content',
    )
    expect(wrapper.get('#main-content').attributes('tabindex')).toBe('-1')
    expect(wrapper.get('nav[aria-label="主要导航"]').text()).toContain(
      '职位浏览',
    )
    expect(
      wrapper.get('.app-navigation-link').classes(),
    ).toContain('router-link-active')
  })

  it('renders unknown routes with the shared not-found state', async () => {
    const wrapper = await mountAt('/missing')

    expect(wrapper.get('[data-state="not-found"]').text()).toContain('404')
    expect(wrapper.text()).toContain('未找到请求的页面')
  })
})
