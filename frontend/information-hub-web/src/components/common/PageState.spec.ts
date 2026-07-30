import { mount, type MountingOptions } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { describe, expect, it } from 'vitest'

import PageState from './PageState.vue'

interface PageStateInput {
  kind: 'loading' | 'empty' | 'error' | 'not-found' | 'invalid'
  title: string
  description?: string
  rows?: number
}

function mountPageState(
  pageStateProps: PageStateInput,
  options: MountingOptions<Record<string, never>> = {},
) {
  const TestHost = defineComponent({
    components: { PageState },
    setup() {
      return { pageStateProps }
    },
    template: `
      <PageState v-bind="pageStateProps">
        <slot />
      </PageState>
    `,
  })

  return mount(TestHost, options)
}

describe('PageState', () => {
  it('announces loading without exposing the decorative skeleton', () => {
    const wrapper = mountPageState({
      kind: 'loading',
      title: '正在加载职位',
      rows: 3,
    })

    const state = wrapper.get('[data-state="loading"]')
    expect(state.attributes('role')).toBe('status')
    expect(state.attributes('aria-live')).toBe('polite')
    expect(state.attributes('aria-busy')).toBe('true')
    expect(state.attributes('aria-label')).toBe('正在加载职位')
    expect(wrapper.text()).toContain('正在加载职位')
    expect(wrapper.get('.el-skeleton').attributes('aria-hidden')).toBe('true')
  })

  it('uses an assertive alert for errors and renders recovery actions', () => {
    const wrapper = mountPageState(
      {
        kind: 'error',
        title: '加载失败',
        description: '请检查网络后重试',
      },
      {
        slots: {
          default: '<button type="button">重试</button>',
        },
      },
    )

    const state = wrapper.get('[data-state="error"]')
    expect(state.attributes('role')).toBe('alert')
    expect(state.attributes('aria-live')).toBe('assertive')
    expect(wrapper.text()).toContain('加载失败')
    expect(wrapper.text()).toContain('请检查网络后重试')
    expect(wrapper.get('button').text()).toBe('重试')
  })

  it('keeps empty and not-found feedback non-alarming', () => {
    const empty = mountPageState({
      kind: 'empty',
      title: '暂无数据',
    })
    const notFound = mountPageState({
      kind: 'not-found',
      title: '页面不存在',
    })

    expect(empty.get('[data-state="empty"]').attributes('role')).toBe(
      'status',
    )
    expect(
      notFound.get('[data-state="not-found"]').attributes('role'),
    ).toBe('status')
  })
})
