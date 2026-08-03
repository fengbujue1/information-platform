import { expect, test } from '@playwright/test'

test('redirects anonymous users to login and restores the protected route', async ({
  page,
}) => {
  let authenticated = false
  let loginCsrfHeader: string | undefined

  await page.route('**/api/v1/auth/**', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname === '/api/v1/auth/me') {
      await route.fulfill({
        status: authenticated ? 200 : 401,
        contentType: 'application/json',
        body: JSON.stringify(
          authenticated
            ? {
                success: true,
                code: 'CURRENT_USER_FOUND',
                data: {
                  id: 1,
                  username: 'admin',
                  displayName: 'Admin',
                  timezone: 'Asia/Shanghai',
                },
              }
            : {
                success: false,
                code: 'AUTHENTICATION_REQUIRED',
                message: 'login required',
              },
        ),
      })
      return
    }
    if (url.pathname === '/api/v1/auth/csrf') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          code: 'CSRF_TOKEN_CREATED',
          data: {
            headerName: 'X-CSRF-TOKEN',
            parameterName: '_csrf',
            token: 'identity-e2e-token',
          },
        }),
      })
      return
    }
    if (url.pathname === '/api/v1/auth/login') {
      loginCsrfHeader = route.request().headers()['x-csrf-token']
      authenticated = true
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          code: 'LOGIN_SUCCEEDED',
          data: {
            id: 1,
            username: 'admin',
            displayName: 'Admin',
            timezone: 'Asia/Shanghai',
          },
        }),
      })
      return
    }
    await route.abort()
  })

  await page.route('**/api/v1/jobs**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        code: 'JOBS_FOUND',
        data: {
          page: 1,
          size: 20,
          total: 0,
          totalPages: 0,
          items: [],
        },
      }),
    })
  })

  await page.goto('/jobs?keyword=Java')
  await expect(page).toHaveURL(/\/login\?redirect=/)
  await page.getByLabel('用户名').fill('admin')
  await page.getByLabel('密码').fill('secret')
  await page.getByRole('button', { name: '登录' }).click()

  await expect(page).toHaveURL(/\/jobs\?keyword=Java/)
  await expect(page.getByRole('heading', { name: '职位浏览' })).toBeVisible()
  expect(loginCsrfHeader).toBe('identity-e2e-token')
  await expect(page.getByText('Admin', { exact: true })).toBeVisible()
})

test('redirects to login when a protected API reports an expired session', async ({
  page,
}) => {
  await page.route('**/api/v1/auth/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        code: 'CURRENT_USER_FOUND',
        data: {
          id: 1,
          username: 'admin',
          displayName: 'Admin',
          timezone: 'Asia/Shanghai',
        },
      }),
    })
  })
  await page.route('**/api/v1/jobs**', async (route) => {
    await route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({
        success: false,
        code: 'AUTHENTICATION_REQUIRED',
        message: 'login required',
      }),
    })
  })

  await page.goto('/jobs?keyword=Java')

  await expect(page).toHaveURL(/\/login\?redirect=/)
  await expect(
    page.getByRole('heading', { name: '登录 Information Hub' }),
  ).toBeVisible()
})

test('logs out with CSRF and clears the authenticated Web session', async ({
  page,
}) => {
  let logoutCsrfHeader: string | undefined

  await page.route('**/api/v1/auth/**', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname === '/api/v1/auth/me') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          code: 'CURRENT_USER_FOUND',
          data: {
            id: 1,
            username: 'admin',
            displayName: 'Admin',
            timezone: 'Asia/Shanghai',
          },
        }),
      })
      return
    }
    if (url.pathname === '/api/v1/auth/csrf') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          code: 'CSRF_TOKEN_CREATED',
          data: {
            headerName: 'X-CSRF-TOKEN',
            parameterName: '_csrf',
            token: 'logout-csrf-token',
          },
        }),
      })
      return
    }
    if (url.pathname === '/api/v1/auth/logout') {
      logoutCsrfHeader = route.request().headers()['x-csrf-token']
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          code: 'LOGOUT_SUCCEEDED',
          data: { loggedOut: true },
        }),
      })
      return
    }
    await route.abort()
  })
  await page.route('**/api/v1/jobs**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        code: 'JOBS_FOUND',
        data: {
          page: 1,
          size: 20,
          total: 0,
          totalPages: 0,
          items: [],
        },
      }),
    })
  })

  await page.goto('/jobs')
  await page.getByRole('button', { name: '退出登录' }).click()

  await expect(page).toHaveURL(/\/login$/)
  expect(logoutCsrfHeader).toBe('logout-csrf-token')
})
