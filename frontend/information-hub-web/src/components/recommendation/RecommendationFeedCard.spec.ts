import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import RecommendationFeedCard from '@/components/recommendation/RecommendationFeedCard.vue'
import type { RecommendationFeedItem } from '@/types/recommendation'

const item: RecommendationFeedItem = {
  recommendationItemId: 1001,
  informationId: 88,
  snapshotId: 102,
  rank: 1,
  finalScore: 92.4,
  scoreBreakdown: {
    aiRelevanceScore: 95,
    profileMatchScore: 86,
    freshnessScore: 90,
  },
  reasons: ['AI 相关度高', '支持远程办公'],
  feedbackState: 'INTERESTED',
  jobDisposition: 'CONTACTED',
  viewed: true,
  job: {
    title: 'Java Backend Engineer',
    companyName: 'Example',
    salaryText: '20-35K',
    locationName: '成都',
    remoteType: 'REMOTE',
    sourceUrl: 'https://www.zhipin.com/job_detail/example.html',
  },
}

describe('RecommendationFeedCard', () => {
  it('shows explainability, CONTACTED state and a safe source link', () => {
    const wrapper = mount(RecommendationFeedCard, {
      props: { item, busy: false },
    })

    expect(wrapper.text()).toContain('92.4')
    expect(wrapper.text()).toContain('AI 相关度高')
    expect(wrapper.text()).toContain('已联系')
    const link = wrapper.get('a')
    expect(link.attributes('target')).toBe('_blank')
    expect(link.attributes('rel')).toBe('noopener noreferrer')
  })

  it('keeps feedback and disposition actions independent', async () => {
    const wrapper = mount(RecommendationFeedCard, {
      props: { item, busy: false },
    })
    const buttons = wrapper.findAll('button')
    await buttons.find((button) => button.text() === '取消感兴趣')!.trigger('click')
    await buttons.find((button) => button.text() === '取消已联系')!.trigger('click')

    expect(wrapper.emitted('feedback')?.[0]?.[1]).toBe('NONE')
    expect(wrapper.emitted('disposition')?.[0]?.[1]).toBe('NONE')
  })
})
