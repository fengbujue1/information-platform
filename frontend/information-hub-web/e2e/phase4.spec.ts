import { expect, test, type Locator, type Page } from '@playwright/test'

const E2E_USERNAME = requiredEnvironment('INFORMATION_HUB_E2E_USERNAME')
const E2E_PASSWORD = requiredEnvironment('INFORMATION_HUB_E2E_PASSWORD')
const COLLECTOR_TOKEN = requiredEnvironment(
  'INFORMATION_HUB_E2E_COLLECTOR_TOKEN',
)
const BACKEND_URL = requiredEnvironment('INFORMATION_HUB_E2E_BACKEND_URL')
const PROVIDER_STATS_URL = requiredEnvironment(
  'INFORMATION_HUB_E2E_PROVIDER_STATS_URL',
)
const FORBIDDEN_PROVIDER_KEY = requiredEnvironment(
  'INFORMATION_HUB_E2E_FORBIDDEN_PROVIDER_KEY',
)
const RAW_PAYLOAD_MARKER = requiredEnvironment(
  'INFORMATION_HUB_E2E_RAW_PAYLOAD_MARKER',
)

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
  activeVersionId: number | null
}

interface PromptVersion {
  id: number
}

interface RecommendationProfile {
  id: number
  analysisPromptProfileId: number
  topN: number
  contentHash: string
}

interface AnalysisPreview {
  selectedCount: number
  previewToken: string
}

interface AnalysisBatch {
  id: number
  status: string
  progress: {
    succeededCount: number
    failedCount: number
  }
}

interface RecommendationRun {
  id: number
  triggerType: string
  sourceAnalysisBatchId: number | null
  status: string
  resultCount: number
}

interface RecommendationFeedItem {
  recommendationItemId: number
  informationId: number
  finalScore: number
  reasons: string[]
  feedbackState: string
  jobDisposition: string
  viewed: boolean
  job: {
    title: string
  }
}

interface RecommendationFeed {
  run: {
    id: number
    profileChangedSinceRun: boolean
  } | null
  total: number
  items: RecommendationFeedItem[]
}

interface Interaction {
  feedbackState: string
  jobDisposition: string
}

interface ProviderStats {
  requestCount: number
}

test('validates the complete Phase 4 recommendation lifecycle', async ({
  page,
}, testInfo) => {
  test.setTimeout(120_000)
  const runKey = `${Date.now()}-${testInfo.workerIndex}`
  const observedResponseBodies: string[] = []

  page.on('response', async (response) => {
    if (!response.url().includes('/api/')) return
    try {
      observedResponseBodies.push(await response.text())
    } catch {
      // Navigation cancellation can make a response body unavailable.
    }
  })

  await login(page)
  const prompt = await createPromptProfile(page, runKey)
  let recommendationProfile = await saveRecommendationProfile(
    page,
    prompt.id,
    10,
  )

  const completedInformation = await ingestJobs(page, runKey, [
    { suffix: 'completed-a', title: `Phase 4 Java Platform ${runKey}` },
    { suffix: 'completed-b', title: `Phase 4 Spring Service ${runKey}` },
  ])
  const completedBatch = await createAndCompleteBatch(page, prompt.id, 2)
  expect(completedBatch.status).toBe('COMPLETED')
  expect(completedBatch.progress).toMatchObject({
    succeededCount: 2,
    failedCount: 0,
  })
  const completedAutoRun = await waitForAutoRun(page, completedBatch.id)
  expect(completedAutoRun.status).toBe('COMPLETED')
  expect(completedAutoRun.resultCount).toBeGreaterThanOrEqual(2)

  const partialInformation = await ingestJobs(page, runKey, [
    { suffix: 'partial-ok', title: `Phase 4 Cloud Engineer ${runKey}` },
    {
      suffix: 'partial-failed',
      title: `Phase 4 Invalid Analysis ${runKey}`,
      contentMarker: 'E2E_FORCE_INVALID_OUTPUT',
    },
  ])
  const partialBatch = await createAndCompleteBatch(page, prompt.id, 2)
  expect(partialBatch.status).toBe('PARTIAL_FAILED')
  expect(partialBatch.progress).toMatchObject({
    succeededCount: 1,
    failedCount: 1,
  })
  const partialAutoRun = await waitForAutoRun(page, partialBatch.id)
  expect(partialAutoRun.status).toBe('COMPLETED')

  const providerCountAfterAnalysis = (await providerStats(page)).requestCount
  expect(providerCountAfterAnalysis).toBe(4)

  let feed = await getFeed(page)
  expect(feed.run?.id).toBe(partialAutoRun.id)
  expect(feed.total).toBeGreaterThanOrEqual(3)
  expect(feed.items.every((item) => item.finalScore >= 0)).toBe(true)
  expect(feed.items.every((item) => item.reasons.length > 0)).toBe(true)
  expect(feed.items.some(
    (item) => item.informationId === partialInformation[1]!.informationId,
  )).toBe(false)

  await page.goto('/recommendations')
  await expect(page.getByRole('heading', { name: '职位推荐' })).toBeVisible()
  await expect(page.getByText(completedInformation[0]!.title)).toBeVisible()
  await expect(page.getByLabel('评分明细').first()).toBeVisible()

  const firstInformationId = completedInformation[0]!.informationId
  const secondInformationId = completedInformation[1]!.informationId
  let firstItem = requireFeedItem(feed, firstInformationId)
  let secondItem = requireFeedItem(feed, secondInformationId)

  const firstCard = page.locator('.recommendation-card').filter({
    hasText: completedInformation[0]!.title,
  })
  const sourceLink = firstCard.getByRole('link', { name: '打开来源 / BOSS' })
  await sourceLink.evaluate((element) => {
    element.addEventListener('click', (event) => event.preventDefault(), {
      once: true,
    })
  })
  await clickAndWaitForApi(
    page,
    sourceLink,
    `/api/v1/recommendation/interactions/${firstInformationId}/view`,
  )
  await expect(firstCard.getByText('已查看', { exact: true })).toBeVisible()

  await clickAndWaitForApi(
    page,
    firstCard.getByRole('button', { name: '感兴趣', exact: true }),
    `/api/v1/recommendation/interactions/${firstInformationId}/feedback`,
  )
  await expect(firstCard.getByText('感兴趣', { exact: true })).toBeVisible()
  feed = await getFeed(page)
  expect(requireFeedItem(feed, firstInformationId).feedbackState).toBe('INTERESTED')

  const secondCard = page.locator('.recommendation-card').filter({
    hasText: completedInformation[1]!.title,
  })
  await clickAndWaitForApi(
    page,
    secondCard.getByRole('button', { name: '标记已联系' }),
    `/api/v1/recommendation/interactions/${secondInformationId}/job-disposition`,
  )
  await expect(secondCard.getByText('已联系', { exact: true })).toBeVisible()
  await expect(page.getByText(completedInformation[1]!.title)).toBeVisible()
  feed = await getFeed(page)
  expect(requireFeedItem(feed, secondInformationId).jobDisposition).toBe('CONTACTED')

  await clickAndWaitForApi(
    page,
    firstCard.getByRole('button', { name: '不感兴趣' }),
    `/api/v1/recommendation/interactions/${firstInformationId}/feedback`,
  )
  await expect(page.getByText(completedInformation[0]!.title)).toBeHidden()
  await clickAndWaitForApi(
    page,
    page.getByRole('button', { name: '撤销并恢复' }),
    `/api/v1/recommendation/interactions/${firstInformationId}/feedback`,
  )
  await expect(page.getByText(completedInformation[0]!.title)).toBeVisible()

  feed = await getFeed(page)
  firstItem = requireFeedItem(feed, firstInformationId)
  await updateFeedback(page, firstItem, 'NOT_INTERESTED')
  expect((await getFeed(page)).items.some(
    (item) => item.informationId === firstInformationId,
  )).toBe(false)
  await runManualRefresh(page)
  expect((await getFeed(page)).items.some(
    (item) => item.informationId === firstInformationId,
  )).toBe(false)
  await updateFeedback(page, firstItem, 'NONE')
  await runManualRefresh(page)
  feed = await getFeed(page)
  expect(feed.items.some((item) => item.informationId === firstInformationId)).toBe(true)

  secondItem = requireFeedItem(feed, secondInformationId)
  await updateDisposition(page, secondItem, 'CONTACTED_NOT_SUITABLE')
  expect((await getFeed(page)).items.some(
    (item) => item.informationId === secondInformationId,
  )).toBe(false)
  await runManualRefresh(page)
  expect((await getFeed(page)).items.some(
    (item) => item.informationId === secondInformationId,
  )).toBe(false)
  await updateDisposition(page, secondItem, 'NONE')
  await runManualRefresh(page)
  feed = await getFeed(page)
  expect(feed.items.some((item) => item.informationId === secondInformationId)).toBe(true)

  const runsBeforeProfileChange = await listRuns(page)
  recommendationProfile = await saveRecommendationProfile(
    page,
    prompt.id,
    recommendationProfile.topN - 1,
  )
  expect((await getFeed(page)).run?.profileChangedSinceRun).toBe(true)
  await delay(500)
  expect((await listRuns(page)).length).toBe(runsBeforeProfileChange.length)

  await page.reload()
  await expect(page.getByText('推荐画像已在本轮推荐生成后修改')).toBeVisible()
  const finalManualRun = await runManualRefresh(page)
  expect(finalManualRun.status).toBe('COMPLETED')
  expect((await getFeed(page)).run?.profileChangedSinceRun).toBe(false)
  expect((await providerStats(page)).requestCount).toBe(providerCountAfterAnalysis)

  const observed = observedResponseBodies.join('\n')
  expect(observed).not.toContain(RAW_PAYLOAD_MARKER)
  expect(observed).not.toContain(FORBIDDEN_PROVIDER_KEY)
  expect(await page.locator('body').innerText()).not.toContain(RAW_PAYLOAD_MARKER)

  const evidence = {
    completedBatchId: completedBatch.id,
    completedAutoRunId: completedAutoRun.id,
    partialBatchId: partialBatch.id,
    partialAutoRunId: partialAutoRun.id,
    finalManualRunId: finalManualRun.id,
    providerRequestCount: providerCountAfterAnalysis,
    finalFeedTotal: (await getFeed(page)).total,
    profileContentHash: recommendationProfile.contentHash,
  }
  console.log(`PHASE4_E2E_EVIDENCE ${JSON.stringify(evidence)}`)
  await testInfo.attach('phase4-e2e-evidence.json', {
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

async function clickAndWaitForApi(
  page: Page,
  target: Locator,
  path: string,
): Promise<void> {
  const [response] = await Promise.all([
    page.waitForResponse((candidate) => {
      const method = candidate.request().method()
      return new URL(candidate.url()).pathname === path
        && (method === 'POST' || method === 'PUT')
    }),
    target.click(),
  ])
  expect(response.ok()).toBe(true)
}

async function createPromptProfile(
  page: Page,
  runKey: string,
): Promise<PromptProfile> {
  const profile = await apiPost<PromptProfile>(
    page,
    '/api/v1/ai/prompt-profiles',
    {
      name: `Phase 4 E2E ${runKey}`,
      analysisDefinitionKey: 'JOB_USER_RELEVANCE',
    },
  )
  const version = await apiPost<PromptVersion>(
    page,
    `/api/v1/ai/prompt-profiles/${profile.id}/versions`,
    { content: '优先 Java、Spring Boot、MySQL 和云平台岗位。' },
  )
  return apiPut<PromptProfile>(
    page,
    `/api/v1/ai/prompt-profiles/${profile.id}/active-version`,
    { versionId: version.id },
  )
}

async function saveRecommendationProfile(
  page: Page,
  promptProfileId: number,
  topN: number,
): Promise<RecommendationProfile> {
  return apiPut(page, '/api/v1/recommendation/profiles/JOB', {
    analysisPromptProfileId: promptProfileId,
    windowDays: 1,
    topN,
    targetRoles: [],
    preferredSkills: [],
    preferredCities: [],
    preferredRemoteTypes: [],
    salaryMinMonthlyYuan: null,
    excludedKeywords: [],
  })
}

interface JobInput {
  suffix: string
  title: string
  contentMarker?: string
}

interface IngestedJob extends JobInput {
  informationId: number
}

async function ingestJobs(
  page: Page,
  runKey: string,
  jobs: JobInput[],
): Promise<IngestedJob[]> {
  const ingested: IngestedJob[] = []
  for (const [index, job] of jobs.entries()) {
    const result = await unwrapResponse<IngestionResult>(
      await page.request.post(`${BACKEND_URL}/api/v1/collector/items`, {
        headers: { Authorization: `Bearer ${COLLECTOR_TOKEN}` },
        data: {
          schemaVersion: 1,
          informationType: 'JOB',
          source: 'BOSS',
          sourceItemId: `${runKey}-${job.suffix}`,
          sourceUrl: `https://example.test/phase4-e2e/${runKey}/${job.suffix}`,
          title: job.title,
          content: `Java 21 Spring Boot MySQL role ${job.contentMarker ?? ''}`,
          collectedAt: new Date().toISOString(),
          collector: {
            collectorId: 'phase4-e2e',
            collectorVersion: '1.0',
          },
          collectionContext: { runKey },
          extension: {
            companyName: `Phase 4 Company ${runKey} ${index}`,
            salaryText: '20-35K',
            salaryMinMonthlyYuan: 20_000,
            salaryMaxMonthlyYuan: 35_000,
            locationName: '成都',
            cityName: '成都',
            remoteType: index % 2 === 0 ? 'REMOTE' : 'HYBRID',
            jobStatus: 'ACTIVE',
            detailStatus: 'FETCHED',
            sourceTags: ['Java', 'Backend'],
            sourceSkillTags: ['Spring Boot', 'MySQL'],
            welfare: ['Synthetic fixture only'],
          },
          rawPayload: {
            privateMarker: RAW_PAYLOAD_MARKER,
            sourceItemId: `${runKey}-${job.suffix}`,
          },
        },
      }),
    )
    ingested.push({ ...job, informationId: result.informationId })
  }
  return ingested
}

async function createAndCompleteBatch(
  page: Page,
  promptProfileId: number,
  maxCandidates: number,
): Promise<AnalysisBatch> {
  const preview = await apiPost<AnalysisPreview>(
    page,
    '/api/v1/ai/analysis-batches/preview',
    {
      promptProfileId,
      windowDays: 1,
      maxCandidates,
      maxEstimatedTokens: 200_000,
    },
  )
  expect(preview.selectedCount).toBe(maxCandidates)
  const accepted = await apiPost<AnalysisBatch>(
    page,
    '/api/v1/ai/analysis-batches/confirm',
    { previewToken: preview.previewToken },
  )
  return pollApi(
    page,
    `/api/v1/ai/analysis-batches/${accepted.id}`,
    (batch: AnalysisBatch) => [
      'COMPLETED',
      'PARTIAL_FAILED',
      'FAILED',
      'NOOP',
    ].includes(batch.status),
    30_000,
  )
}

async function waitForAutoRun(
  page: Page,
  batchId: number,
): Promise<RecommendationRun> {
  return pollValue(async () => {
    const run = (await listRuns(page)).find(
      (candidate) => candidate.sourceAnalysisBatchId === batchId,
    )
    if (!run || !['COMPLETED', 'FAILED', 'NOOP'].includes(run.status)) {
      return null
    }
    return run
  }, 30_000, `Auto Recommendation Run for Batch #${batchId}`)
}

async function runManualRefresh(page: Page): Promise<RecommendationRun> {
  const accepted = await apiPost<{ runId: number; status: string }>(
    page,
    '/api/v1/recommendations/JOB/refresh',
    undefined,
  )
  return pollApi(
    page,
    `/api/v1/recommendations/JOB/runs/${accepted.runId}`,
    (run: RecommendationRun) => ['COMPLETED', 'FAILED', 'NOOP'].includes(run.status),
    30_000,
  )
}

async function listRuns(page: Page): Promise<RecommendationRun[]> {
  return apiGet(page, '/api/v1/recommendations/JOB/runs?limit=100')
}

async function getFeed(page: Page): Promise<RecommendationFeed> {
  return apiGet(page, '/api/v1/recommendations/JOB/feed?page=1&pageSize=100')
}

function requireFeedItem(
  feed: RecommendationFeed,
  informationId: number,
): RecommendationFeedItem {
  const item = feed.items.find((candidate) => candidate.informationId === informationId)
  expect(item, `Feed item for Information #${informationId}`).toBeDefined()
  return item!
}

async function updateFeedback(
  page: Page,
  item: RecommendationFeedItem,
  feedbackState: string,
): Promise<Interaction> {
  return apiPut(
    page,
    `/api/v1/recommendation/interactions/${item.informationId}/feedback`,
    { feedbackState, recommendationItemId: item.recommendationItemId },
  )
}

async function updateDisposition(
  page: Page,
  item: RecommendationFeedItem,
  jobDisposition: string,
): Promise<Interaction> {
  return apiPut(
    page,
    `/api/v1/recommendation/interactions/${item.informationId}/job-disposition`,
    { jobDisposition, recommendationItemId: item.recommendationItemId },
  )
}

async function providerStats(page: Page): Promise<ProviderStats> {
  const response = await page.request.get(PROVIDER_STATS_URL)
  expect(response.ok()).toBe(true)
  return response.json() as Promise<ProviderStats>
}

async function csrfHeaders(page: Page): Promise<Record<string, string>> {
  const response = await page.request.get('/api/v1/auth/csrf')
  const csrf = await unwrapResponse<{ headerName: string; token: string }>(response)
  return { [csrf.headerName]: csrf.token }
}

async function apiGet<T>(page: Page, path: string): Promise<T> {
  return unwrapResponse<T>(await page.request.get(path))
}

async function apiPost<T>(
  page: Page,
  path: string,
  data: unknown,
): Promise<T> {
  return unwrapResponse<T>(await page.request.post(path, {
    data,
    headers: await csrfHeaders(page),
  }))
}

async function apiPut<T>(
  page: Page,
  path: string,
  data: unknown,
): Promise<T> {
  return unwrapResponse<T>(await page.request.put(path, {
    data,
    headers: await csrfHeaders(page),
  }))
}

async function pollApi<T>(
  page: Page,
  path: string,
  completed: (value: T) => boolean,
  timeoutMs: number,
): Promise<T> {
  return pollValue(async () => {
    const value = await apiGet<T>(page, path)
    return completed(value) ? value : null
  }, timeoutMs, path)
}

async function pollValue<T>(
  read: () => Promise<T | null>,
  timeoutMs: number,
  description: string,
): Promise<T> {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    const value = await read()
    if (value !== null) return value
    await delay(250)
  }
  throw new Error(`Timed out waiting for ${description}`)
}

async function unwrapResponse<T>(
  response: { ok(): boolean; status(): number; json(): Promise<unknown> },
): Promise<T> {
  const body = (await response.json()) as ApiEnvelope<T>
  if (!response.ok() || body.success !== true) {
    throw new Error(`API request failed with ${response.status()} / ${body.code}`)
  }
  return body.data
}

function requiredEnvironment(name: string): string {
  const value = process.env[name]
  if (!value?.trim()) throw new Error(`${name} is required for Phase 4 E2E`)
  return value
}

function delay(milliseconds: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, milliseconds))
}
