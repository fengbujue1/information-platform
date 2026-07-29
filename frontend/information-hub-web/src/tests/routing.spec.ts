import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory } from 'vue-router'

import App from '@/App.vue'
import { createAppRouter } from '@/router'

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
  const router = createAppRouter(createMemoryHistory())
  await router.push(path)
  await router.isReady()

  const wrapper = mount(App, {
    global: {
      plugins: [router],
    },
  })

  return { router, wrapper }
}

describe('application routing', () => {
  it('renders the jobs list at /jobs', async () => {
    const { wrapper } = await mountAt('/jobs')

    expect(wrapper.text()).toContain('职位浏览')
    expect(wrapper.text()).toContain('筛选职位')
  })

  it('redirects the root path to /jobs', async () => {
    const { router, wrapper } = await mountAt('/')

    expect(router.currentRoute.value.fullPath).toBe('/jobs')
    expect(wrapper.text()).toContain('职位浏览')
  })

  it('renders the detail placeholder without loading detail data', async () => {
    const { wrapper } = await mountAt(
      '/jobs/7?from=%2Fjobs%3Fkeyword%3DJava',
    )

    expect(wrapper.text()).toContain('职位详情')
    expect(wrapper.text()).toContain('TASK-015')
    expect(getJobsMock).not.toHaveBeenCalled()
  })

  it('renders the 404 placeholder for an unknown path', async () => {
    const { wrapper } = await mountAt('/missing-page')

    expect(wrapper.text()).toContain('404')
    expect(wrapper.text()).toContain('未找到请求的页面')
  })
})
