import { expect, test } from '@playwright/test'

import {
  installJobApiMock,
  type JobApiMockState,
} from './fixtures/jobApiFixtures'

let apiState: JobApiMockState

test.beforeEach(async ({ page }) => {
  apiState = await installJobApiMock(page)
})

test('loads jobs and keeps filters, sorting and pagination in the URL', async ({
  page,
}) => {
  await page.goto('/jobs')

  await expect(
    page.getByRole('heading', { name: '职位浏览' }),
  ).toBeVisible()
  await expect(
    page.locator('.job-table').getByRole('button', {
      name: '高级 Java 工程师',
    }),
  ).toBeVisible()

  await page.getByLabel('关键词').fill('平台')
  await page.getByLabel('城市').fill('上海')

  const sortField = page
    .locator('.filter-field')
    .filter({ hasText: '排序字段' })
    .locator('.el-select__wrapper')
  await sortField.click()
  await page.getByRole('option', { name: '最低月薪' }).click()

  const sortDirection = page
    .locator('.filter-field')
    .filter({ hasText: '排序方向' })
    .locator('.el-select__wrapper')
  await sortDirection.click()
  await page.getByRole('option', { name: '升序' }).click()

  await page.getByRole('button', { name: '搜索' }).click()

  await expect(page).toHaveURL(/keyword=%E5%B9%B3%E5%8F%B0/)
  await expect(page).toHaveURL(/city=%E4%B8%8A%E6%B5%B7/)
  await expect(page).toHaveURL(/sortBy=salaryMinMonthlyYuan/)
  await expect(page).toHaveURL(/sortDirection=asc/)
  await expect(
    page.locator('.job-table').getByRole('button', {
      name: '平台开发工程师',
    }),
  ).toBeVisible()

  const lastFilteredRequest = apiState.listRequests.at(-1)
  expect(lastFilteredRequest?.searchParams.get('keyword')).toBe('平台')
  expect(lastFilteredRequest?.searchParams.get('sortBy')).toBe(
    'salaryMinMonthlyYuan',
  )

  await page.goto(
    '/jobs?page=2&size=20&sortBy=salaryMinMonthlyYuan&sortDirection=asc',
  )
  await expect(
    page.locator('.job-table').getByRole('button', {
      name: '第二页测试职位',
    }),
  ).toBeVisible()
  expect(apiState.listRequests.at(-1)?.searchParams.get('page')).toBe('2')
})

test('navigates from list to detail and snapshots, including deep refresh', async ({
  page,
}) => {
  await page.goto('/jobs?keyword=Java&page=1&size=20')

  await page
    .locator('.job-table')
    .getByRole('button', { name: '高级 Java 工程师' })
    .click()

  await expect(page).toHaveURL(/\/jobs\/1001\?from=/)
  await expect(
    page.getByRole('heading', { name: '高级 Java 工程师' }),
  ).toBeVisible()

  const sourceLink = page.getByRole('link', { name: '查看来源职位' })
  await expect(sourceLink).toHaveAttribute(
    'href',
    'https://example.test/jobs/1001',
  )
  await expect(sourceLink).toHaveAttribute('rel', 'noopener noreferrer')

  await page.getByRole('link', { name: '查看历史快照' }).click()
  await expect(page).toHaveURL(/\/jobs\/1001\/snapshots\?from=/)
  await expect(
    page.getByRole('heading', { name: '历史快照' }),
  ).toBeVisible()
  await expect(page.getByText('第二版职位描述')).toBeVisible()

  await page.locator('[data-test="snapshot-option-1"]').click()
  await expect(page.getByText('第一版职位描述')).toBeVisible()

  await page.reload()
  await expect(
    page.getByRole('heading', { name: '历史快照' }),
  ).toBeVisible()
  await expect(page.getByText('第二版职位描述')).toBeVisible()
  expect(apiState.detailRequests).toHaveLength(1)
  expect(apiState.snapshotRequests).toHaveLength(2)
  await expect(page.locator('body')).not.toContainText('rawPayload')
  await expect(page.locator('body')).not.toContainText('Collector Token')
})

test('shows empty, API error and unknown-route states', async ({ page }) => {
  await page.goto('/jobs?keyword=%E7%A9%BA%E7%BB%93%E6%9E%9C')
  await expect(
    page.getByText('没有符合条件的职位', { exact: true }),
  ).toBeVisible()

  await page.goto('/jobs?keyword=%E6%8E%A5%E5%8F%A3%E9%94%99%E8%AF%AF')
  await expect(
    page.getByText('职位列表加载失败', { exact: true }),
  ).toBeVisible()
  await expect(page.getByRole('button', { name: '重试' })).toBeVisible()

  await page.goto('/does-not-exist')
  await expect(page.getByText('404', { exact: true })).toBeVisible()
  await expect(
    page.getByRole('button', { name: '返回职位入口' }),
  ).toBeVisible()
})
