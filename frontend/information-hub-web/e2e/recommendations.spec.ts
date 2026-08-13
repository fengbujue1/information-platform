import { expect, test } from '@playwright/test'

import {
  installRecommendationApiMock,
  type RecommendationApiMockState,
} from './fixtures/recommendationApiFixtures'

let apiState: RecommendationApiMockState

test.beforeEach(async ({ page }) => {
  apiState = await installRecommendationApiMock(page)
})

test('completes the Profile, Feed, Refresh, feedback and contact workflow', async ({
  page,
}) => {
  await page.goto('/recommendations')

  await expect(page.getByRole('heading', { name: '职位推荐' })).toBeVisible()
  await expect(page.getByLabel('提示词方案')).toBeVisible()
  await expect(page.getByText('推荐画像已在本轮推荐生成后修改')).toBeVisible()
  await expect(page.getByText('Java Backend Engineer')).toBeVisible()
  await expect(page.getByText('Spring Boot Engineer')).toBeVisible()
  await expect(page.getByText('已联系', { exact: true })).toBeVisible()

  const sourceLink = page.getByRole('link', { name: '打开来源 / BOSS' }).first()
  await expect(sourceLink).toHaveAttribute('rel', 'noopener noreferrer')
  await expect(sourceLink).toHaveAttribute('target', '_blank')
  await sourceLink.evaluate((element) => {
    element.addEventListener('click', (event) => event.preventDefault(), {
      once: true,
    })
  })
  await sourceLink.click()
  await expect(page.locator('.recommendation-card').first().getByText('已查看')).toBeVisible()

  await page.getByRole('button', { name: '保存推荐画像' }).click()
  await expect(page.getByText(/推荐画像已保存/)).toBeVisible()
  expect(apiState.profileSaveCount).toBe(1)

  const firstCard = page.locator('.recommendation-card').filter({
    hasText: 'Java Backend Engineer',
  })
  await firstCard.getByRole('button', { name: '不感兴趣' }).click()
  await expect(page.getByText('Java Backend Engineer')).toBeHidden()
  await expect(page.getByText('Spring Boot Engineer')).toBeVisible()

  await page.getByRole('button', { name: '撤销并恢复' }).click()
  await expect(page.getByText('Java Backend Engineer')).toBeVisible()

  const restoredCard = page.locator('.recommendation-card').filter({
    hasText: 'Java Backend Engineer',
  })
  await restoredCard.getByRole('button', { name: '标记已联系' }).click()
  await expect(restoredCard.getByText('已联系', { exact: true })).toBeVisible()
  await expect(page.getByText('Java Backend Engineer')).toBeVisible()

  await restoredCard.getByRole('button', { name: '已联系且不合适' }).click()
  await expect(page.getByText('Java Backend Engineer')).toBeHidden()

  await page.getByRole('button', { name: '刷新推荐' }).click()
  await expect(page.getByText(/Run #32 已完成/)).toBeVisible({ timeout: 5_000 })
  expect(apiState.refreshCount).toBe(1)
  expect(apiState.interactionCsrfHeaders.every(
    (header) => header === 'recommendation-e2e-token',
  )).toBe(true)
})

test('shows a retryable error state when the Feed cannot be loaded', async ({
  page,
}) => {
  apiState.failFeed = true
  await page.goto('/recommendations')

  await expect(page.getByText('职位推荐加载失败', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '重试' })).toBeVisible()
})
