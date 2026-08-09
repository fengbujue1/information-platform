import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import RecommendationsView from '@/views/RecommendationsView.vue'
import type { RecommendationFeed, RecommendationRun } from '@/types/recommendation'

const listPromptProfilesMock = vi.hoisted(() => vi.fn())
const getProfileMock = vi.hoisted(() => vi.fn())
const saveProfileMock = vi.hoisted(() => vi.fn())
const getFeedMock = vi.hoisted(() => vi.fn())
const refreshMock = vi.hoisted(() => vi.fn())
const getRunMock = vi.hoisted(() => vi.fn())
const listRunsMock = vi.hoisted(() => vi.fn())
const recordViewMock = vi.hoisted(() => vi.fn())
const updateFeedbackMock = vi.hoisted(() => vi.fn())
const updateDispositionMock = vi.hoisted(() => vi.fn())

vi.mock('@/api/promptApi', () => ({
  listPromptProfiles: listPromptProfilesMock,
}))

vi.mock('@/api/recommendationApi', () => ({
  getRecommendationProfile: getProfileMock,
  saveRecommendationProfile: saveProfileMock,
  getRecommendationFeed: getFeedMock,
  refreshRecommendations: refreshMock,
  getRecommendationRun: getRunMock,
  listRecommendationRuns: listRunsMock,
  recordRecommendationView: recordViewMock,
  updateRecommendationFeedback: updateFeedbackMock,
  updateJobDisposition: updateDispositionMock,
}))

const profile = {
  id: 7,
  informationType: 'JOB' as const,
  analysisPromptProfileId: 12,
  windowDays: 7,
  topN: 50,
  targetRoles: ['Java 后端'],
  preferredSkills: ['Java'],
  preferredCities: ['成都'],
  preferredRemoteTypes: ['REMOTE' as const],
  salaryMinMonthlyYuan: 15_000,
  excludedKeywords: ['纯销售'],
  contentHash: 'a'.repeat(64),
  createdAt: '2026-08-09T01:00:00',
  updatedAt: '2026-08-09T01:00:00',
}

const feedFixture: RecommendationFeed = {
  run: {
    id: 31,
    informationType: 'JOB',
    triggerType: 'MANUAL',
    completedAt: '2026-08-09T02:00:00Z',
    algorithmKey: 'JOB_RECOMMENDATION',
    algorithmVersion: 1,
    profileChangedSinceRun: true,
  },
  page: 1,
  pageSize: 20,
  total: 2,
  items: [
    {
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
      reasons: ['AI 相关度高'],
      feedbackState: 'NONE',
      jobDisposition: 'NONE',
      viewed: false,
      job: {
        title: 'Java Backend Engineer',
        companyName: 'Example',
        salaryText: '20-35K',
        locationName: '成都',
        remoteType: 'REMOTE',
        sourceUrl: 'https://example.test/jobs/88',
      },
    },
    {
      recommendationItemId: 1002,
      informationId: 89,
      snapshotId: 103,
      rank: 2,
      finalScore: 88,
      scoreBreakdown: {
        aiRelevanceScore: 90,
        profileMatchScore: 80,
        freshnessScore: 85,
      },
      reasons: ['符合目标岗位偏好'],
      feedbackState: 'INTERESTED',
      jobDisposition: 'CONTACTED',
      viewed: true,
      job: {
        title: 'Spring Boot Engineer',
        companyName: 'Contacted Co',
        salaryText: '18-30K',
        locationName: '成都',
        remoteType: 'HYBRID',
        sourceUrl: 'https://example.test/jobs/89',
      },
    },
  ],
}

function cloneFeed(): RecommendationFeed {
  return structuredClone(feedFixture)
}

function interaction(
  feedbackState: 'NONE' | 'INTERESTED' | 'NOT_INTERESTED',
  jobDisposition: 'NONE' | 'CONTACTED' | 'CONTACTED_NOT_SUITABLE',
) {
  return {
    informationId: 88,
    viewCount: 1,
    lastViewedAt: '2026-08-09T02:30:00',
    feedbackState,
    feedbackUpdatedAt: '2026-08-09T02:30:00',
    jobDisposition,
    dispositionUpdatedAt: '2026-08-09T02:30:00',
    lastRecommendationItemId: 1001,
    updatedAt: '2026-08-09T02:30:00',
  }
}

function completedRun(): RecommendationRun {
  return {
    id: 32,
    informationType: 'JOB',
    triggerType: 'MANUAL',
    sourceAnalysisBatchId: null,
    profileId: 7,
    profileContentHash: profile.contentHash,
    promptProfileId: 12,
    promptVersionId: 22,
    algorithmKey: 'JOB_RECOMMENDATION',
    algorithmVersion: 1,
    windowStart: '2026-08-02T02:00:00Z',
    windowEnd: '2026-08-09T02:00:00Z',
    candidateCount: 2,
    eligibleCount: 2,
    resultCount: 2,
    status: 'COMPLETED',
    skipReason: null,
    failureCode: null,
    failureMessage: null,
    startedAt: '2026-08-09T02:00:00Z',
    completedAt: '2026-08-09T02:00:01Z',
    createdAt: '2026-08-09T02:00:00Z',
    updatedAt: '2026-08-09T02:00:01Z',
  }
}

beforeEach(() => {
  listPromptProfilesMock.mockReset().mockResolvedValue([{
    id: 12,
    name: 'JOB relevance',
    analysisDefinitionKey: 'JOB_USER_RELEVANCE',
    activeVersionId: 22,
    status: 'ACTIVE',
    createdAt: '2026-08-01T00:00:00Z',
    updatedAt: '2026-08-01T00:00:00Z',
  }])
  getProfileMock.mockReset().mockResolvedValue(profile)
  saveProfileMock.mockReset().mockResolvedValue(profile)
  getFeedMock.mockReset().mockImplementation(async () => cloneFeed())
  refreshMock.mockReset().mockResolvedValue({ runId: 32, status: 'PENDING' })
  getRunMock.mockReset().mockResolvedValue(completedRun())
  listRunsMock.mockReset().mockResolvedValue([])
  recordViewMock.mockReset().mockResolvedValue(interaction('NONE', 'NONE'))
  updateFeedbackMock.mockReset().mockImplementation(
    async (_informationId: number, _itemId: number, state: 'NONE' | 'INTERESTED' | 'NOT_INTERESTED') =>
      interaction(state, 'NONE'),
  )
  updateDispositionMock.mockReset().mockImplementation(
    async (_informationId: number, _itemId: number, state: 'NONE' | 'CONTACTED' | 'CONTACTED_NOT_SUITABLE') =>
      interaction('NONE', state),
  )
})

afterEach(() => vi.useRealTimers())

describe('RecommendationsView', () => {
  it('renders profile, explainable feed, stale hint and CONTACTED without hiding it', async () => {
    const wrapper = mount(RecommendationsView)
    await flushPromises()

    expect(wrapper.text()).toContain('职位推荐')
    expect(wrapper.text()).toContain('推荐画像已在本轮推荐生成后修改')
    expect(wrapper.text()).toContain('Java Backend Engineer')
    expect(wrapper.text()).toContain('Spring Boot Engineer')
    expect(wrapper.text()).toContain('已联系')
    expect(wrapper.text()).toContain('AI 相关度高')
  })

  it('immediately hides hard exclusions and restores them through explicit NONE', async () => {
    const wrapper = mount(RecommendationsView)
    await flushPromises()

    const firstCard = wrapper.findAll('.recommendation-card')[0]!
    await firstCard.findAll('button').find((button) => button.text() === '不感兴趣')!.trigger('click')
    await flushPromises()

    expect(updateFeedbackMock).toHaveBeenCalledWith(88, 1001, 'NOT_INTERESTED')
    expect(wrapper.text()).not.toContain('Java Backend Engineer')
    expect(wrapper.text()).toContain('Spring Boot Engineer')

    await wrapper.findAll('button').find((button) => button.text() === '撤销并恢复')!.trigger('click')
    await flushPromises()

    expect(updateFeedbackMock).toHaveBeenLastCalledWith(88, 1001, 'NONE')
    expect(wrapper.text()).toContain('Java Backend Engineer')
  })

  it('polls an accepted manual Run and reloads the feed at terminal status', async () => {
    vi.useFakeTimers()
    const wrapper = mount(RecommendationsView)
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === '刷新推荐')!.trigger('click')
    await flushPromises()
    expect(refreshMock).toHaveBeenCalledOnce()

    await vi.advanceTimersByTimeAsync(800)
    await flushPromises()

    expect(getRunMock).toHaveBeenCalledWith(32)
    expect(getFeedMock).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Run #32 已完成')
    wrapper.unmount()
  })
})
