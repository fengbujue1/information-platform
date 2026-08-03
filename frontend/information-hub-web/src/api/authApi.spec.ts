import AxiosMockAdapter from 'axios-mock-adapter'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'

import {
  clearCsrfToken,
  getCurrentUser,
  login,
  logout,
} from '@/api/authApi'
import { httpClient } from '@/api/httpClient'

describe('authApi', () => {
  let mock: AxiosMockAdapter

  beforeEach(() => {
    mock = new AxiosMockAdapter(httpClient)
    clearCsrfToken()
  })

  afterEach(() => {
    mock.restore()
    clearCsrfToken()
  })

  it('initializes CSRF before login and sends the negotiated header', async () => {
    mock.onGet('/v1/auth/csrf').reply(200, {
      success: true,
      code: 'CSRF_TOKEN_CREATED',
      data: {
        headerName: 'X-CSRF-TOKEN',
        parameterName: '_csrf',
        token: 'csrf-token',
      },
    })
    mock.onPost('/v1/auth/login').reply((config) => {
      expect(config.headers?.['X-CSRF-TOKEN']).toBe('csrf-token')
      return [
        200,
        {
          success: true,
          code: 'LOGIN_SUCCEEDED',
          data: {
            id: 1,
            username: 'admin',
            displayName: 'Admin',
            timezone: 'Asia/Shanghai',
          },
        },
      ]
    })

    await expect(
      login({ username: 'admin', password: 'secret' }),
    ).resolves.toMatchObject({ id: 1, username: 'admin' })
  })

  it('loads current user and protects logout with CSRF', async () => {
    mock.onGet('/v1/auth/me').reply(200, {
      success: true,
      code: 'CURRENT_USER_FOUND',
      data: {
        id: 1,
        username: 'admin',
        displayName: null,
        timezone: 'Asia/Shanghai',
      },
    })
    mock.onGet('/v1/auth/csrf').reply(200, {
      success: true,
      code: 'CSRF_TOKEN_CREATED',
      data: {
        headerName: 'X-CSRF-TOKEN',
        parameterName: '_csrf',
        token: 'csrf-token',
      },
    })
    mock.onPost('/v1/auth/logout').reply((config) => {
      expect(config.headers?.['X-CSRF-TOKEN']).toBe('csrf-token')
      return [
        200,
        {
          success: true,
          code: 'LOGOUT_SUCCEEDED',
          data: { loggedOut: true },
        },
      ]
    })

    await expect(getCurrentUser()).resolves.toMatchObject({
      username: 'admin',
    })
    await expect(logout()).resolves.toBeUndefined()
  })
})
