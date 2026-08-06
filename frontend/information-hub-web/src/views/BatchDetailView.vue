<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  ElButton,
  ElDescriptions,
  ElDescriptionsItem,
  ElProgress,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus'

import { toApiClientError } from '@/api/apiError'
import {
  getAnalysisBatch,
  getAnalysisBatchProgress,
} from '@/api/batchApi'
import PageState from '@/components/common/PageState.vue'
import type { AnalysisBatch, AnalysisBatchProgress } from '@/types/batch'
import { formatDateTime, formatNullableValue } from '@/utils/formatters'

const route = useRoute()
const batchId = Number(route.params.id)
const batch = ref<AnalysisBatch | null>(null)
const progress = ref<AnalysisBatchProgress | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
let pollTimer: ReturnType<typeof setInterval> | null = null
let polling = false

const validId = Number.isSafeInteger(batchId) && batchId > 0
const completedCount = computed(() => {
  const value = progress.value
  return value
    ? value.succeededCount +
        value.failedCount +
        value.skippedCount +
        value.deferredCount
    : 0
})
const percentage = computed(() => {
  const total = progress.value?.itemCount ?? 0
  return total === 0
    ? 0
    : Math.round((completedCount.value / total) * 100)
})
const isTerminal = computed(() =>
  ['COMPLETED', 'PARTIAL_FAILED', 'FAILED', 'NOOP'].includes(
    batch.value?.status ?? '',
  ),
)

async function load(): Promise<void> {
  if (!validId) {
    loading.value = false
    return
  }
  loading.value = true
  error.value = null
  try {
    batch.value = await getAnalysisBatch(batchId)
    progress.value = batch.value.progress
    configurePolling()
  } catch (cause) {
    error.value = toApiClientError(cause).message
  } finally {
    loading.value = false
  }
}

async function pollProgress(): Promise<void> {
  if (polling || isTerminal.value) return
  polling = true
  try {
    progress.value = await getAnalysisBatchProgress(batchId)
    const remaining =
      progress.value.selectedCount + progress.value.runningCount
    if (remaining === 0) {
      batch.value = await getAnalysisBatch(batchId)
      progress.value = batch.value.progress
      stopPolling()
    }
  } catch (cause) {
    error.value = toApiClientError(cause).message
    stopPolling()
  } finally {
    polling = false
  }
}

function configurePolling(): void {
  stopPolling()
  if (!isTerminal.value) {
    pollTimer = setInterval(() => void pollProgress(), 2500)
  }
}

function stopPolling(): void {
  if (pollTimer !== null) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onMounted(load)
onBeforeUnmount(stopPolling)
</script>

<template>
  <section class="ai-page">
    <header class="ai-page-header">
      <div>
        <RouterLink to="/ai/batches">← 返回 Batch 列表</RouterLink>
        <h1>Batch #{{ batchId }}</h1>
      </div>
      <ElButton @click="load">刷新</ElButton>
    </header>

    <PageState
      v-if="!validId"
      kind="invalid"
      title="Batch 地址无效"
      description="Batch ID 必须为正整数"
    />
    <PageState
      v-else-if="loading"
      kind="loading"
      title="正在加载 Batch"
    />
    <PageState
      v-else-if="error && !batch"
      kind="error"
      title="Batch 加载失败"
      :description="error"
    >
      <ElButton type="primary" @click="load">重试</ElButton>
    </PageState>

    <template v-else-if="batch">
      <p v-if="error" class="ai-error" role="alert">{{ error }}</p>
      <section class="ai-section">
        <div class="ai-card-heading">
          <h2>执行概况</h2>
          <ElTag>{{ batch.status }}</ElTag>
        </div>
        <ElProgress :percentage="percentage" />
        <div v-if="progress" class="ai-metric-grid">
          <div class="ai-metric">
            <span class="ai-metric-label">Succeeded / Failed</span>
            <strong class="ai-metric-value">
              {{ progress.succeededCount }} / {{ progress.failedCount }}
            </strong>
          </div>
          <div class="ai-metric">
            <span class="ai-metric-label">Running / Selected</span>
            <strong class="ai-metric-value">
              {{ progress.runningCount }} / {{ progress.selectedCount }}
            </strong>
          </div>
          <div class="ai-metric">
            <span class="ai-metric-label">Estimated Total</span>
            <strong class="ai-metric-value">
              {{ formatNullableValue(batch.estimatedTotalTokens) }}
            </strong>
          </div>
          <div class="ai-metric">
            <span class="ai-metric-label">Actual Total</span>
            <strong class="ai-metric-value">
              {{ formatNullableValue(progress.actualTotalTokens) }}
            </strong>
          </div>
        </div>
      </section>

      <section class="ai-section">
        <h2>冻结参数</h2>
        <ElDescriptions :column="2" border>
          <ElDescriptionsItem label="Trigger">{{ batch.triggerType }}</ElDescriptionsItem>
          <ElDescriptionsItem label="Prompt Version">{{ batch.promptVersionId }}</ElDescriptionsItem>
          <ElDescriptionsItem label="窗口">{{ formatDateTime(batch.windowStart) }} — {{ formatDateTime(batch.windowEnd) }}</ElDescriptionsItem>
          <ElDescriptionsItem label="Limits">{{ batch.requestedMaxCandidates }} / {{ batch.requestedTokenBudget }}</ElDescriptionsItem>
          <ElDescriptionsItem label="Eligible / Selected">{{ batch.eligibleCount }} / {{ batch.selectedCount }}</ElDescriptionsItem>
          <ElDescriptionsItem label="Skip Reason">{{ formatNullableValue(batch.skipReason) }}</ElDescriptionsItem>
        </ElDescriptions>
      </section>

      <section class="ai-section">
        <h2>Items</h2>
        <ElTable :data="batch.items" stripe>
          <ElTableColumn prop="selectionOrder" label="#" width="70" />
          <ElTableColumn prop="informationId" label="Information" width="120" />
          <ElTableColumn prop="snapshotId" label="Snapshot" width="110" />
          <ElTableColumn prop="status" label="状态" width="110" />
          <ElTableColumn prop="decisionReason" label="原因" />
          <ElTableColumn label="Estimated" width="120">
            <template #default="{ row }">
              {{ formatNullableValue(row.estimatedTotalTokens) }}
            </template>
          </ElTableColumn>
          <ElTableColumn label="Result" width="120">
            <template #default="{ row }">
              <RouterLink
                v-if="row.analysisId"
                :to="`/ai/analyses/${row.analysisId}`"
              >
                Analysis
              </RouterLink>
              <span v-else>—</span>
            </template>
          </ElTableColumn>
        </ElTable>
      </section>
    </template>
  </section>
</template>
