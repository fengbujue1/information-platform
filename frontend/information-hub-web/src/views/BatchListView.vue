<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElButton, ElTable, ElTableColumn, ElTag } from 'element-plus'

import { toApiClientError } from '@/api/apiError'
import { listAnalysisBatches } from '@/api/batchApi'
import PageState from '@/components/common/PageState.vue'
import type { AnalysisBatch } from '@/types/batch'
import { formatDateTime, formatNullableValue } from '@/utils/formatters'

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
        <h1>Analysis Batches</h1>
        <p>查看 Manual 与 Schedule 共用 Batch Engine 的执行状态。</p>
      </div>
      <div class="ai-actions">
        <RouterLink to="/ai/analyze">新建手动分析</RouterLink>
        <ElButton @click="load">刷新</ElButton>
      </div>
    </header>

    <PageState
      v-if="loading"
      kind="loading"
      title="正在加载 Batches"
    />
    <PageState
      v-else-if="error"
      kind="error"
      title="Batches 加载失败"
      :description="error"
    >
      <ElButton type="primary" @click="load">重试</ElButton>
    </PageState>
    <PageState
      v-else-if="batches.length === 0"
      kind="empty"
      title="暂无 Analysis Batch"
      description="执行 Preview 并确认后，Batch 会显示在这里。"
    />

    <ElTable v-else :data="batches" stripe>
      <ElTableColumn prop="id" label="ID" width="90">
        <template #default="{ row }">
          <RouterLink :to="`/ai/batches/${row.id}`">
            #{{ row.id }}
          </RouterLink>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="triggerType" label="Trigger" width="120" />
      <ElTableColumn label="状态" width="120">
        <template #default="{ row }">
          <ElTag :type="row.status === 'SUCCEEDED' ? 'success' : row.status === 'FAILED' ? 'danger' : 'info'">
            {{ row.status }}
          </ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="selectedCount" label="Selected" width="100" />
      <ElTableColumn label="Estimated / Actual">
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
