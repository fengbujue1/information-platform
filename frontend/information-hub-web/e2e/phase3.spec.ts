import { expect, test, type Page } from '@playwright/test'

const E2E_USERNAME = requiredEnvironment('INFORMATION_HUB_E2E_USERNAME')
const E2E_PASSWORD = requiredEnvironment('INFORMATION_HUB_E2E_PASSWORD')
const COLLECTOR_TOKEN = requiredEnvironment(
  'INFORMATION_HUB_E2E_COLLECTOR_TOKEN',
)
const RAW_PAYLOAD_MARKER = 'PHASE3_E2E_PRIVATE_RAW_MARKER'
const FORBIDDEN_PROVIDER_KEY = requiredEnvironment(
  'INFORMATION_HUB_E2E_FORBIDDEN_PROVIDER_KEY',
)
const VALID_USAGE_TOTAL = 150
const BACKEND_URL = requiredEnvironment('INFORMATION_HUB_E2E_BACKEND_URL')
const FAILED_USAGE_TOTAL = 18

interface ApiEnvelope<T> {
  success: boolean
  code: string
  data: T
}

interface IngestionResult {
  informationId: number
}

interface PromptProfile {
  id: number
  name: string
}

interface AnalysisResult {
  status: string
  estimatedTotalTokens: number
  invocations: Array<{
    usageStatus: string
    totalTokens: number | null
  }>
}

interface AnalysisPreview {
  eligibleCount: number
  selectedCount: number
  deferredByItemLimitCount: number
  deferredByTokenBudgetCount: number
  estimatedTotalTokens: number
}

interface AnalysisBatch {
  id: number
  status: string
  estimatedTotalTokens: number
  progress: {
    actualTotalTokens: number | null
  }
}

interface AnalysisSchedule {
  id: number
  name: string
  promptProfileId: number
  enabled: boolean
  nextRunAt: string | null
  lastRun: {
    batchId: number
    status: string
    skipReason: string | null
  } | null
}

interface AnalysisUsage {
  allTime: {
    invocationCount: number
    reportedInvocationCount: number
    unavailableInvocationCount: number
    actualTotalTokens: number | null
  }
}

test('runs the complete Phase 3 flow through the real Web and backend', async ({
  page,
}, testInfo) => {
  test.setTimeout(90_000)
  const runId = `${Date.now()}-${testInfo.workerIndex}`
  const observedResponseBodies: string[] = []

  page.on('response', async (response) => {
    if (!response.url().includes('/api/')) return
    try {
      observedResponseBodies.push(await response.text())
    } catch {
      // Navigation cancellation can make a response body unavailable.
    }
  })

  const informationIds = await ingestJobs(page, runId, 3)
  await login(page)
  const baselineUsage = await apiGet<AnalysisUsage>(
    page,
    '/api/v1/ai/usage',
  )

  const successfulProfile = await createPromptProfile(
    page,
    `Phase 3 E2E Success ${runId}`,
    '优先匹配 Java、Spring Boot 和 MySQL 后端职位。',
  )

  const successfulAnalysis = await executeSingleAnalysis(
    page,
    informationIds[0]!,
    successfulProfile.id,
  )
  expect(successfulAnalysis.status).toBe('SUCCEEDED')
  expect(successfulAnalysis.estimatedTotalTokens).toBeGreaterThan(0)
  expect(successfulAnalysis.invocations).toHaveLength(1)
  expect(successfulAnalysis.invocations[0]).toMatchObject({
    usageStatus: 'REPORTED',
    totalTokens: VALID_USAGE_TOTAL,
  })
  await expect(page.getByText('SUCCEEDED', { exact: true })).toBeVisible()
  await expect(page.getByText(String(VALID_USAGE_TOTAL), { exact: true }))
    .toBeVisible()

  const previewResponsePromise = page.waitForResponse(
    (response) =>
      response.url().endsWith('/api/v1/ai/analysis-batches/preview') &&
      response.request().method() === 'POST',
  )
  await page.goto('/ai/analyze')
  await page.getByRole('spinbutton', { name: /Max Candidates/ }).fill('1')
  await page.getByRole('button', { name: 'Preview', exact: true }).click()
  const preview = await unwrapResponse<AnalysisPreview>(
    await previewResponsePromise,
  )
  expect(preview.eligibleCount).toBeGreaterThanOrEqual(2)
  expect(preview.selectedCount).toBe(1)
  expect(preview.deferredByItemLimitCount).toBeGreaterThanOrEqual(1)
  expect(preview.estimatedTotalTokens).toBeGreaterThan(0)
  await expect(page.getByText('Preview 没有 Actual Token。')).toBeVisible()

  await page.getByRole('spinbutton', { name: /Max Candidates/ }).fill('2')
  await page.getByRole('spinbutton', {
    name: /Estimated Token Budget/,
  }).fill(String(preview.estimatedTotalTokens))
  const budgetPreviewResponsePromise = page.waitForResponse(
    (response) =>
      response.url().endsWith('/api/v1/ai/analysis-batches/preview') &&
      response.request().method() === 'POST',
  )
  await page.getByRole('button', { name: 'Preview', exact: true }).click()
  const budgetPreview = await unwrapResponse<AnalysisPreview>(
    await budgetPreviewResponsePromise,
  )
  expect(budgetPreview.eligibleCount).toBeGreaterThanOrEqual(2)
  expect(budgetPreview.selectedCount).toBe(1)
  expect(budgetPreview.deferredByItemLimitCount).toBeGreaterThanOrEqual(1)
  expect(budgetPreview.deferredByTokenBudgetCount).toBeGreaterThanOrEqual(1)
  expect(budgetPreview.estimatedTotalTokens).toBe(
    preview.estimatedTotalTokens,
  )

  const confirmResponsePromise = page.waitForResponse(
    (response) =>
      response.url().endsWith('/api/v1/ai/analysis-batches/confirm') &&
      response.request().method() === 'POST',
  )
  await page.getByRole('button', { name: '确认并创建 Batch' }).click()
  const confirmedBatch = await unwrapResponse<AnalysisBatch>(
    await confirmResponsePromise,
  )
  const completedBatch = await pollApi<AnalysisBatch>(
    page,
    `/api/v1/ai/analysis-batches/${confirmedBatch.id}`,
    (batch) => ['COMPLETED', 'PARTIAL_FAILED', 'FAILED', 'NOOP']
      .includes(batch.status),
  )
  expect(completedBatch.status).toBe('COMPLETED')
  expect(completedBatch.progress.actualTotalTokens).toBe(VALID_USAGE_TOTAL)
  expect(completedBatch.estimatedTotalTokens).toBeGreaterThan(0)
  await page.reload()
  await expect(page.getByText('COMPLETED', { exact: true })).toBeVisible()

  const failedProfile = await createPromptProfile(
    page,
    `Phase 3 E2E Failure ${runId}`,
    'E2E_FORCE_INVALID_OUTPUT：验证失败输出仍保留 Provider Usage。',
  )
  const failedAnalysis = await executeSingleAnalysis(
    page,
    informationIds[0]!,
    failedProfile.id,
  )
  expect(failedAnalysis.status).toBe('FAILED')
  expect(failedAnalysis.invocations).toHaveLength(1)
  expect(failedAnalysis.invocations[0]).toMatchObject({
    usageStatus: 'REPORTED',
    totalTokens: FAILED_USAGE_TOTAL,
  })
  await expect(page.getByText('FAILED', { exact: true })).toBeVisible()

  const scheduleJobId = (await ingestJobs(page, `${runId}-schedule`, 1))[0]!
  expect(scheduleJobId).toBeGreaterThan(0)
  const schedule = await createDisabledSchedule(
    page,
    successfulProfile.id,
    `Phase 3 E2E Schedule ${runId}`,
  )
  expect(schedule.enabled).toBe(false)
  expect(schedule.nextRunAt).toBeNull()

  let scheduledBatch: AnalysisBatch | null = null
  try {
    await configureAndEnableDueSchedule(page, schedule)
    const dispatched = await pollApi<AnalysisSchedule>(
      page,
      `/api/v1/ai/analysis-schedules/${schedule.id}`,
      (value) => value.lastRun !== null,
      30_000,
    )
    expect(dispatched.lastRun?.skipReason).toBeNull()
    scheduledBatch = await pollApi<AnalysisBatch>(
      page,
      `/api/v1/ai/analysis-batches/${dispatched.lastRun!.batchId}`,
      (batch) => ['COMPLETED', 'PARTIAL_FAILED', 'FAILED', 'NOOP']
        .includes(batch.status),
      30_000,
    )
    expect(scheduledBatch.status).toBe('COMPLETED')
    expect(scheduledBatch.progress.actualTotalTokens).toBe(VALID_USAGE_TOTAL)
  } finally {
    await setScheduleEnabled(page, schedule.id, false)
  }

  const disabledSchedule = await apiGet<AnalysisSchedule>(
    page,
    `/api/v1/ai/analysis-schedules/${schedule.id}`,
  )
  expect(disabledSchedule.enabled).toBe(false)
  expect(disabledSchedule.nextRunAt).toBeNull()

  await page.goto('/ai/usage')
  const usage = await apiGet<AnalysisUsage>(page, '/api/v1/ai/usage')
  const expectedActualTotal =
    (baselineUsage.allTime.actualTotalTokens ?? 0) +
    VALID_USAGE_TOTAL * 3 +
    FAILED_USAGE_TOTAL
  expect(usage.allTime).toMatchObject({
    invocationCount: baselineUsage.allTime.invocationCount + 4,
    reportedInvocationCount:
      baselineUsage.allTime.reportedInvocationCount + 4,
    unavailableInvocationCount:
      baselineUsage.allTime.unavailableInvocationCount,
    actualTotalTokens: expectedActualTotal,
  })
  await expect(page.getByText(String(expectedActualTotal), { exact: true }))
    .toHaveCount(3)

  const observed = observedResponseBodies.join('\n')
  expect(observed).not.toContain(RAW_PAYLOAD_MARKER)
  expect(observed).not.toContain('rawPayload')
  expect(observed).not.toContain(FORBIDDEN_PROVIDER_KEY)
  expect(await page.locator('body').innerText()).not.toContain(
    RAW_PAYLOAD_MARKER,
  )

  const evidence = {
    singleEstimated: successfulAnalysis.estimatedTotalTokens,
    singleActual: VALID_USAGE_TOTAL,
    batchEstimated: completedBatch.estimatedTotalTokens,
    batchActual: completedBatch.progress.actualTotalTokens,
    failedActual: FAILED_USAGE_TOTAL,
    scheduledActual: scheduledBatch?.progress.actualTotalTokens,
    userActualTotal: usage.allTime.actualTotalTokens,
    baselineUserActualTotal: baselineUsage.allTime.actualTotalTokens,
    runActualTotal: VALID_USAGE_TOTAL * 3 + FAILED_USAGE_TOTAL,
  }
  console.log(`PHASE3_E2E_EVIDENCE ${JSON.stringify(evidence)}`)
  await testInfo.attach('phase3-token-evidence.json', {
    body: JSON.stringify(evidence, null, 2),
    contentType: 'application/json',
  })
})

async function login(page: Page): Promise<void> {
  await page.goto('/login')
  await page.getByLabel('用户名').fill(E2E_USERNAME)
  await page.getByLabel('密码').fill(E2E_PASSWORD)
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/jobs$/)
}

async function createPromptProfile(
  page: Page,
  name: string,
  content: string,
): Promise<PromptProfile> {
  await page.goto('/ai/prompts')
  await page.getByPlaceholder('例如：Java 后端职位').fill(name)
  const profileResponsePromise = page.waitForResponse(
    (response) =>
      response.url().endsWith('/api/v1/ai/prompt-profiles') &&
      response.request().method() === 'POST',
  )
  await page.getByRole('button', { name: '创建', exact: true }).click()
  const profile = await unwrapResponse<PromptProfile>(
    await profileResponsePromise,
  )

  await page.getByLabel('创建新 Version').fill(content)
  await page.getByRole('button', { name: '创建并激活新 Version' }).click()
  await expect(page.getByText('Active', { exact: true })).toBeVisible()
  return profile
}

async function executeSingleAnalysis(
  page: Page,
  informationId: number,
  promptProfileId: number,
): Promise<AnalysisResult> {
  await page.goto(`/ai/analyze?informationId=${informationId}`)
  const profiles = await apiGet<PromptProfile[]>(
    page,
    '/api/v1/ai/prompt-profiles',
  )
  const requestedProfile = profiles.find(
    (profile) => profile.id === promptProfileId,
  )
  expect(requestedProfile).toBeDefined()
  await page.locator('.ai-form-field')
    .filter({ hasText: 'Prompt Profile' })
    .locator('.el-select__wrapper')
    .click()
  await page.getByRole('option', { name: requestedProfile!.name }).click()

  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().endsWith('/api/v1/ai/analyses') &&
      response.request().method() === 'POST',
  )
  await page.getByRole('button', { name: '执行单条分析' }).click()
  const result = await unwrapResponse<AnalysisResult>(await responsePromise)
  expect(result).toBeDefined()
  return result
}

async function createDisabledSchedule(
  page: Page,
  promptProfileId: number,
  name: string,
): Promise<AnalysisSchedule> {
  await page.goto('/ai/schedules')
  await page.getByRole('textbox', { name: '名称', exact: true }).fill(name)
  const profiles = await apiGet<PromptProfile[]>(
    page,
    '/api/v1/ai/prompt-profiles',
  )
  const requestedProfile = profiles.find(
    (profile) => profile.id === promptProfileId,
  )
  expect(requestedProfile).toBeDefined()
  await page.locator('.ai-form-field')
    .filter({ hasText: 'Prompt Profile' })
    .locator('.el-select__wrapper')
    .click()
  await page.getByRole('option', { name: requestedProfile!.name }).click()
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().endsWith('/api/v1/ai/analysis-schedules') &&
      response.request().method() === 'POST',
  )
  await page.getByRole('button', { name: '保存配置' }).click()
  const schedule = await unwrapResponse<AnalysisSchedule>(
    await responsePromise,
  )
  expect(schedule.promptProfileId).toBe(promptProfileId)
  await expect(
    page.getByText(name).locator('..').getByText('关闭', { exact: true }),
  ).toBeVisible()
  return schedule
}

async function configureAndEnableDueSchedule(
  page: Page,
  schedule: AnalysisSchedule,
): Promise<void> {
  const due = new Date(Date.now() + 8_000)
  const localTime = due.toISOString().slice(11, 19)
  const headers = await csrfHeaders(page)
  await apiPut(
    page,
    `/api/v1/ai/analysis-schedules/${schedule.id}`,
    {
      name: schedule.name,
      promptProfileId: schedule.promptProfileId,
      localTime,
      timezone: 'UTC',
      windowDays: 3,
      maxCandidates: 1,
      maxEstimatedTokens: 75_000,
    },
    headers,
  )
  const enabled = await apiPut<AnalysisSchedule>(
    page,
    `/api/v1/ai/analysis-schedules/${schedule.id}/status`,
    { enabled: true },
    await csrfHeaders(page),
  )
  expect(enabled.enabled).toBe(true)
  expect(enabled.nextRunAt).not.toBeNull()
}

async function setScheduleEnabled(
  page: Page,
  scheduleId: number,
  enabled: boolean,
): Promise<void> {
  await apiPut(
    page,
    `/api/v1/ai/analysis-schedules/${scheduleId}/status`,
    { enabled },
    await csrfHeaders(page),
  )
}

async function ingestJobs(
  page: Page,
  runId: string,
  count: number,
): Promise<number[]> {
  const ids: number[] = []
  for (let index = 1; index <= count; index += 1) {
    const response = await page.request.post(`${BACKEND_URL}/api/v1/collector/items`, {
      headers: { Authorization: `Bearer ${COLLECTOR_TOKEN}` },
      data: {
        schemaVersion: 1,
        informationType: 'JOB',
        source: 'BOSS',
        sourceItemId: `${runId}-${index}`,
        sourceUrl: `https://example.test/phase3-e2e/${runId}/${index}`,
        title: `Phase 3 E2E Java Engineer ${index}`,
        content: 'Build Java 21, Spring Boot and MySQL information services.',
        collectedAt: new Date().toISOString(),
        collector: {
          collectorId: 'phase3-e2e',
          collectorVersion: '1.0',
        },
        collectionContext: { runId },
        extension: {
          companyName: 'Synthetic E2E Company',
          locationName: 'Shanghai',
          cityName: 'Shanghai',
          experienceText: '3-5 years',
          educationText: 'Bachelor',
          remoteType: 'HYBRID',
          jobStatus: 'ACTIVE',
          detailStatus: 'FETCHED',
          sourceTags: ['Java', 'Backend'],
          sourceSkillTags: ['Spring Boot', 'MySQL'],
          welfare: ['Synthetic fixture only'],
        },
        rawPayload: {
          privateMarker: RAW_PAYLOAD_MARKER,
          sourceItemId: `${runId}-${index}`,
        },
      },
    })
    const result = await unwrapResponse<IngestionResult>(response)
    ids.push(result.informationId)
  }
  return ids
}

async function csrfHeaders(page: Page): Promise<Record<string, string>> {
  const response = await page.request.get('/api/v1/auth/csrf')
  const csrf = await unwrapResponse<{
    headerName: string
    token: string
  }>(response)
  return { [csrf.headerName]: csrf.token }
}

async function apiGet<T>(page: Page, path: string): Promise<T> {
  return unwrapResponse<T>(await page.request.get(path))
}

async function apiPut<T>(
  page: Page,
  path: string,
  data: unknown,
  headers: Record<string, string>,
): Promise<T> {
  return unwrapResponse<T>(
    await page.request.put(path, { data, headers }),
  )
}

async function pollApi<T>(
  page: Page,
  path: string,
  completed: (value: T) => boolean,
  timeoutMs = 20_000,
): Promise<T> {
  const deadline = Date.now() + timeoutMs
  let latest: T | undefined
  while (Date.now() < deadline) {
    latest = await apiGet<T>(page, path)
    if (completed(latest)) return latest
    await new Promise((resolve) => setTimeout(resolve, 300))
  }
  throw new Error(
    `Timed out waiting for ${path}; latest=${JSON.stringify(latest)}`,
  )
}

async function unwrapResponse<T>(
  response: { ok(): boolean; status(): number; json(): Promise<unknown> },
): Promise<T> {
  const body = (await response.json()) as ApiEnvelope<T>
  if (!response.ok() || body.success !== true) {
    throw new Error(
      `API request failed with ${response.status()} / ${body.code}`,
    )
  }
  return body.data
}

function requiredEnvironment(name: string): string {
  const value = process.env[name]
  if (!value?.trim()) {
    throw new Error(`${name} is required for Phase 3 E2E`)
  }
  return value
}
