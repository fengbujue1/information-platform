import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import PromptProfilesView from '@/views/PromptProfilesView.vue'

const listProfilesMock = vi.hoisted(() => vi.fn())
const listVersionsMock = vi.hoisted(() => vi.fn())
const createVersionMock = vi.hoisted(() => vi.fn())
const activateVersionMock = vi.hoisted(() => vi.fn())

vi.mock('@/api/promptApi', () => ({
  listPromptProfiles: listProfilesMock,
  listPromptVersions: listVersionsMock,
  createPromptProfile: vi.fn(),
  createPromptVersion: createVersionMock,
  activatePromptVersion: activateVersionMock,
  updatePromptProfileStatus: vi.fn(),
}))

const profile = {
  id: 11,
  name: 'Default',
  analysisDefinitionKey: 'JOB_USER_RELEVANCE',
  activeVersionId: 21,
  status: 'ACTIVE',
  createdAt: '2026-08-01T00:00:00Z',
  updatedAt: '2026-08-01T00:00:00Z',
}

describe('PromptProfilesView', () => {
  beforeEach(() => {
    listProfilesMock.mockReset()
    listVersionsMock.mockReset()
    createVersionMock.mockReset()
    activateVersionMock.mockReset()
    listProfilesMock.mockResolvedValue([profile])
    listVersionsMock.mockResolvedValue([
      {
        id: 21,
        promptProfileId: 11,
        versionNo: 1,
        content: '关注 Java 后端职位',
        contentHash: 'a'.repeat(64),
        createdAt: '2026-08-01T00:00:00Z',
      },
    ])
    createVersionMock.mockResolvedValue({
      id: 22,
      promptProfileId: 11,
      versionNo: 2,
      content: '新增云原生要求',
      contentHash: 'b'.repeat(64),
      createdAt: '2026-08-02T00:00:00Z',
    })
    activateVersionMock.mockResolvedValue({
      ...profile,
      activeVersionId: 22,
    })
  })

  it('presents immutable history and creates a new active Version', async () => {
    const wrapper = mount(PromptProfilesView)
    await flushPromises()

    expect(wrapper.text()).toContain('历史正文不会被覆盖')
    expect(wrapper.text()).toContain('Version 1')
    expect(wrapper.text()).toContain('关注 Java 后端职位')

    await wrapper.get('textarea').setValue('新增云原生要求')
    const createButton = wrapper.findAll('button').find(
      (button) => button.text().includes('创建并激活新 Version'),
    )
    await createButton!.trigger('click')
    await flushPromises()

    expect(createVersionMock).toHaveBeenCalledWith(
      11,
      { content: '新增云原生要求' },
    )
    expect(activateVersionMock).toHaveBeenCalledWith(11, 22)
  })
})
