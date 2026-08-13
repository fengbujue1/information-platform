<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElCard,
  ElPagination,
  ElTag,
} from 'element-plus'

import { ApiClientError, toApiClientError } from '@/api/apiError'
import { listPromptProfiles } from '@/api/promptApi'
import {
  getRecommendationFeed,
  getRecommendationProfile,
  getRecommendationRun,
  listRecommendationRuns,
  recordRecommendationView,
  refreshRecommendations,
  saveRecommendationProfile,
  updateJobDisposition,
  updateRecommendationFeedback,
} from '@/api/recommendationApi'
import PageState from '@/components/common/PageState.vue'
import RecommendationFeedCard from '@/components/recommendation/RecommendationFeedCard.vue'
import RecommendationProfileForm from '@/components/recommendation/RecommendationProfileForm.vue'
import type { PromptProfile } from '@/types/prompt'
import type {
  JobDisposition,
  RecommendationFeed,
  RecommendationFeedItem,
  RecommendationFeedbackState,
  RecommendationProfile,
  RecommendationProfileRequest,
  RecommendationRun,
  RecommendationRunStatus,
} from '@/types/recommendation'
import { formatDateTime } from '@/utils/formatters'

const POLL_INTERVAL_MS = 800
const TERMINAL_STATUSES: ReadonlySet<RecommendationRunStatus> = new Set([
  'COMPLETED',
  'FAILED',
  'NOOP',
])

interface UndoAction {
  item: RecommendationFeedItem
  kind: 'feedback' | 'disposition'
}

const promptProfiles = ref<PromptProfile[]>([])
const profile = ref<RecommendationProfile | null>(null)
const feed = ref<RecommendationFeed | null>(null)
const runs = ref<RecommendationRun[]>([])
const activeRun = ref<RecommendationRun | null>(null)
const page = ref(1)
const pageSize = ref(20)
const loading = ref(true)
const feedLoading = ref(false)
const savingProfile = ref(false)
const refreshing = ref(false)
const busyItemId = ref<number | null>(null)
const loadError = ref<string | null>(null)
const actionError = ref<string | null>(null)
const notice = ref<string | null>(null)
const undoAction = ref<UndoAction | null>(null)
let pollTimer: ReturnType<typeof setTimeout> | null = null
let pollGeneration = 0
let disposed = false

const hasProfile = computed(() => profile.value !== null)
const isRunActive = computed(
  () => activeRun.value?.status === 'PENDING' || activeRun.value?.status === 'RUNNING',
)

function isProfileMissing(cause: unknown): boolean {
  return cause instanceof ApiClientError && cause.code === 'RECOMMENDATION_PROFILE_NOT_FOUND'
}

async function loadInitialState(): Promise<void> {
  loading.value = true
  loadError.value = null
  try {
    const [profilesResult, profileResult, feedResult, runsResult] = await Promise.allSettled([
      listPromptProfiles(),
      getRecommendationProfile(),
      getRecommendationFeed(page.value, pageSize.value),
      listRecommendationRuns(),
    ])

    if (profilesResult.status === 'rejected') throw profilesResult.reason
    promptProfiles.value = profilesResult.value.filter(
      (candidate) => candidate.analysisDefinitionKey === 'JOB_USER_RELEVANCE',
    )

    if (profileResult.status === 'fulfilled') {
      profile.value = profileResult.value
    } else if (!isProfileMissing(profileResult.reason)) {
      throw profileResult.reason
    }

    if (feedResult.status === 'rejected') throw feedResult.reason
    feed.value = feedResult.value

    if (runsResult.status === 'rejected') throw runsResult.reason
    runs.value = runsResult.value
    const running = runs.value.find(
      (run) => run.status === 'PENDING' || run.status === 'RUNNING',
    )
    if (running) startPolling(running)
  } catch (cause) {
    loadError.value = toApiClientError(cause).message
  } finally {
    loading.value = false
  }
}

async function loadFeed(): Promise<void> {
  feedLoading.value = true
  actionError.value = null
  try {
    feed.value = await getRecommendationFeed(page.value, pageSize.value)
  } catch (cause) {
    actionError.value = toApiClientError(cause).message
  } finally {
    feedLoading.value = false
  }
}

async function saveProfile(request: RecommendationProfileRequest): Promise<void> {
  savingProfile.value = true
  actionError.value = null
  notice.value = null
  try {
    profile.value = await saveRecommendationProfile(request)
    notice.value = '推荐画像已保存；下一次自动或手动刷新时生效。'
    await loadFeed()
  } catch (cause) {
    actionError.value = toApiClientError(cause).message
  } finally {
    savingProfile.value = false
  }
}

async function requestRefresh(): Promise<void> {
  refreshing.value = true
  actionError.value = null
  notice.value = null
  try {
    const accepted = await refreshRecommendations()
    const pending = createPendingRun(accepted.runId)
    notice.value = `推荐刷新已提交（Run #${accepted.runId}）。`
    startPolling(pending)
  } catch (cause) {
    actionError.value = toApiClientError(cause).message
  } finally {
    refreshing.value = false
  }
}

function createPendingRun(runId: number): RecommendationRun {
  const now = new Date().toISOString()
  return {
    id: runId,
    informationType: 'JOB',
    triggerType: 'MANUAL',
    sourceAnalysisBatchId: null,
    profileId: profile.value?.id ?? 0,
    profileContentHash: profile.value?.contentHash ?? '',
    promptProfileId: profile.value?.analysisPromptProfileId ?? 0,
    promptVersionId: 0,
    algorithmKey: 'JOB_RECOMMENDATION',
    algorithmVersion: 1,
    windowStart: now,
    windowEnd: now,
    candidateCount: 0,
    eligibleCount: 0,
    resultCount: 0,
    status: 'PENDING',
    skipReason: null,
    failureCode: null,
    failureMessage: null,
    startedAt: null,
    completedAt: null,
    createdAt: now,
    updatedAt: now,
  }
}

function startPolling(run: RecommendationRun): void {
  stopPolling()
  activeRun.value = run
  const generation = ++pollGeneration
  pollTimer = setTimeout(() => void pollRun(run.id, generation), POLL_INTERVAL_MS)
}

async function pollRun(runId: number, generation: number): Promise<void> {
  if (disposed || generation !== pollGeneration) return
  try {
    const current = await getRecommendationRun(runId)
    if (disposed || generation !== pollGeneration) return
    activeRun.value = current
    if (TERMINAL_STATUSES.has(current.status)) {
      pollTimer = null
      runs.value = await listRecommendationRuns()
      await loadFeed()
      notice.value = current.status === 'COMPLETED'
        ? `Run #${current.id} 已完成，推荐列表已更新。`
        : current.status === 'NOOP'
          ? `Run #${current.id} 未产生新推荐，当前成功 Feed 保持不变。`
          : null
      if (current.status === 'FAILED') {
        actionError.value = current.failureMessage || '推荐刷新失败，旧的成功 Feed 仍可继续查看。'
      }
      return
    }
    pollTimer = setTimeout(() => void pollRun(runId, generation), POLL_INTERVAL_MS)
  } catch (cause) {
    if (disposed || generation !== pollGeneration) return
    pollTimer = null
    actionError.value = toApiClientError(cause).message
  }
}

function stopPolling(): void {
  pollGeneration += 1
  if (pollTimer !== null) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
}

async function changeFeedback(
  item: RecommendationFeedItem,
  state: RecommendationFeedbackState,
): Promise<void> {
  busyItemId.value = item.recommendationItemId
  actionError.value = null
  try {
    const interaction = await updateRecommendationFeedback(
      item.informationId,
      item.recommendationItemId,
      state,
    )
    if (state === 'NOT_INTERESTED') {
      removeWithUndo(item, 'feedback')
    } else {
      item.feedbackState = interaction.feedbackState
    }
  } catch (cause) {
    actionError.value = toApiClientError(cause).message
  } finally {
    busyItemId.value = null
  }
}

async function changeDisposition(
  item: RecommendationFeedItem,
  state: JobDisposition,
): Promise<void> {
  busyItemId.value = item.recommendationItemId
  actionError.value = null
  try {
    const interaction = await updateJobDisposition(
      item.informationId,
      item.recommendationItemId,
      state,
    )
    if (state === 'CONTACTED_NOT_SUITABLE') {
      removeWithUndo(item, 'disposition')
    } else {
      item.jobDisposition = interaction.jobDisposition
    }
  } catch (cause) {
    actionError.value = toApiClientError(cause).message
  } finally {
    busyItemId.value = null
  }
}

function removeWithUndo(
  item: RecommendationFeedItem,
  kind: UndoAction['kind'],
): void {
  if (!feed.value) return
  feed.value.items = feed.value.items.filter(
    (candidate) => candidate.recommendationItemId !== item.recommendationItemId,
  )
  feed.value.total = Math.max(0, feed.value.total - 1)
  undoAction.value = { item, kind }
  notice.value = kind === 'feedback'
    ? '已标记为不感兴趣，该职位已从当前 Feed 隐藏。'
    : '已标记为已联系且不合适，该职位已从当前 Feed 隐藏。'
}

async function undoHardExclusion(): Promise<void> {
  const action = undoAction.value
  if (!action) return
  busyItemId.value = action.item.recommendationItemId
  actionError.value = null
  try {
    if (action.kind === 'feedback') {
      await updateRecommendationFeedback(
        action.item.informationId,
        action.item.recommendationItemId,
        'NONE',
      )
    } else {
      await updateJobDisposition(
        action.item.informationId,
        action.item.recommendationItemId,
        'NONE',
      )
    }
    undoAction.value = null
    notice.value = '状态已恢复为 NONE；仍属于当前成功 Run 的职位会重新显示。'
    await loadFeed()
  } catch (cause) {
    actionError.value = toApiClientError(cause).message
  } finally {
    busyItemId.value = null
  }
}

async function recordView(item: RecommendationFeedItem): Promise<void> {
  try {
    await recordRecommendationView(item.informationId, item.recommendationItemId)
    item.viewed = true
  } catch (cause) {
    actionError.value = toApiClientError(cause).message
  }
}

async function changePage(nextPage: number): Promise<void> {
  page.value = nextPage
  await loadFeed()
}

function runTagType(status: RecommendationRunStatus): 'success' | 'danger' | 'warning' | 'info' {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'PENDING' || status === 'RUNNING') return 'warning'
  return 'info'
}

onMounted(loadInitialState)
onBeforeUnmount(() => {
  disposed = true
  stopPolling()
})
</script>

<template>
  <section class="recommendation-page">
    <header class="recommendation-page-header">
      <div>
        <h1>职位推荐</h1>
        <p>基于已完成的相关性分析生成可解释的预计算推荐。</p>
      </div>
      <ElButton
        type="primary"
        :loading="refreshing"
        :disabled="!hasProfile || isRunActive"
        @click="requestRefresh"
      >
        {{ isRunActive ? '刷新运行中' : '刷新推荐' }}
      </ElButton>
    </header>

    <PageState
      v-if="loading"
      kind="loading"
      title="正在加载职位推荐"
    />
    <PageState
      v-else-if="loadError"
      kind="error"
      title="职位推荐加载失败"
      :description="loadError"
    >
      <ElButton type="primary" @click="loadInitialState">重试</ElButton>
    </PageState>

    <template v-else>
      <ElAlert
        v-if="actionError"
        class="recommendation-alert"
        type="error"
        :title="actionError"
        show-icon
        @close="actionError = null"
      />
      <ElAlert
        v-if="notice"
        class="recommendation-alert"
        type="success"
        :title="notice"
        show-icon
        @close="notice = null"
      />
      <div v-if="undoAction" class="recommendation-undo" role="status">
        <span>需要恢复刚才隐藏的职位？</span>
        <ElButton link type="primary" @click="undoHardExclusion">
          撤销并恢复
        </ElButton>
      </div>

      <ElCard class="recommendation-section" shadow="never">
        <template #header>
          <div class="recommendation-section-heading">
            <div>
              <h2>推荐画像</h2>
              <p>配置职位推荐偏好，并绑定已有的提示词方案。</p>
            </div>
            <ElTag :type="hasProfile ? 'success' : 'info'">
              {{ hasProfile ? '已配置' : '未配置' }}
            </ElTag>
          </div>
        </template>
        <RecommendationProfileForm
          :profile="profile"
          :prompt-profiles="promptProfiles"
          :saving="savingProfile"
          @save="saveProfile"
        />
      </ElCard>

      <ElAlert
        v-if="feed?.run?.profileChangedSinceRun"
        class="recommendation-alert"
        type="warning"
        title="推荐画像已在本轮推荐生成后修改"
        description="当前仍展示最近一次成功 Run；请手动刷新，让新画像在下一轮生效。"
        show-icon
        :closable="false"
      />

      <ElCard
        v-if="activeRun || runs.length > 0"
        class="recommendation-run-panel"
        shadow="never"
      >
        <div class="recommendation-run-summary">
          <div>
            <strong>最近 Run #{{ (activeRun || runs[0])?.id }}</strong>
            <span>
              {{ (activeRun || runs[0])?.triggerType }} ·
              {{ formatDateTime((activeRun || runs[0])?.createdAt ?? null) }}
            </span>
          </div>
          <ElTag :type="runTagType((activeRun || runs[0])!.status)">
            {{ (activeRun || runs[0])?.status }}
          </ElTag>
        </div>
      </ElCard>

      <section class="recommendation-feed-section" aria-labelledby="recommendation-feed-title">
        <div class="recommendation-section-heading">
          <div>
            <h2 id="recommendation-feed-title">推荐 Feed</h2>
            <p v-if="feed?.run">
              Run #{{ feed.run.id }} · {{ feed.run.algorithmKey }} V{{ feed.run.algorithmVersion }} ·
              完成于 {{ formatDateTime(feed.run.completedAt) }}
            </p>
            <p v-else>尚无成功的 Recommendation Run。</p>
          </div>
          <span v-if="feed">共 {{ feed.total }} 条可见推荐</span>
        </div>

        <PageState
          v-if="feedLoading"
          kind="loading"
          title="正在加载推荐 Feed"
        />
        <PageState
          v-else-if="!feed?.run"
          kind="empty"
          title="尚无推荐结果"
          description="请先保存推荐画像，再手动刷新；刷新只使用数据库中已有的成功分析。"
        />
        <PageState
          v-else-if="feed.items.length === 0"
          kind="empty"
          title="当前没有可见推荐"
          description="本轮可能没有候选，或职位已被不感兴趣/已联系且不合适状态排除。"
        />
        <div v-else class="recommendation-feed-list">
          <RecommendationFeedCard
            v-for="item in feed.items"
            :key="item.recommendationItemId"
            :item="item"
            :busy="busyItemId === item.recommendationItemId"
            @feedback="changeFeedback"
            @disposition="changeDisposition"
            @view="recordView"
          />
        </div>

        <ElPagination
          v-if="feed && feed.total > feed.pageSize"
          class="recommendation-pagination"
          background
          layout="prev, pager, next"
          :current-page="feed.page"
          :page-size="feed.pageSize"
          :total="feed.total"
          @current-change="changePage"
        />
      </section>
    </template>
  </section>
</template>
