<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElButton } from 'element-plus'

import { toApiClientError } from '@/api/apiError'
import { getAnalysisUsage } from '@/api/usageApi'
import UsageMetrics from '@/components/ai/UsageMetrics.vue'
import PageState from '@/components/common/PageState.vue'
import type { AnalysisUsage } from '@/types/usage'

const usage = ref<AnalysisUsage | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)

async function load(): Promise<void> {
  loading.value = true
  error.value = null
  try {
    usage.value = await getAnalysisUsage()
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
        <h1>Token 用量统计</h1>
        <p>
          只展示模型服务实际报告的 Token；预估 Token 不会补入实际用量。
        </p>
      </div>
      <ElButton @click="load">刷新</ElButton>
    </header>
    <PageState
      v-if="loading"
      kind="loading"
      title="正在加载 Token 用量统计"
    />
    <PageState
      v-else-if="error"
      kind="error"
      title="Token 用量统计加载失败"
      :description="error"
    >
      <ElButton type="primary" @click="load">重试</ElButton>
    </PageState>
    <template v-else-if="usage">
      <p class="ai-muted">自然日/月时区：{{ usage.timezone }}</p>
      <UsageMetrics title="今日" :usage="usage.today" />
      <UsageMetrics title="本月" :usage="usage.month" />
      <UsageMetrics title="累计" :usage="usage.allTime" />
    </template>
  </section>
</template>
