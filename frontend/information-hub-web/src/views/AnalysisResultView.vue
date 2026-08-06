<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElButton } from 'element-plus'

import { getAnalysis } from '@/api/analysisApi'
import { toApiClientError } from '@/api/apiError'
import AnalysisResultPanel from '@/components/ai/AnalysisResultPanel.vue'
import PageState from '@/components/common/PageState.vue'
import type { InformationAnalysis } from '@/types/analysis'

const route = useRoute()
const analysisId = Number(route.params.id)
const validId = Number.isSafeInteger(analysisId) && analysisId > 0
const analysis = ref<InformationAnalysis | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)

async function load(): Promise<void> {
  if (!validId) {
    loading.value = false
    return
  }
  loading.value = true
  error.value = null
  try {
    analysis.value = await getAnalysis(analysisId)
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
        <RouterLink to="/ai/batches">← 返回 Batch</RouterLink>
        <h1>Analysis Result</h1>
      </div>
      <ElButton v-if="validId" @click="load">刷新</ElButton>
    </header>
    <PageState
      v-if="!validId"
      kind="invalid"
      title="Analysis 地址无效"
      description="Analysis ID 必须为正整数"
    />
    <PageState
      v-else-if="loading"
      kind="loading"
      title="正在加载 Analysis"
    />
    <PageState
      v-else-if="error"
      kind="error"
      title="Analysis 加载失败"
      :description="error"
    >
      <ElButton type="primary" @click="load">重试</ElButton>
    </PageState>
    <AnalysisResultPanel v-else-if="analysis" :analysis="analysis" />
  </section>
</template>
