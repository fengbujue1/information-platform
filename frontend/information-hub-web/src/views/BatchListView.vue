<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElButton, ElTable, ElTableColumn, ElTag } from 'element-plus'

import { toApiClientError } from '@/api/apiError'
import { listAnalysisBatches } from '@/api/batchApi'
import PageState from '@/components/common/PageState.vue'
import type { AnalysisBatch } from '@/types/batch'
import { formatDateTime, formatNullableValue } from '@/utils/formatters'
import {
  formatBatchStatus,
  formatBatchTrigger,
} from '@/utils/aiDisplay'

const batches = ref<AnalysisBatch[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

async function load(): Promise<void> {
  loading.value = true
  error.value = null
  try {
    batches.value = await listAnalysisBatches(50)
  } catch (cause) {
    error.value = toApiClientError(cause).message
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="ai-page">
    <header class="ai-page-header">
      <div>
        <h1>分析批次</h1>
        <p>查看手动分析与定时分析共用批次引擎的执行状态。</p>
      </div>
      <div class="ai-actions">
        <RouterLink to="/ai/analyze">新建手动分析</RouterLink>
        <ElButton @click="load">刷新</ElButton>
      </div>
    </header>

    <PageState
      v-if="loading"
      kind="loading"
      title="正在加载分析批次"
    />
    <PageState
      v-else-if="error"
      kind="error"
      title="分析批次加载失败"
      :description="error"
    >
      <ElButton type="primary" @click="load">重试</ElButton>
    </PageState>
    <PageState
      v-else-if="batches.length === 0"
      kind="empty"
      title="暂无分析批次"
      description="执行预览并确认后，分析批次会显示在这里。"
    />

    <ElTable v-else :data="batches" stripe>
      <ElTableColumn prop="id" label="ID" width="90">
        <template #default="{ row }">
          <RouterLink :to="`/ai/batches/${row.id}`">
            #{{ row.id }}
          </RouterLink>
        </template>
      </ElTableColumn>
      <ElTableColumn label="触发方式" width="120">
        <template #default="{ row }">
          {{ formatBatchTrigger(row.triggerType) }}
        </template>
      </ElTableColumn>
      <ElTableColumn label="状态" width="120">
        <template #default="{ row }">
          <ElTag :type="row.status === 'SUCCEEDED' ? 'success' : row.status === 'FAILED' ? 'danger' : 'info'">
            {{ formatBatchStatus(row.status) }}
          </ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="selectedCount" label="已选择" width="100" />
      <ElTableColumn label="预估 Token / 实际 Token">
        <template #default="{ row }">
          {{ formatNullableValue(row.estimatedTotalTokens) }} /
          {{ formatNullableValue(row.progress.actualTotalTokens) }}
        </template>
      </ElTableColumn>
      <ElTableColumn label="创建时间">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </ElTableColumn>
    </ElTable>
  </section>
</template>
