import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { setAuthenticatedUserForTest } from '@/stores/authSession'
import SchedulesView from '@/views/SchedulesView.vue'

const listSchedulesMock = vi.hoisted(() => vi.fn())
const listProfilesMock = vi.hoisted(() => vi.fn())
const createScheduleMock = vi.hoisted(() => vi.fn())
const limitsMock = vi.hoisted(() => vi.fn())

vi.mock('@/api/scheduleApi', () => ({
  listAnalysisSchedules: listSchedulesMock,
  createAnalysisSchedule: createScheduleMock,
  updateAnalysisSchedule: vi.fn(),
  updateAnalysisScheduleStatus: vi.fn(),
  previewAnalysisSchedule: vi.fn(),
}))

vi.mock('@/api/batchApi', () => ({
  getAnalysisPreviewLimits: limitsMock,
}))

vi.mock('@/api/promptApi', () => ({
  listPromptProfiles: listProfilesMock,
}))

describe('SchedulesView', () => {
  beforeEach(() => {
    listSchedulesMock.mockReset()
    listProfilesMock.mockReset()
    createScheduleMock.mockReset()
    limitsMock.mockReset()
    limitsMock.mockResolvedValue({
      windowDays: { defaultValue: 5, minimum: 1, maximum: 30 },
      maxCandidates: { defaultValue: 80, minimum: 1, maximum: 100 },
      maxEstimatedTokens: {
        defaultValue: 400_000,
        minimum: 1,
        maximum: 500_000,
      },
    })
    listSchedulesMock.mockResolvedValue([])
    listProfilesMock.mockResolvedValue([
      {
        id: 11,
        name: 'Default',
        activeVersionId: 21,
        status: 'ACTIVE',
      },
    ])
    createScheduleMock.mockResolvedValue({ id: 31 })
    setAuthenticatedUserForTest({
      id: 1,
      username: 'admin',
      displayName: 'Admin',
      timezone: 'Asia/Shanghai',
    })
  })

  it('shows frozen default time and keeps a new Schedule disabled', async () => {
    const wrapper = mount(SchedulesView, {
      global: {
        stubs: { RouterLink: true },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('默认本地执行时间为 02:00')
    expect(wrapper.text()).toContain('保存配置不会隐式启用')

    const inputs = wrapper.findAll('input')
    const nameInput = inputs.find(
      (input) => input.attributes('maxlength') === '255',
    )
    expect(nameInput).toBeDefined()
    await nameInput!.setValue('每日分析')

    const saveButton = wrapper.findAll('button').find(
      (button) => button.text().includes('保存配置'),
    )
    await saveButton!.trigger('click')
    await flushPromises()

    expect(createScheduleMock).toHaveBeenCalledWith(
      expect.objectContaining({
        name: '每日分析',
        promptProfileId: 11,
        localTime: '02:00:00',
        timezone: 'Asia/Shanghai',
        windowDays: 5,
        maxCandidates: 80,
        maxEstimatedTokens: 400_000,
        enabled: false,
      }),
    )
  })

  it('keeps an over-limit saved schedule readable and blocks execution actions', async () => {
    listSchedulesMock.mockResolvedValue([
      {
        id: 31,
        name: '旧配置',
        promptProfileId: 11,
        enabled: false,
        localTime: '02:00:00',
        timezone: 'Asia/Shanghai',
        windowDays: 5,
        maxCandidates: 101,
        maxEstimatedTokens: 400_000,
        nextRunAt: null,
        lastRun: null,
      },
    ])

    const wrapper = mount(SchedulesView, {
      global: { stubs: { RouterLink: true } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('当前配置已超过平台限制')
    expect(wrapper.text()).toContain('最大候选数量需在 1 到 100 条之间')
    const previewButton = wrapper.findAll('button').find(
      (button) => button.text().includes('预览当前配置'),
    )
    expect(previewButton?.attributes('disabled')).toBeDefined()
  })
})
