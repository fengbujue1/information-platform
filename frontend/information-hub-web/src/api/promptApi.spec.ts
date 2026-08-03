import AxiosMockAdapter from 'axios-mock-adapter'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'

import { clearCsrfToken } from '@/api/authApi'
import { httpClient } from '@/api/httpClient'
import {
  activatePromptVersion,
  createPromptProfile,
  createPromptVersion,
  getPromptProfile,
  listPromptProfiles,
  listPromptVersions,
  updatePromptProfileStatus,
} from '@/api/promptApi'

const profile = {
  id: 10,
  name: 'Default',
  analysisDefinitionKey: 'JOB_USER_RELEVANCE',
  activeVersionId: 20,
  status: 'ACTIVE',
  createdAt: '2026-08-03T10:00:00Z',
  updatedAt: '2026-08-03T10:00:00Z',
}

const version = {
  id: 20,
  promptProfileId: 10,
  versionNo: 1,
  content: '关注 Java 后端岗位',
  contentHash: 'a'.repeat(64),
  createdAt: '2026-08-03T10:00:00Z',
}

describe('promptApi', () => {
  let mock: AxiosMockAdapter

  beforeEach(() => {
    mock = new AxiosMockAdapter(httpClient)
    clearCsrfToken()
    mock.onGet('/v1/auth/csrf').reply(200, {
      success: true,
      code: 'CSRF_TOKEN_CREATED',
      data: {
        headerName: 'X-CSRF-TOKEN',
        parameterName: '_csrf',
        token: 'prompt-csrf-token',
      },
    })
  })

  afterEach(() => {
    mock.restore()
    clearCsrfToken()
  })

  it('reads Profile and Version contract endpoints', async () => {
    mock.onGet('/v1/ai/prompt-profiles').reply(200, {
      success: true,
      code: 'PROMPT_PROFILES_FOUND',
      data: [profile],
    })
    mock.onGet('/v1/ai/prompt-profiles/10').reply(200, {
      success: true,
      code: 'PROMPT_PROFILE_FOUND',
      data: profile,
    })
    mock.onGet('/v1/ai/prompt-profiles/10/versions').reply(200, {
      success: true,
      code: 'PROMPT_VERSIONS_FOUND',
      data: [version],
    })

    await expect(listPromptProfiles()).resolves.toHaveLength(1)
    await expect(getPromptProfile(10)).resolves.toMatchObject({ id: 10 })
    await expect(listPromptVersions(10)).resolves.toMatchObject([
      { versionNo: 1 },
    ])
  })

  it('protects every write endpoint with the negotiated CSRF header', async () => {
    const assertCsrf = (config: { headers?: Record<string, unknown> }) => {
      expect(config.headers?.['X-CSRF-TOKEN']).toBe('prompt-csrf-token')
    }
    mock.onPost('/v1/ai/prompt-profiles').reply((config) => {
      assertCsrf(config)
      expect(JSON.parse(config.data)).not.toHaveProperty('userId')
      return [200, {
        success: true,
        code: 'PROMPT_PROFILE_CREATED',
        data: profile,
      }]
    })
    mock.onPost('/v1/ai/prompt-profiles/10/versions').reply((config) => {
      assertCsrf(config)
      return [200, {
        success: true,
        code: 'PROMPT_VERSION_CREATED',
        data: version,
      }]
    })
    mock.onPut('/v1/ai/prompt-profiles/10/active-version').reply((config) => {
      assertCsrf(config)
      return [200, {
        success: true,
        code: 'PROMPT_ACTIVE_VERSION_UPDATED',
        data: profile,
      }]
    })
    mock.onPut('/v1/ai/prompt-profiles/10/status').reply((config) => {
      assertCsrf(config)
      return [200, {
        success: true,
        code: 'PROMPT_PROFILE_STATUS_UPDATED',
        data: { ...profile, status: 'DISABLED' },
      }]
    })

    await createPromptProfile({
      name: 'Default',
      analysisDefinitionKey: 'JOB_USER_RELEVANCE',
    })
    await createPromptVersion(10, { content: '关注 Java 后端岗位' })
    await activatePromptVersion(10, 20)
    await updatePromptProfileStatus(10, 'DISABLED')
  })
})
