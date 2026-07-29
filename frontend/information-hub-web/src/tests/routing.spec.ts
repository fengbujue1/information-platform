import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory } from 'vue-router'

import App from '@/App.vue'
import { createAppRouter } from '@/router'

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
  it('renders the jobs placeholder at /jobs', async () => {
    const { wrapper } = await mountAt('/jobs')

    expect(wrapper.text()).toContain('职位浏览')
    expect(wrapper.text()).toContain('职位列表将在后续任务中实现')
  })

  it('redirects the root path to /jobs', async () => {
    const { router, wrapper } = await mountAt('/')

    expect(router.currentRoute.value.fullPath).toBe('/jobs')
    expect(wrapper.text()).toContain('职位浏览')
  })

  it('renders the 404 placeholder for an unknown path', async () => {
    const { wrapper } = await mountAt('/missing-page')

    expect(wrapper.text()).toContain('404')
    expect(wrapper.text()).toContain('未找到请求的页面')
  })
})
